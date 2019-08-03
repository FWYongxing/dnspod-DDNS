package com.fuyongxing.networking.dnspod.iplookup

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface HostIPService {
    @GET("get_json.php")
    fun get(): Call<GetResponse>

    data class GetResponse(
        val ip: String,
        val city: String?,
        val country_code: String?,
        val country_name: String?
    )
}

val hostIPService: HostIPService = Retrofit.Builder()
    .baseUrl("http://api.hostip.info")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(HostIPService::class.java)