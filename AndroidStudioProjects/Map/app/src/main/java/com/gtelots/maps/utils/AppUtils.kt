package com.gtelots.maps.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.gson.Gson
import com.ots.myapplication.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.ColorUtils
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.navigation.android.navigation.v5.models.DirectionsRoute
import java.text.DecimalFormat


object AppUtils {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    fun addMarker(point: LatLng, maplibreMap: MapLibreMap, markerList: ArrayList<MarkerOptions>) {
        val pixel = maplibreMap.projection.toScreenLocation(point)
        val title = (point.latitude) + (point.longitude)
        val snippet = "" + pixel.x.toInt() + " " + pixel.y.toInt()
        val marker = MarkerOptions()
            .position(point)
            .title(title.toString())
            .snippet(snippet)
        markerList.add(marker)
        maplibreMap.addMarker(marker)
    }

    fun drawGeoJson(jsonData: String, maplibreMap: MapLibreMap) {
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            val data = FeatureCollection.fromJson(jsonData)
            withContext(Dispatchers.Main) {
                drawLines(data, maplibreMap)
            }
        }
    }


    private fun drawLines(featureCollection: FeatureCollection, maplibreMap: MapLibreMap) {
        val features = featureCollection.features()
        if (!features.isNullOrEmpty()) {
            addLine("rawLine", features[0], "#124afd", maplibreMap)
        }
    }


    private fun addLine(
        layerId: String,
        feature: Feature,
        lineColorHex: String,
        maplibreMap: MapLibreMap
    ) {
        maplibreMap.getStyle { style ->
            // Kiểm tra xem nguồn dữ liệu đã tồn tại hay chưa
            if (style.getSource(layerId) != null) {
                style.removeLayer(layerId)
                style.removeSource(layerId)
            }

            // Thêm GeoJsonSource
            style.addSource(GeoJsonSource(layerId, feature))

            // Thêm LineLayer
            style.addLayer(
                LineLayer(layerId, layerId).withProperties(
                    PropertyFactory.lineColor(
                        ColorUtils.colorToRgbaString(
                            Color.parseColor(lineColorHex)
                        )
                    ),
                    PropertyFactory.lineWidth(6f)
                )
            )
        }
    }

    fun convertMarkersToGeoJson(markerList: ArrayList<MarkerOptions>?): String {
        if (markerList.isNullOrEmpty()) {
            return "{}"
        }

        val coordinates = ArrayList<List<Double>>()
        for (markerOptions in markerList) {
            val latLng = markerOptions.position
            val coord = listOf(latLng.longitude, latLng.latitude)
            coordinates.add(coord)
        }

        val geometry = mapOf(
            "type" to "LineString",
            "coordinates" to coordinates
        )

        val feature = mapOf(
            "type" to "Feature",
            "geometry" to geometry,
            "properties" to null
        )

        val featureCollection = mapOf(
            "type" to "FeatureCollection",
            "features" to listOf(feature)
        )
        Log.d("ghhg", Gson().toJson(featureCollection))
        return Gson().toJson(featureCollection)
    }

    fun convertMetersToKilometers(meters: Double): String {
        val kilometers = meters / 1000
        val decimalFormat = DecimalFormat("#,###.###")
        return "${decimalFormat.format(kilometers)} km"
    }

    fun adjustCameraZoomForTwoPoints(
        maplibreMap: MapLibreMap,
        point1: LatLng,
        point2: LatLng,
        routes: List<DirectionsRoute>
    ) {

        val boundsBuilder = LatLngBounds.Builder()
            .include(point1)
            .include(point2)
        if (routes.isNotEmpty()) {
            val route = routes[0]
            val geometry = route.geometry()
            val lineString = geometry?.let { LineString.fromPolyline(it, 6) }
            for (point in lineString?.coordinates() ?: arrayListOf()) {
                val latLng = LatLng(point.latitude(), point.longitude())
                boundsBuilder.include(latLng)
            }
        }
        val bounds = boundsBuilder.build()
        val cameraUpdate: CameraUpdate = newLatLngBounds(bounds, 100, 450, 100, 500)
        maplibreMap.animateCamera(cameraUpdate)
    }

    fun hideKeyboard(context: Context, view: View) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(context: Context, view: View) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.requestFocus()
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun progressDialog(context: Context): Dialog {
        try {
            val inflate =
                LayoutInflater.from(context).inflate(R.layout.loading_item, null)
            return Dialog(context).apply {
                setContentView(inflate)
                setCancelable(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        } catch (e: Exception) {
            e.message?.let { Log.d("exception", it) }
        }
        return Dialog(context)
    }


}