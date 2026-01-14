package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName
import com.gtelots.maps.data.model.models.High
import com.gtelots.maps.data.model.models.Low

data class Viewport(
    @SerializedName("high")
    var high: High? = null,
    @SerializedName("low")
    var low: Low? = null
)