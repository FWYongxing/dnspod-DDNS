package com.fuyongxing.kotlin.extension

import com.beust.klaxon.Klaxon

@ExperimentalStdlibApi
inline fun <reified T> Klaxon.parse(byteArray: ByteArray) = this.parse<T>(byteArray.decodeToString())