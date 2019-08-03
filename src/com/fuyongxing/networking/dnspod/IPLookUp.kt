package com.fuyongxing.networking.dnspod

import com.fuyongxing.kotlin.extension.println
import com.fuyongxing.networking.dnspod.iplookup.*
import io.reactivex.Flowable
import io.reactivex.Flowable.defer
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun ipLookup(): Single<List<IpLookup>> = Single.defer {
    Single.just<List<IpLookup>>(
        Observable.create<IpLookup> { observer ->
            runBlocking {
                launch { observer.onNext(IpLookup.from(hostIPService.get().execute().body()!!)) }
                launch { observer.onNext(IpLookup.from(ipifyService.get().execute().body()!!)) }
                launch { observer.onNext(IpLookup.from(ipapiService.get().execute().body()!!)) }
                launch { observer.onNext(IpLookup.from(the360IPService.get().execute().body()!!)) }
                launch { observer.onNext(IpLookup.from(ifconfigIPService.get().execute().body()!!)) }
            }
            observer.onComplete()
        }.blockingIterable().toList()
    )
}

fun main() {
    runBlocking {
        launch { hostIPService.get().execute().body()!!.println() }
        launch { ipifyService.get().execute().body()!!.println() }
        launch { ipapiService.get().execute().body()!!.println() }
        launch { the360IPService.get().execute().body()!!.println() }
        launch { ifconfigIPService.get().execute().body()!!.println() }
    }
}

data class IpLookup(
    val id: Int,
    val name: String,
    val ipv4Address: String?,
    val country: String? = "unknown",
    val city: String? = "unknown"
) {
    companion object {
        fun generateTable(list: List<IpLookup>) =
            StringBuilder("These IPs are detected by public IP lookup services:").apply {
                appendln()
                appendln()
                append("ID".padEnd(4))
                append("IPV4".padEnd(17))
                append("SITE".padEnd(30))
                appendln()
                list.forEach {
                    append(it.id.toString().padEnd(4))
                    append((it.ipv4Address ?: "").padEnd(17))
                    append(it.name.padEnd(16))
                    appendln()
                }
            }

        fun from(response: HostIPService.GetResponse) =
            IpLookup(-1, "hostip.info", response.ip, response.country_name, response.city)

        fun from(response: IPIFYService.GetResponse) =
            IpLookup(-1, "ipify.org", response.ip)

        fun from(response: IPAPIService.GetResponse) =
            IpLookup(-1, "ipapi.co", response.ip, response.country, response.city)

        fun from(response: IfconfigIPService.GetResponse) =
            IpLookup(-1, "ifconfig.co", response.ip, response.country, response.city)

        fun from(response: The360IPService.GetResponse) =
            IpLookup(-1, "ip.360.cn", response.ip, city = response.location)

    }
}
