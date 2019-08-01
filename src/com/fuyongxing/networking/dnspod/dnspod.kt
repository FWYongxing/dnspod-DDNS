package com.fuyongxing.networking.dnspod

import com.fuyongxing.kotlin.extension.println
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.TimeUnit


val logger = KotlinLogging.logger {}

fun main() {

    val config = config().blockingGet()
    val name = selectName().blockingGet()
    Observable.interval(0, 1, TimeUnit.MINUTES)
        .flatMap { currentIP(name).toObservable() }
        .zipWith(Observable.interval(0, 1, TimeUnit.MINUTES).flatMap {
            records(
                config.apiId,
                config.apiKey,
                config.domain
            )
        })
        .blockingSubscribe(object : Observer<Pair<String, List<DNSPodGetRecordsResponse.Record>>> {
            override fun onComplete() {
            }

            override fun onSubscribe(d: Disposable) {
            }

            override fun onNext(t: Pair<String, List<DNSPodGetRecordsResponse.Record>>) {
                val ipv4: String = t.first
                val records: List<DNSPodGetRecordsResponse.Record> = t.second
                val matchedRecord = records.firstOrNull { it.name == config.subDomain }
                if (matchedRecord == null) {
                    logger.warn { "none of the following dns record match sub domain: (${config.subDomain})" }
                    println(DNSPodGetRecordsResponse.Record.tableHeader)
                    records.forEach { println(it) }
                    return
                }
                logger.info { "remote:${matchedRecord.value}" }
                logger.info { "local:$ipv4" }
                if (matchedRecord.value == ipv4) {
                    logger.info { "remote ip and local ip are the same. no updating will be executed" }
                    return
                } else {
                    logger.info { "remote ip differs from local ip" }
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
                        return
                    }
                    logger.info { "SUCCESS. remote DNS record ip is now ${response2.body()!!.record.value}" }
                }
            }

            override fun onError(e: Throwable) {
                logger.error { "unexpected error occurred: ${e.message}" }
            }
        }
        )
}


fun selectName(): Single<String> = networkInterfaces().zipWith(ipLookup())
    .map { (interfaces, ipLookups) ->
        if (interfaces.isEmpty()) {
            logger.warn { "No network interfaces found on your computer" }
        }
        NetworkInterfaceData.generateTable(interfaces).println()
        val ipLookupCopy = ipLookups.withIndex().map { (index, ipLookup) ->
            ipLookup.copy(id = interfaces.size + index)
        }
        IpLookup.generateTable(ipLookupCopy).println()
        var name: String? = null
        do {
            println("Select an item as your dynamic DNS IP.")
            println("Input a number range rom 0 to ${interfaces.size + ipLookups.size - 1} :")
            try {
                val id = (readLine() ?: "").toInt()
                name = if (id in 0 until interfaces.size) {
                    val inter = interfaces[id]
                    if (inter.ipv4Address == null) {
                        println("Selected network must have an IPV4 address")
                        continue
                    }
                    inter.name
                } else if (id in interfaces.size until (interfaces.size + ipLookups.size)) {
                    val ipLookup = ipLookups[id - interfaces.size]
                    if (ipLookup.ipv4Address == null) {
                        println("Selected IP lookup must have an IPV4 address")
                        continue
                    }
                    ipLookup.name
                } else {
                    println("a valid number ranges from ${0} to ${interfaces.size + ipLookups.size - 1}")
                    continue
                }
            } catch (e: NumberFormatException) {
                println("Only a valid number is accepted")
            }
        } while (name == null)
        name
    }

fun currentIP(name: String): Single<String> = networkInterfaces()
    .toObservable()
    .flatMap { Observable.fromIterable(it) }
    .filter { it.name == name }
    .map { it.ipv4Address }
    .switchIfEmpty(
        ipLookup().toObservable()
            .flatMap { Observable.fromIterable(it) }
            .filter { it.name == name }
            .map { it.ipv4Address }
    ).take(1)
    .singleOrError()
    .onErrorResumeNext { e ->
        if (e is NoSuchElementException) {
            Single.error { java.lang.Exception("Cannot get IP of $name") }
        } else {
            Single.error { e }
        }
    } as Single<String>
