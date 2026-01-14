package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class AddressComponent(
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("types")
    var types: List<String?>? = null
)