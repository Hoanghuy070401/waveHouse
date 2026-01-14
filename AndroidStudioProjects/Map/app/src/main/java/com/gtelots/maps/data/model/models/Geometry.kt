package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Geometry(
    @SerializedName("coordinates")
    var coordinates: List<Double?>? = null,
    @SerializedName("type")
    var type: String? = null
)