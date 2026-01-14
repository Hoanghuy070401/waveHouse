package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Extras(
    @SerializedName("charging_port")
    var chargingPort: String? = null,
    @SerializedName("description")
    var description: String? = null
)