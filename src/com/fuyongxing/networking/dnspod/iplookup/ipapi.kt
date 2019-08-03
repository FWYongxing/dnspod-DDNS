package com.fuyongxing.networking.dnspod.iplookup

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface IPAPIService {
    @GET("///ipapi.co/json")
    fun get(): Call<GetResponse>

    data class GetResponse(
        val ip: String,
        val country: String?,
        val city: String?,
        val continent_code: String?,
        val country_calling_code: String?,
        val country_name: String?,
        val currency: String?,
        val in_eu: Boolean?,
        val languages: String?,
        val latitude: Double?,
        val longitude: Double?,
        val asn: String?,
        val org: String?,
        val postal: Any?,
        val region: String?,
        val region_code: String?,
        val timezone: String?,
        val utc_offset: String
    )
}

val ipapiService: IPAPIService = Retrofit.Builder()
    .baseUrl("https://api.ipify.org")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(IPAPIService::class.java)