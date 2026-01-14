package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName
import com.gtelots.maps.data.model.models.AddressComponent

data class Properties(
    @SerializedName("accuracy")
    var accuracy: String? = null,
    @SerializedName("address")
    var address: String? = null,
    @SerializedName("address_components")
    var addressComponents: List<AddressComponent?>? = null,
    @SerializedName("country")
    var country: String? = null,
    @SerializedName("county")
    var county: String? = null,
    @SerializedName("county_code")
    var countyCode: String? = null,
    @SerializedName("gid")
    var gid: String? = null,
    @SerializedName("id")
    var id: String? = null,
    @SerializedName("layer")
    var layer: String? = null,
    @SerializedName("localadmin")
    var localadmin: String? = null,
    @SerializedName("localadmin_code")
    var localadminCode: String? = null,
    @SerializedName("locality")
    var locality: String? = null,
    @SerializedName("locality_code")
    var localityCode: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("plus_code")
    var plusCode: String? = null,
    @SerializedName("region")
    var region: String? = null,
    @SerializedName("region_code")
    var regionCode: String? = null,
    @SerializedName("source")
    var source: String? = null,
    @SerializedName("source_id")
    var sourceId: String? = null
)