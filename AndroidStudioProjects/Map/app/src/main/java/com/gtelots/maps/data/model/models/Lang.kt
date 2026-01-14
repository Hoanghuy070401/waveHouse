package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Lang(
    @SerializedName("defaulted")
    var defaulted: Boolean? = null,
    @SerializedName("iso6391")
    var iso6391: String? = null,
    @SerializedName("iso6393")
    var iso6393: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("via")
    var via: String? = null
)