package com.fuyongxing.networking.dnspod

import java.net.NetworkInterface

val ipv4Regex = "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}".toRegex()
val macRegex = "((\\w+:){5}\\w+)".toRegex()

fun networkInterfaces() =
    NetworkInterface.getNetworkInterfaces().toList().withIndex().map { (index, interface_) ->
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
    }.toList()

data class NetworkInterfaceData(
    val id: Int,
    val name: String,
    val ipv4Address: String?,
    val macAddress: String?
) {
    companion object {
        fun generateTable(list: List<NetworkInterfaceData>) =
            StringBuilder("these network interfaces are found on your computer:").apply {
                appendln()
                appendln()
                append("ID".padEnd(4))
                append("IPV4".padEnd(17))
                append("MAC".padEnd(30))
                append("NAME")
                appendln()
                list.forEach {
                    append(it.id.toString().padEnd(4))
                    append((it.ipv4Address ?: "").padEnd(17))
                    append((it.macAddress ?: "").padEnd(30))
                    append(it.name)
                    appendln()
                }
            }
    }
}