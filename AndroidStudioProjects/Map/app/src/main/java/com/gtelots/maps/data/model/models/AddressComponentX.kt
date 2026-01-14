package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class AddressComponentX(
    @SerializedName("adminCode")
    var adminCode: String? = null,
    @SerializedName("adminLevel")
    var adminLevel: String? = null,
    @SerializedName("longText")
    var longText: String? = null,
    @SerializedName("shortText")
    var shortText: String? = null,
    @SerializedName("types")
    var types: List<String?>? = null
)