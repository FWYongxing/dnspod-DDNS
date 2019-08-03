package com.fuyongxing.networking.dnspod.iplookup

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface IPIFYService {
    @GET("/")
    fun get(@Query(value = "format") format:String="json"): Call<GetResponse>

    data class GetResponse(
        val ip: String
    )
}

val ipifyService: IPIFYService = Retrofit.Builder()
    .baseUrl("https://api.ipify.org")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(IPIFYService::class.java)