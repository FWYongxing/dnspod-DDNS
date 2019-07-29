package com.fuyongxing.kotlin.extension

import com.google.gson.Gson
import com.rabbitmq.client.*


val gson = Gson()
fun ConnectionFactory.myConnection(host: String): Channel {
    this.host = host
    return newConnection().createChannel()
}

@ExperimentalStdlibApi
inline fun <reified T> Channel.consume(channelName: String, crossinline consumer: (t: T) -> Unit) {
    val channelConsumer = object : DefaultConsumer(this) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray
        ) {
            val result = Gson().fromJson<T>(body.decodeToString())!!
            consumer.invoke(result)
        }
    }
    this.basicConsume(channelName, true, channelConsumer)
}

@ExperimentalStdlibApi
fun <T> Channel.publish(exchangeName: String, route: String, message: T) {
    this.basicPublish(
        exchangeName,
        route,
        true,
        null,
        gson.toJson(message).toByteArray()
    )
}