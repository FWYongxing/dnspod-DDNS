package com.fuyongxing.networking.dnspod

import com.fuyongxing.kotlin.extension.println
import com.fuyongxing.networking.dnspod.iplookup.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun ipLookup(): List<IpLookup> {
    val list = mutableListOf<IpLookup>()

    runBlocking {
        launch {
            try {
                IpLookup.from(hostIPService.get().execute().body()!!).let { list.add(it) }
            } catch (e: Exception) {
            }
        }
        launch {
            try {
                IpLookup.from(ipifyService.get().execute().body()!!).let { list.add(it) }
            } catch (e: Exception) {
            }
        }
        launch {
            try {
                IpLookup.from(ipapiService.get().execute().body()!!).let { list.add(it) }
            } catch (e: Exception) {
            }
        }
        launch {
            try {
                IpLookup.from(ifconfigIPService.get().execute().body()!!).let { list.add(it) }
            } catch (e: Exception) {
            }
        }
    }
    return list
}

fun main() {
    runBlocking {
        launch { hostIPService.get().execute().body()!!.println() }
        launch { ipifyService.get().execute().body()!!.println() }
        launch { ipapiService.get().execute().body()!!.println() }
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
