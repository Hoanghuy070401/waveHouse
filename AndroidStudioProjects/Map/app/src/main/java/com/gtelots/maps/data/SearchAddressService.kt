package com.gtelots.maps.data

import com.gtelots.maps.data.model.models.ResponseDetailModel
import com.gtelots.maps.data.model.models.SuggestAddressModel
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchAddressService {
    @GET("api/v1/autocomplete")
    suspend fun suggestAddress(
        @Query("text") key:String,
        @Query("limit") limit:Int,
        @Query("apikey") apikey:String,
    ): SuggestAddressModel
    @GET("api/v1/place")
    suspend fun detailsAddress(
        @Query("ids") id:String,
        @Query("apikey") apikey:String
    ): ResponseDetailModel
    @GET("api/v1/reverse")
    suspend fun detailsAddressMarker(
        @Query("point.lon") longitude:String,
        @Query("point.lat") sessionToken:String,
        @Query("apikey") apikey:String,
    ): SuggestAddressModel
}