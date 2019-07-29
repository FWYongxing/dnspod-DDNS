package com.fuyongxing.example.corejava

import com.fuyongxing.kotlin.extension.printEach
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*

fun main() {
    val socket = Socket()
    socket.connect(InetSocketAddress("time-a.nist.gov", 13), 2000)
    val scanner = Scanner(socket.getInputStream(), "UTF-8")
    while (scanner.hasNext()) {
        println(scanner.nextLine())
    }
    InetAddress.getAllByName("time-a.nist.gov").printEach().iterator().asSequence().map { }
    InetAddress.getAllByName("google.com").printEach()
}