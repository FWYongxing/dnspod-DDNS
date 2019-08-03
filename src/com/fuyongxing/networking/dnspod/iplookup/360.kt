package com.fuyongxing.networking.dnspod.iplookup

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface The360IPService {
    @GET("IPShare/info")
    fun get(): Call<GetResponse>

    data class GetResponse(
        val ip: String,
        val greetheader: String,
        val loc_client: String,
        val location: String,
        val nickname: String
    )
}


val the360IPService: The360IPService = Retrofit.Builder()
    .baseUrl("http://ip.360.cn ")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(The360IPService::class.java)