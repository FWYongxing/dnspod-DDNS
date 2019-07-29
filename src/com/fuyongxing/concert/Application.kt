package com.fuyongxing.concert

import com.fuyongxing.PostgresqlConf
import com.fuyongxing.RabbitMQConf
import com.fuyongxing.concert.concertproducer.*
import com.fuyongxing.kotlin.extension.DatabaseFactory
import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalStdlibApi
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) = runBlocking {
    val config = ConfigFactory.load()
    val postgresqlConf = config.extract<PostgresqlConf>("postgresql")
    val rabbitMQConf = config.extract<RabbitMQConf>("rabbitmq")
    install(Authentication) {}
    DatabaseFactory.init(postgresqlConf)
    transaction {
        SchemaUtils.create(ConcertTable)
        ConcertTable.deleteAll()
    }
    initMQ(rabbitMQConf.host)
    routing {
        get("/") {
            val concerts = transaction {
                ConcertEntity.all().map { it.toConcert() }.toList()
            }
            call.respondText(Gson().toJson(concerts), ContentType.Application.Json)
        }
    }
}

