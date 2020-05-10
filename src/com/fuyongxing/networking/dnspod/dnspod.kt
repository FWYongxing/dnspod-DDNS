package com.fuyongxing.networking.dnspod

import com.fuyongxing.kotlin.extension.println
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.IOException


val logger = KotlinLogging.logger {}

fun main() {

    val config = config()
    val selectedInterfaceName = selectName()
    repeat(Int.MAX_VALUE) { times ->
        if (times > 0) runBlocking {
            delay(60000)
        }
        val records = try {
            records(
                config.apiId,
                config.apiKey,
                config.domain
            )
        } catch (e: IOException) {
            logger.warn { "no sub domains of domain(${config.domain}) are found" }
            return@repeat
        }

        if (records.isEmpty()) {
            logger.warn { "no sub domains of domain(${config.domain}) are found" }
        }

        try {
            val interfaces = networkInterfaces()
            val ipv4 = interfaces.filter { it.name == selectedInterfaceName }.map { it.ipv4Address }.firstOrNull()
                ?: ipLookup().filter { it.name == selectedInterfaceName }.map { it.ipv4Address }.firstOrNull()
            if (ipv4 == null || !ipv4Regex.matches(ipv4)) {
                logger.warn { "selected interface or IP lookup service doesn't have a well formatted IPV4 address" }
                return@repeat
            }
            val matchedRecord = records.firstOrNull { it.name == config.subDomain }
            if (matchedRecord == null) {
                logger.warn { "none of the following dns record match sub domain: (${config.subDomain})" }
                println(DNSPodGetRecordsResponse.Record.tableHeader)
                records.forEach { println(it) }
                return@repeat
            }

            logger.info { "remote IPV4 address: ${matchedRecord.value}" }
            logger.info { "local IPV4 address: $ipv4" }
            if (matchedRecord.value == ipv4) {
                logger.info { "remote IP and local IP are identical, skip updating" }
                return@repeat
            } else {
                logger.info { "remote IP differs from local IP" }
                logger.info { "updating remote DNS record ..." }
                val response2 = dnsPodService.put(
                    "${config.apiId},${config.apiKey}",
                    format,
                    error_on_empty,
                    config.domain,
                    config.subDomain,
                    matchedRecord.id,
                    ipv4,
                    matchedRecord.line_id,
                    matchedRecord.type
                ).execute()
                if (!response2.isSuccessful || (response2.body()?.status?.code ?: "?") != "1") {
                    logger.warn { "error occurred while update record. ${response2.raw()}" }
                    logger.warn { "${response2.body()}" }
                    return@repeat
                }
                logger.info { "SUCCESS. remote DNS record ip is now ${response2.body()!!.record.value}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "unexpected error occurred" }
        }
    }
}


fun selectName(): String {
    val interfaces = networkInterfaces()
    if (interfaces.isEmpty()) {
        logger.warn { "no network interfaces found on your computer" }
    }
    val ipLookups = ipLookup()
    if (ipLookups.isEmpty()) {
        logger.warn { "no public IP detected for your computer" }
    }
    NetworkInterfaceData.generateTable(interfaces).println()
    val ipLookupCopy = ipLookups.withIndex().map { (index, ipLookup) ->
        ipLookup.copy(id = interfaces.size + index)
    }
    IpLookup.generateTable(ipLookupCopy).println()
    var name: String? = null
    do {
        println("select an item as your dynamic DNS IP.")
        println("input a number range rom 0 to ${interfaces.size + ipLookups.size - 1} :")
        try {
            val id = (readLine() ?: "").toInt()
            name = if (id in interfaces.indices) {
                val inter = interfaces[id]
                if (inter.ipv4Address == null) {
                    println("selected network must have an IPV4 address")
                    continue
                }
                inter.name
            } else if (id in interfaces.size until (interfaces.size + ipLookups.size)) {
                val ipLookup = ipLookups[id - interfaces.size]
                if (ipLookup.ipv4Address == null) {
                    println("selected IP lookup must have an IPV4 address")
                    continue
                }
                ipLookup.name
            } else {
                println("a valid number ranges from ${0} to ${interfaces.size + ipLookups.size - 1}")
                continue
            }
        } catch (e: NumberFormatException) {
            println("only a valid number is accepted")
        }
    } while (name == null)
    return name
}