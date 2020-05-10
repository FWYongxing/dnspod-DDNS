package com.fuyongxing.networking.dnspod

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

fun config(): DNSPodConf {
    val load = ConfigFactory.load()

    if (load.hasPath("dnspod")) {
        return load.extract("dnspod")
    }
    val load2 = File("application.conf").let { file ->
        if (file.exists()) ConfigFactory.parseFile(file) else null
    }
    if (load2?.hasPath("dnspod") == true) {
        return load2.extract("dnspod")
    }

    logger.info { "config file not found." }
    println("input API id (e.g. 7xxx1):")
    val apiId = readLine()
    assert(!apiId.isNullOrEmpty()) { "an API id cannot be empty" }
    println("input API key (e.g. 4508xxxxxxxxxxxxxxxxxxxxxxdb6):")
    val apiKey = readLine()
    assert(!apiKey.isNullOrEmpty()) { "an API key cannot be empty" }
    println("input a domain(e.g. fuyongxing.com):")
    val domain = readLine()
    assert(!domain.isNullOrEmpty()) { "a domain cannot be empty" }
    println("input a sub domain(e.g. blog):")
    val subDomain = readLine()
    assert(!subDomain.isNullOrEmpty()) { "a sub domain cannot be empty" }
    return DNSPodConf(apiId!!, apiKey!!, domain!!, subDomain!!)

}

data class DNSPodConf(
    val apiId: String,
    val apiKey: String,
    val domain: String,
    val subDomain: String,
    var interfaceName: String? = null
)