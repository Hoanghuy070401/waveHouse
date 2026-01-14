package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName
import com.gtelots.maps.data.model.models.Feature
import com.gtelots.maps.data.model.models.Geocoding

data class SuggestAddressModel(
    @SerializedName("bbox")
    var bbox: List<Double?>? = null,
    @SerializedName("features")
    var features: List<Feature>? = null,
    @SerializedName("geocoding")
    var geocoding: Geocoding? = null,
    @SerializedName("licence")
    var licence: String? = null,
    @SerializedName("type")
    var type: String? = null
)