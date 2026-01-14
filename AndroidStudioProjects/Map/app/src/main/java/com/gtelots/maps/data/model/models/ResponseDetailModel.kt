package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName
import com.gtelots.maps.data.model.models.Data

data class ResponseDetailModel(
    @SerializedName("data")
    var `data`: Data? = Data()
)