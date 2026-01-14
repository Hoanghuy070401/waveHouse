package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Feature(
    @SerializedName("geometry")
    var geometry: Geometry? = null,
    @SerializedName("properties")
    var properties: Properties? = null,
    @SerializedName("type")
    var type: String? = null
)