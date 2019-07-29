package com.fuyongxing.networking

import com.fuyongxing.DNSPodConf
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.awt.image.RescaleOp
import java.io.File
import java.net.NetworkInterface.getNetworkInterfaces
import java.util.concurrent.TimeUnit

interface DNSPodService {
    @FormUrlEncoded
    @POST("Record.List")
    fun get(
        @Field("login_token") login_token: String,
        @Field("format") format: String,
        @Field("error_on_empty") error_on_empty: String,
        @Field("domain") domain: String,
        @Field("sub_domain") sub_domain: String?
    ): Call<DNSPodGetRecordsResponse>

    @FormUrlEncoded
    @POST("Record.Modify")
    fun put(
        @Field("login_token") login_token: String,
        @Field("format") format: String,
        @Field("error_on_empty") error_on_empty: String,
        @Field("domain") domain: String,
        @Field("sub_domain") sub_domain: String,
        @Field("record_id") record_id: String,
        @Field("value") value: String,
        @Field("record_line_id") record_line_id: String,
        @Field("record_type") record_type: String = "A"
    ): Call<DNSPodPutRecordResponse>
}

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://dnsapi.cn/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val dnsPodService: DNSPodService = retrofit.create(DNSPodService::class.java)!!

const val format: String = "json"
const val error_on_empty: String = "yes"

val logger = KotlinLogging.logger {}
val ipv4Regex = "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}".toRegex()
val macRegex = "((\\w+:){5}\\w+)".toRegex()
fun main() {
    Observable.interval(0, 1, TimeUnit.MINUTES)
        .onErrorResumeNext(Observable.empty())
        .blockingSubscribe(object : Observer<Long> {
            override fun onComplete() {
            }

            override fun onSubscribe(d: Disposable) {
            }

            override fun onNext(t: Long) {
                cycle()
            }

            override fun onError(e: Throwable) {
                logger.error { "unexpected error occurred:" }
                e.printStackTrace()
                logger.error { "will retry in a minute" }
            }

        })

}

fun cycle() {

    val config = ConfigFactory.parseFile(File("application.conf"))
    val dnsPodConf = config.extract<DNSPodConf>("dnspod")
    val file = File("selectedNetworkInterfaceNameOfDNSPod")
    val networkInterfaceName: String? =
        if (file.exists()) {
            file.readText()
        } else {
            null
        }
    val interfaces = getNetworkInterfaces().toList()
    if (interfaces.isEmpty()) {
        logger.warn { "no network interface found." }
        return
    }
    val list = interfaces.withIndex().map { (index, interface_) ->
        val ipv4Address = interface_.inetAddresses
            .asSequence()
            .map { ipv4Regex.find(it.hostAddress) }
            .filterNotNull()
            .map { it.value }
            .firstOrNull()
        val macAddress = interface_.inetAddresses
            .asSequence()
            .map { macRegex.find(it.hostAddress) }
            .filterNotNull()
            .map { it.value }
            .firstOrNull()
        val name = interface_.displayName
        NetworkInterfaceData(index, name, ipv4Address, macAddress)
    }
    if (list.isEmpty()) {
        logger.warn { "no network interfaces are found on your computer" }
        return
    }
    var ipv4: String? = null
    if (networkInterfaceName != null) {
        val interfaceData = list.firstOrNull { it.name == networkInterfaceName }
        if (interfaceData?.ipv4Address == null) {
            println("found these network interfaces on your computer:")
            println()
            println(NetworkInterfaceData.tableHeader)
            list.forEach { println(it) }
            println()
            if (interfaceData == null) {
                println("network interface(name=$networkInterfaceName) not found. ")
                println("check your network config or delete the 'selectedNetworkInterfaceNameOfDNSPod' file")
            } else {
                println("selected network must have a IPV4 address")
            }
            return
        }
        ipv4 = interfaceData.ipv4Address
    }
    if (ipv4 == null) {
        println("found these network interfaces on your computer:")
        println()
        println(NetworkInterfaceData.tableHeader)
        list.forEach { println(it) }
        println()
        do {
            println("select the ID of network interface:")
            var networkInterfaceData: NetworkInterfaceData?
            try {
                networkInterfaceData = list[readLine()!!.toInt()]
                ipv4 = networkInterfaceData.ipv4Address
            } catch (e: Exception) {
                println(e)
                continue
            }
            if (ipv4 == null) {
                println("selected network must have a IPV4 address")
                continue
            }
            file.writeText(networkInterfaceData.name)
        } while (ipv4 == null)
    }

    val response =
        dnsPodService.get(
            "${dnsPodConf.apiId},${dnsPodConf.apiKey}",
            format,
            error_on_empty,
            dnsPodConf.domain,
            null
        ).execute()
    if (!response.isSuccessful) {
        logger.error { "error occurred while retrieving dns records. ${response.raw()}" }
    }
    val records = response.body()!!.records
    if (records.isNullOrEmpty()) {
        logger.warn { "no sub domains of domain(${dnsPodConf.domain}) are found" }
        return
    }
    val matchedRecord = records.firstOrNull { it.name == dnsPodConf.subDomain }
    if (matchedRecord == null) {
        logger.warn { "none of the following dns record match sub domain: (${dnsPodConf.subDomain})" }
        println(Record.tableHeader)
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
            "${dnsPodConf.apiId},${dnsPodConf.apiKey}",
            format,
            error_on_empty,
            dnsPodConf.domain,
            dnsPodConf.subDomain,
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

data class NetworkInterfaceData(
    val id: Int,
    val name: String,
    val ipv4Address: String?,
    val macAddress: String?

) {
    companion object {
        val tableHeader = "ID".padEnd(4) +
                "IPV4".padEnd(17) +
                "MAC".padEnd(30) +
                "NAME"
    }

    override fun toString() =
        id.toString().padEnd(4) +
                (ipv4Address ?: "").padEnd(17) +
                (macAddress ?: "").padEnd(30) +
                name
}


data class DNSPodGetRecordsResponse(
    val status: Status,
    val domain: Domain,
    val info: Info,
    val records: List<Record>
)

data class DNSPodPutRecordResponse(
    val status: Status,
    val record: Record2
)

data class Domain(
    val ext_status: String,
    val grade: String,
    val id: Int,
    val name: String,
    val owner: String,
    val punycode: String,
    val ttl: Int
)

data class Info(
    val record_total: String,
    val records_num: String,
    val sub_domains: String
)

data class Record(
    val id: String,
    val name: String,
    val value: String,
    val status: String,
    val enabled: String,
    val line: String,
    val line_id: String,
    val monitor_status: String,
    val mx: String,
    val remark: String,
    val ttl: String,
    val type: String,
    val updated_on: String,
    val use_aqb: String,
    val weight: Any
) {
    companion object {
        val tableHeader = "SUB DOMAIN".padEnd(12) +
                "IP ADDRESS".padEnd(17)
    }

    override fun toString(): String {
        return "${name.padEnd(12)}${value.padEnd(17)}"
    }
}

data class Status(
    val code: String,
    val created_at: String,
    val message: String
)

data class Record2(
    val id: String,
    val name: String,
    val value: String,
    val status: String
)