package com.fuyongxing.networking.dnspod.iplookup

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface IfconfigIPService {
    @GET("json")
    fun get(): Call<GetResponse>

    data class GetResponse(
        val ip: String,
        val city: String?,
        val country: String?,
        val country_eu: Boolean?,
        val country_iso: String?,
        val asn: String?,
        val asn_org: String?,
        val ip_decimal: Int?,
        val latitude: Double?,
        val longitude: Double?
    )
}

val ifconfigIPService = Retrofit.Builder()
    .baseUrl("https://ifconfig.co")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(IfconfigIPService::class.java)