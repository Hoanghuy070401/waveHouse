package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class ParsedText(
    @SerializedName("subject")
    var subject: String? = null
)