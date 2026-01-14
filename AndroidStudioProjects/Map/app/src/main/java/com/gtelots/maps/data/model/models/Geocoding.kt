package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Geocoding(
    @SerializedName("query")
    var query: Query? = null
)