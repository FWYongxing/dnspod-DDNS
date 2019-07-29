package com.fuyongxing.concert.concertproducer

import com.fuyongxing.kotlin.extension.consume
import com.fuyongxing.kotlin.extension.myConnection
import com.fuyongxing.kotlin.extension.publish
import com.fuyongxing.kotlin.extension.withParentUrl
import com.fuyongxing.kotlin.extension.localDateTime
import com.google.gson.annotations.SerializedName
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

interface ServiceOfSZConcert {
    @GET("AjaxAction/Other.ashx")
    fun concerts(
        @Query("type") type: String = "getPerformance2",
        @Query("m") m: String = "126001"
    ): Call<ResponseOfSZConcert<List<ConcertOfSZConcert>?>>
}

val logger = KotlinLogging.logger {}

val baseUrl = "http://www.szyyt.com"

var retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

var serviceOfSZConcert: ServiceOfSZConcert = retrofit.create(ServiceOfSZConcert::class.java)!!

const val grabExchangeName = "concert.grab"
const val grabQueueName = grabExchangeName
const val changesExchangeName = "concert.changes"
const val changesQueueName = changesExchangeName
const val newExchangeName = "concert.new"
const val newQueueName = newExchangeName
const val defaultRoute = "route"

@ExperimentalStdlibApi
fun initMQ(host: String = "dev.fuyongxing.com") = runBlocking {
    val channel = ConnectionFactory().myConnection(host)
    channel.exchangeDeclare(grabExchangeName, BuiltinExchangeType.FANOUT)
    channel.queueDeclare(grabQueueName, true, false, false, mutableMapOf())
    channel.queueBind(grabQueueName, grabExchangeName, defaultRoute)

    channel.exchangeDeclare(changesExchangeName, BuiltinExchangeType.FANOUT)
    channel.queueDeclare(changesQueueName, true, false, false, mutableMapOf())
    channel.queueBind(changesQueueName, changesExchangeName, defaultRoute)

    channel.exchangeDeclare(newExchangeName, BuiltinExchangeType.FANOUT)
    channel.queueDeclare(newQueueName, true, false, false, mutableMapOf())
    channel.queueBind(newQueueName, newExchangeName, defaultRoute)

    channel.close()
    launch { concertChangesNotification(host) }
    launch { newConcertNotification(host) }
    launch { analyze(host) }
    launch { grab(host) }

}

@ExperimentalStdlibApi
fun grab(host: String) {
    Observable.interval(0, 30, TimeUnit.SECONDS)
        .observeOn(Schedulers.io())
        .flatMap {
            val concerts = serviceOfSZConcert.concerts().execute().body()?.info ?: throw ConcertHallAPIException()
            logger.debug { "第${it}次获取Concert列表，数量：${concerts.size}" }
            Observable.fromIterable(concerts)
        }
        .subscribeOn(Schedulers.computation())
        .map { it.toConcert(baseUrl) }
        .retry(3)
        .subscribe(object : Observer<Concert> {
            lateinit var channel: Channel
            override fun onComplete() {

            }

            override fun onSubscribe(d: Disposable) {
                channel = ConnectionFactory().myConnection(host)
                channel.exchangeDeclare(grabExchangeName, BuiltinExchangeType.FANOUT)
                channel.queueDeclare(grabQueueName, true, false, false, mutableMapOf())
                channel.queueBind(grabQueueName, grabExchangeName, defaultRoute)
            }

            override fun onNext(concert: Concert) {
                this.channel.publish(
                    grabExchangeName,
                    defaultRoute,
                    concert
                )
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
            }
        })
}

@ExperimentalStdlibApi
fun analyze(host: String) {
    val channel = ConnectionFactory().myConnection(host)
    channel.consume<Concert>(grabQueueName) {
        transaction {
            val firstOrNull =
                ConcertEntity.find { (ConcertTable.title eq it.title) and (ConcertTable.city eq it.city) }
                    .firstOrNull()
            if (firstOrNull == null) {
                ConcertEntity.new {
                    title = it.title
                    link = it.link
                    imageLink = it.imageLink
                    time = it.time
                    city = it.city
                }
                GlobalScope.launch {
                    channel.publish(
                        newExchangeName,
                        defaultRoute,
                        it
                    )
                }
            } else if (
                it.link != firstOrNull.link
                || it.imageLink != firstOrNull.imageLink
                || it.time != firstOrNull.time
            ) {
                firstOrNull.link = it.link
                firstOrNull.imageLink = it.imageLink
                firstOrNull.time = it.time
                firstOrNull.refresh()
                GlobalScope.launch {
                    channel.publish(
                        changesExchangeName,
                        defaultRoute,
                        it
                    )
                }

            } else {
            }
        }
    }
}

@ExperimentalStdlibApi
fun concertChangesNotification(host: String) {
    val channel = ConnectionFactory().myConnection(host)
    channel.consume<Concert>(newExchangeName) {
        logger.warn { "CONCERT CHANGES: $it" }
    }
}

@ExperimentalStdlibApi
fun newConcertNotification(host: String) {
    val channel = ConnectionFactory().myConnection(host)
    channel.consume<Concert>(newExchangeName) {
        logger.warn { "NEW CONCERT: $it" }
    }
}

/*
{
    "status": "1",
    "info": [
        {
            "ID": "100000014869347",
            "Title": "《蓝色猜想曲》格什温钢琴四重奏音乐会",
            "Image": "/vancheerfile/images/2019/3/20190320025513802.jpg",
            "Time": "2019.07.11(星期四)   20:00",
            "Links": "/performance/show_100000014869347.html"
        }
    ]
}
*/
data class ConcertOfSZConcert(
    @com.beust.klaxon.Json(name = "ID")
    @SerializedName("ID")
    val id: String,
    @com.beust.klaxon.Json(name = "Title")
    @SerializedName("Title")
    val title: String,
    @com.beust.klaxon.Json(name = "Image")
    @SerializedName("Image")
    val image: String,
    @com.beust.klaxon.Json(name = "Time")
    @SerializedName("Time")
    val time: String,
    @com.beust.klaxon.Json(name = "Links")
    @SerializedName("Links")
    val link: String
) {
    private val _formattedTime: LocalDateTime
        get() {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.ddHHmm")
            return LocalDateTime.parse(time.replace("""[^.\w]*""".toRegex(), ""), formatter)
        }

    fun toConcert(baseUrl: String) = Concert(
        title,
        link.withParentUrl(baseUrl),
        image.withParentUrl(baseUrl),
        _formattedTime,
        City.Shenzhen
    )

}

enum class City {
    Shenzhen
}

object ConcertTable : LongIdTable("concert") {
    val title = text("title")
    val link = text("link")
    val imageLink = text("image_link")
    val time = localDateTime("time")
    val city = enumeration("city", City::class)

    init {
        index(true, title, city)
    }
}

class ConcertEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ConcertEntity>(ConcertTable)

    var title: String by ConcertTable.title
    var link: String by ConcertTable.link
    var imageLink: String by ConcertTable.imageLink
    var time: LocalDateTime by ConcertTable.time
    var city: City by ConcertTable.city

    fun toConcert() = Concert(title, link, imageLink, time, city)
}

data class Concert(
    val title: String,
    val link: String,
    val imageLink: String,
    val time: LocalDateTime,
    val city: City
)

class ResponseOfSZConcert<T>(val status: String, val info: T?)




