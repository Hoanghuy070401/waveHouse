package com.gtelots.maps.data


import com.gtelots.maps.data.model.models.ResponseDetailModel
import com.gtelots.maps.data.model.models.SuggestAddressModel
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SearchAddressApiHelper @Inject constructor(
    private val api: SearchAddressService
){

    suspend fun getAddressSuggest(q: String) : SuggestAddressModel {
        return api.suggestAddress(q,10,"NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox")
    }
    suspend fun getAddressDetails(q: String) : ResponseDetailModel {
        return api.detailsAddress(q,"NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox")
    }
    suspend fun getAddressDetails(latLng: LatLng) : SuggestAddressModel {
        return api.detailsAddressMarker(latLng.longitude.toString(),latLng.latitude.toString(),"NkJ7pLZ3rA5tXvW2cE0KdQ9oIi4CVY8Ox")
    }

}