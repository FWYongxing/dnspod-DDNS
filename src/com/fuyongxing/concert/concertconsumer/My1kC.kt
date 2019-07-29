package com.fuyongxing.concert.concertconsumer

import com.fuyongxing.concert.concertproducer.Concert
import com.fuyongxing.concert.concertproducer.ConcertHallAPIException
import com.fuyongxing.concert.concertproducer.logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ServiceOfMyConcert {
    @GET("/")
    fun concerts(): Call<List<Concert>>
}

const val baseUrl = "http://localhost:8080"

var retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val serviceOfMyConcert: ServiceOfMyConcert = retrofit.create(ServiceOfMyConcert::class.java)

fun main() = runBlocking {
    repeat(10000) {
        launch {
            val concerts = serviceOfMyConcert.concerts().execute().body() ?: throw ConcertHallAPIException()
            logger.debug { "第${it}次获取Concert列表，数量：${concerts.size}" }
        }
    }

}