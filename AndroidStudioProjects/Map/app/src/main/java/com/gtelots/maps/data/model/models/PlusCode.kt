package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class PlusCode(
    @SerializedName("compoundCode")
    var compoundCode: String? = null,
    @SerializedName("globalCode")
    var globalCode: String? = null
)