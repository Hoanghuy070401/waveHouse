package com.gtelots.maps.data.model.models


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("addressComponents")
    var addressComponents: List<AddressComponentX>? = listOf(),
    @SerializedName("adrFormatAddress")
    var adrFormatAddress: String? = "",
    @SerializedName("displayName")
    var displayName: DisplayName? = DisplayName(),
    @SerializedName("email")
    var email: String? = "",
    @SerializedName("extras")
    var extras: Extras? = Extras(),
    @SerializedName("formattedAddress")
    var formattedAddress: String? = "",
    @SerializedName("id")
    var id: String? = "",
    @SerializedName("internationalPhoneNumber")
    var internationalPhoneNumber: String? = "",
    @SerializedName("location")
    var location: Location? = Location(),
    @SerializedName("nationalPhoneNumber")
    var nationalPhoneNumber: String? = "",
    @SerializedName("plusCode")
    var plusCode: PlusCode? = PlusCode(),
    @SerializedName("timeZone")
    var timeZone: TimeZone? = TimeZone(),
    @SerializedName("types")
    var types: List<String>? = listOf(),
    @SerializedName("viewport")
    var viewport: Viewport? = Viewport(),
    @SerializedName("websiteUri")
    var websiteUri: String? = ""
)