package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Location(
    @SerializedName("latitude")
    var latitude: Double? = null,
    @SerializedName("longitude")
    var longitude: Double? = null
)