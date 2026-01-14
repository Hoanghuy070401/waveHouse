package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName
import com.gtelots.maps.data.model.models.Lang
import com.gtelots.maps.data.model.models.ParsedText

data class Query(
    @SerializedName("lang")
    var lang: Lang? = null,
    @SerializedName("layers")
    var layers: List<String?>? = null,
    @SerializedName("parsed_text")
    var parsedText: ParsedText? = null,
    @SerializedName("parser")
    var parser: String? = null,
    @SerializedName("private")
    var `private`: Boolean? = null,
    @SerializedName("querySize")
    var querySize: Int? = null,
    @SerializedName("size")
    var size: Int? = null,
    @SerializedName("sources")
    var sources: List<String?>? = null,
    @SerializedName("text")
    var text: String? = null
)