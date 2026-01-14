package com.gtelots.maps.data.model

import com.google.gson.annotations.SerializedName

open class HttpData<T>(
    @field:SerializedName("status")
    open var status: Int? = null,

    @field:SerializedName("message")
    open var message: String? = null,


    @field:SerializedName("code")
    open var code: String? = "",

    @field:SerializedName("meta")
    open var meta: MetaDataModel? = null,


    @field:SerializedName("value")
    open val value: T? = null,

    @field:SerializedName("data")
    open var data: ArrayList<T>? = null
){
    fun isSuccess(): Boolean {
        return status == 200
    }
}