package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class DisplayName(
    @SerializedName("languageCode")
    var languageCode: String? = null,
    @SerializedName("text")
    var text: String? = null
)