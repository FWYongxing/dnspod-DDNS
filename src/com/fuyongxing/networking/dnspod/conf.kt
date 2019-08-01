package com.fuyongxing.networking.dnspod

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

fun config(): Single<DNSPodConf> =
    ConfigFactory.load().let {
        if (it.isEmpty) Observable.empty<Config>() else Observable.just(it)
    }
        .filter { it.hasPath("dnspod") }
        .switchIfEmpty(
            File("application.conf").let { file ->
                if (file.exists()) Observable.just<Config>(ConfigFactory.parseFile(file)) else Observable.empty<Config>()
            }
        )
        .filter { it.hasPath("dnspod") }
        .map { config -> config.extract<DNSPodConf>("dnspod") }
        .switchIfEmpty {observer->
            logger.info{"Config file not found."}
            println("Input API id (e.g. 7xxx1):")
            val apiId = readLine()
            assert(!apiId.isNullOrEmpty()) { "an API id cannot be empty" }
            println("Input API key (e.g. 4508xxxxxxxxxxxxxxxxxxxxxxdb6):")
            val apiKey = readLine()
            assert(!apiKey.isNullOrEmpty()) { "an API key cannot be empty" }
            println("Input a domain(e.g. fuyongxing.com):")
            val domain = readLine()
            assert(!domain.isNullOrEmpty()) { "a domain cannot be empty" }
            println("Input a sub domain(e.g. blog):")
            val subDomain = readLine()
            assert(!subDomain.isNullOrEmpty()) { "a sub domain cannot be empty" }
            observer.onNext(DNSPodConf(apiId!!, apiKey!!, domain!!, subDomain!!))
            observer.onComplete()
        }.singleOrError()

data class DNSPodConf(
    val apiId: String,
    val apiKey: String,
    val domain: String,
    val subDomain: String,
    var interfaceName: String? = null
)