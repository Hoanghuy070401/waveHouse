package com.gtelots.maps.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.gtelots.maps.constants.EnumKey
import com.gtelots.maps.data.model.models.StyleModel
import com.gtelots.maps.ui.SearchFragment
import com.gtelots.maps.ui.dialog.StyleBottomSheetDialog
import com.gtelots.maps.ui.viewmodel.SearchViewModel
import com.gtelots.maps.utils.AppUtils
import com.gtelots.maps.utils.AppUtils.addMarker
import com.gtelots.maps.utils.State
import com.ots.myapplication.R
import com.ots.myapplication.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Point
import org.maplibre.navigation.android.navigation.ui.v5.NavigationLauncher
import org.maplibre.navigation.android.navigation.ui.v5.NavigationLauncherOptions
import org.maplibre.navigation.android.navigation.v5.models.DirectionsResponse
import org.maplibre.navigation.android.navigation.v5.models.DirectionsRoute
import org.maplibre.navigation.android.navigation.v5.models.RouteOptions
import org.maplibre.navigation.android.navigation.v5.navigation.NavigationMapRoute
import org.maplibre.turf.TurfConstants
import org.maplibre.turf.TurfMeasurement
import java.io.IOException


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private var lastLocation: Location? = Location("default").apply {
        latitude = 10.8231
        longitude = 106.6297
    }
    private lateinit var mapView: MapView
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private var maplibreMap: MapLibreMap? = null
    private lateinit var mBehavior: BottomSheetBehavior<View>
    private var markerList: ArrayList<MarkerOptions>? = ArrayList()
    private var fragment = SearchFragment()
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC
        )
        .enableAutoZoom()
        .build()
    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val scanner = GmsBarcodeScanning.getClient(this, options)
                scanner.startScan()
                    .addOnSuccessListener { barcode ->
                        handleQRCode(barcode.rawValue ?: "")
                    }
                    .addOnCanceledListener {
                        Toast.makeText(this, "Quét thất bại", Toast.LENGTH_SHORT).show()

                    }
                    .addOnFailureListener { e ->
                        Log.d("abc", "Quét thất bại ${e.message}")
                    }
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền sử dụng camera", Toast.LENGTH_SHORT).show()
            }
        }
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false


        }

    private val viewModel: SearchViewModel by viewModels()
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val loadingProgress by lazy {
        AppUtils.progressDialog(this)
    }
    private val list = arrayListOf(
        StyleModel(
            "Mặc đinh",
            "https://maps.ots.vn/api/styles/v1/gtelmaps-streets-v1/style.json?apikey=NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox",//"asset://gtel-ots-map.geojson",
            R.drawable.ic_point
        ),
        StyleModel(
            "Đường phố",
            "https://tileserver-gl.laragis.vn/styles/gtelmaps-streets/style.json",
            R.drawable.ic_street
        ),
        StyleModel(
            "Giao thông",
            "https://tileserver-gl.laragis.vn/styles/gtelmaps-traffic/style.json",
            R.drawable.ic_traffic_car
        ),
        StyleModel(
            "Vệ tinh",
            "https://maps.ots.vn/api/styles/v1/gtelmaps-satellite-streets-v1/style.json?apikey=NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox",
            R.drawable.ic
        ),
        StyleModel(
            "3D",
            "https://tileserver-gl.laragis.vn/styles/laragis-traffic/style.json",
            R.drawable.ic_3d
        )

    )

    //Directions
    private var simulateRoute = false
    private var route: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var destination: Point? = null
    private var latitude: Double? = null
    private var longitude: Double? = null


    private var linkStyleMap =
        "https://maps.ots.vn/api/styles/v1/gtelmaps-streets-v1/style.json?apikey=NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox"


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initView()
        setEvent()
        observer()

        lastLocation = savedInstanceState?.getParcelable(
            EnumKey.SAVED_STATE_LOCATION.toString()
        )
        checkPermissions()
    }
    @SuppressLint("MissingPermission")
    private fun enableLocation(style: Style) {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        locationComponent = maplibreMap?.locationComponent

        val options = LocationComponentOptions.builder(this)
            .pulseEnabled(true)
            .foregroundDrawable(R.drawable.ic_my_location)
            .build()

        val activationOptions = LocationComponentActivationOptions
            .builder(this, style)
            .locationComponentOptions(options)
            .useDefaultLocationEngine(true)
            .build()

        locationComponent?.activateLocationComponent(activationOptions)
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.TRACKING

        lastLocation = locationComponent?.lastKnownLocation
    }
    private fun observer() {
        viewModel.addressDetails.observe(this) {
            when (it) {
                is State.Error -> {
                }

                State.Loading -> {}
                is State.Success -> {
                    resetMap()
                    if (it.data.data?.location?.latitude != null) {
                        val dataDetails = it.data.data
                        latitude = dataDetails?.location?.latitude ?: 0.0
                        longitude = dataDetails?.location?.longitude ?: 0.0
                        val position = LatLng(latitude ?: 10.8231, longitude ?: 106.6297)
                        moveDetails(position, dataDetails?.displayName?.text ?: "")
                        mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        binding.tvTitle.text = dataDetails?.displayName?.text
                        binding.tvDetails.text = dataDetails?.formattedAddress

                    } else {
                        Toast.makeText(
                            this,
                            "Vị trí này hiện tại chưa xác định",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        viewModel.addressDetailsMarker.observe(this) {
            when (it) {
                is State.Error -> {
                }

                State.Loading -> {}
                is State.Success -> {
                    if (!it.data.features?.isEmpty()!!) {
                        val dataDetails = it.data.features?.get(0)?.properties
                        mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        binding.tvTitle.text = dataDetails?.name
                        binding.tvDetails.text = dataDetails?.address
                        binding.tvSearch.text = dataDetails?.address

                    } else {
                        Toast.makeText(
                            this,
                            "Vị trí này hiện tại chưa xác định",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun moveDetails(position: LatLng, name: String) {
        binding.tvSearch.text = name
        maplibreMap?.let { addMarker(position, it, markerList ?: arrayListOf()) }
        maplibreMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(position, 16.0)
        )
    }

    private fun directions() {
        route?.let { route ->
            val userLocation = maplibreMap?.locationComponent?.lastKnownLocation ?: return@let
            val options = NavigationLauncherOptions.builder()
                .directionsRoute(route)
                .shouldSimulateRoute(simulateRoute)
                .initialMapCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(userLocation.latitude, userLocation.longitude)).build()
                )
                .lightThemeResId(R.style.TestNavigationViewLight)
                .darkThemeResId(R.style.TestNavigationViewDark)
                .build()
            NavigationLauncher.startNavigation(this@MainActivity, options)
        }
    }

    private fun directionsDrawl(lat: Double, long: Double) {
        destination = Point.fromLngLat(long, lat)
        calculateRoute()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: MapLibreMap) {
        this.maplibreMap = p0
        try {
            maplibreMap?.setStyle(Style.Builder().fromUri(linkStyleMap)) { style: Style ->

                enableLocation(style)
//            navigationMapRoute = NavigationMapRoute(binding.mapView, p0)
                Log.d("linkMap", linkStyleMap)
                locationComponent = maplibreMap?.locationComponent
                val locationComponentOptions =
                    LocationComponentOptions.builder(this@MainActivity)
                        .pulseEnabled(true)
                        .pulseColor(Color.GREEN)
                        .foregroundDrawable(R.drawable.ic_my_location)
                        .build()
                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(style, locationComponentOptions)

                locationComponent?.activateLocationComponent(locationComponentActivationOptions)
                locationComponent?.isLocationComponentEnabled = true
                locationComponent?.cameraMode = CameraMode.TRACKING_GPS
                locationComponent?.forceLocationUpdate(lastLocation)
                lastLocation = locationComponent?.lastKnownLocation
                fragment.newInstance(lastLocation)
                locationComponent!!.zoomWhileTracking(16.0)
                p0.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            lastLocation?.latitude ?: 0.0,
                            lastLocation?.longitude ?: 0.0
                        ), 16.0
                    )
                )
                p0.addOnMapLongClickListener { point: LatLng ->
                    resetMap()
                    addMarker(point, p0, markerList ?: arrayListOf())
                    viewModel.getAddressDetailsMarkers(point)
                    latitude = point.latitude
                    longitude = point.longitude
                    binding.btnDirections.visibility = View.VISIBLE
                    false
                }


            }
            maplibreMap?.uiSettings?.isLogoEnabled = false
            maplibreMap?.uiSettings?.isAttributionEnabled = false

        } catch (ex: Exception) {

        }

    }

    private fun changeStyleMap(link: String, position: Int = 0) {
        if (position == 4) {
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(10.774953, 106.706519))
                .zoom(16.919812667890913)
                .tilt(56.83499771356583)
                .bearing(326.5813175691292)
                .build()
            maplibreMap?.animateCamera(
                CameraUpdateFactory.newCameraPosition(cameraPosition),
                1000,
                object : MapLibreMap.CancelableCallback {
                    override fun onFinish() {
                        Log.d("CameraAnimation", "Camera move finished")
                    }

                    override fun onCancel() {
                        Log.d("CameraAnimation", "Camera move canceled")
                    }
                }
            )
            maplibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            val cameraPosition = CameraPosition.Builder()
                .zoom(16.0)
                .tilt(0.0)
                .bearing(0.0)
                .build()
            maplibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        maplibreMap?.setStyle(Style.Builder().fromUri(link))
    }


    private fun buildLocationComponentActivationOptions(
        style: Style,
        locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions
            .builder(this, style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(750)
                    .setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build()
            )
            .build()
    }

    private fun setEvent() {
        binding.changeStyleMap.setOnClickListener {
            val dialog = StyleBottomSheetDialog(list)
            dialog.onAction = { position, it ->
                changeStyleMap(it.link, position)
                list.forEachIndexed { index, styleModel ->
                    styleModel.isCheck = index == position
                }
                dialog.dismiss()
            }
            dialog.onActionAdd = {
                dialog.dismiss()
                requestCamera.launch(Manifest.permission.CAMERA)

            }
            dialog.show(supportFragmentManager, "Chọn kiểu bản đồ")
        }
        binding.location.setOnClickListener {
            if (locationComponent != null) {
                locationComponent!!.cameraMode = CameraMode.TRACKING
                locationComponent!!.forceLocationUpdate(lastLocation)
                locationComponent!!.zoomWhileTracking(16.0)
            } else {
                checkPermissions()
            }

        }
        binding.btnDirections.setOnClickListener {
            val cameraPosition = CameraPosition.Builder()
                .tilt(0.0)
                .bearing(0.0)
                .build()
            maplibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            simulateRoute = false
            directionsDrawl(latitude ?: 10.8231, longitude ?: 106.6297)
        }
        binding.btnStart.setOnClickListener {
            simulateRoute = true
            directions()
        }
        binding.tvSearch.setOnClickListener {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.container, fragment).commit()

            binding.container.visibility = View.VISIBLE
        }
        fragment.onItemSelected = { model, isData ->
            if (isData) {
                viewModel.getAddressDetailsList(model.properties?.gid ?: "")
            }
            AppUtils.hideKeyboard(this, binding.root)
            binding.container.visibility = View.GONE
            binding.btnDirections.visibility = View.VISIBLE

        }
    }


    private fun handleQRCode(qrCode: String) {
        val isExist = list.any { it.link == qrCode }

        if (!isExist) {
            list.add(StyleModel("style ${list.size + 1}", qrCode, R.drawable.ic_map_default))
            Toast.makeText(this, "Thêm dữ liệu thành công", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "QR code đã tồn tại trong danh sách", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetMap() {

        markerList?.clear()
        maplibreMap?.removeAnnotations()

        maplibreMap?.getStyle { style ->
            if (style.getSource("rawLine") != null) {
                style.removeLayer("rawLine")
                style.removeSource("rawLine")
            }
        }
        mBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        destination = null
        navigationMapRoute?.removeRoute()
    }


    private fun initView() {
        mBehavior = BottomSheetBehavior.from(binding.llInfo)
        mBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(p0: View, p1: Int) {
            }

            override fun onSlide(p0: View, p1: Float) {
            }

        })
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private var backPressedTime: Long = 0
    private val backPressInterval: Long = 2000
    override fun onBackPressed() {
        if (backPressedTime + backPressInterval > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            resetMap()
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "Nhấn thêm lần nữa để thoát", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateRoute() {
        runOnUiThread {
            loadingProgress.show()
        }
        val userLocation = maplibreMap?.locationComponent?.lastKnownLocation
        val destination = destination
        if (userLocation == null) {
            Log.d("abc", "calculateRoute: User location is null, therefore, origin can't be set.")
            return
        }

        if (destination == null) {
            Log.d(
                "calculateRoute",
                "calculateRoute: destination is null, therefore, destination can't be set."
            )
            return
        }

        val origin = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
            Log.d("calculateRoute", "calculateRoute: distance < 50 m")
            return
        }
        val requestBody = mapOf(
            "format" to "osrm",
            "costing" to "auto",
            "banner_instructions" to true,
            "voice_instructions" to true,
            "language" to "vi",
            "directions_options" to mapOf(
                "units" to "kilometers"
            ),
            "costing_options" to mapOf(
                "auto" to mapOf(
                    "top_speed" to 130
                )
            ),
            "locations" to listOf(
                mapOf(
                    "lon" to origin.longitude(),
                    "lat" to origin.latitude(),
                    "type" to "break"
                ),
                mapOf(
                    "lon" to destination.longitude(),
                    "lat" to destination.latitude(),
                    "type" to "break"
                )
            )
        )

        val requestBodyJson = Gson().toJson(requestBody)
        val client = OkHttpClient()

        val request = Request.Builder()
            .header("User-Agent", "MapLibre Android Navigation SDK Demo App")
            .url(getString(R.string.valhalla_url))
            .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {

                        Log.d(
                            "calculateRoute",
                            response.code.toString()
                        )
                        val responseBodyJson = response.body?.string()
                        Log.d(
                            "calculateRoute",
                            Gson().toJson(responseBodyJson)
                        )
                        val maplibreResponse = DirectionsResponse.fromJson(responseBodyJson);
                        this@MainActivity.route = maplibreResponse.routes()
                            .first().toBuilder()
                            .routeOptions(
                                RouteOptions.builder()
                                    .baseUrl("https://valhalla1.openstreetmap.de/route?")
                                    .profile("driving-traffic")
                                    .user("valhalla")
                                    .steps(true)
                                    .accessToken("sk.eyJ1IjoicXVvY3RoaW5oMjEyIiwiYSI6ImNsbWl3N3VoZDA0MHAzZW12aTV5MW01bGkifQ.DznR-rRVNIpwVCqFgjzsjw")
                                    .voiceInstructions(true)
                                    .bannerInstructions(true)
                                    .language("vi")
                                    .coordinates(listOf(origin, destination))
                                    .requestUuid("0000-0000-0000-0000")
                                    .build()
                            )
                            .build()

                        runOnUiThread {
                            Log.d("ghjhgg", Gson().toJson(maplibreResponse.routes()))
                            navigationMapRoute?.addRoutes(maplibreResponse.routes())

                            maplibreMap?.let { it1 ->
                                AppUtils.adjustCameraZoomForTwoPoints(
                                    it1,
                                    LatLng(latitude ?: 10.8231, longitude ?: 106.6297),
                                    LatLng(
                                        lastLocation?.latitude ?: 10.8231,
                                        lastLocation?.longitude ?: 106.6297
                                    ), maplibreResponse.routes()
                                )
                            }
                            binding.btnDirections.visibility = View.GONE
                            loadingProgress.dismiss()
                        }
                    } else {
                        runOnUiThread {
                            loadingProgress.dismiss()
                            Toast.makeText(
                                this@MainActivity,
                                "Tuyến đường hiện tại không thể xác định",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}