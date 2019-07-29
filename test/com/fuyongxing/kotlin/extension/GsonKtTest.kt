package com.fuyongxing.kotlin.extension

import com.fuyongxing.kotlin.extension.fromJson
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test

class GsonKtTest {
    @Test
    fun setUp() {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.ddHHmm")
        val ldt = LocalDateTime.parse("2011.11.110101", formatter)
        val temp  = Temp("a",ldt)
        val tempInString = Gson().toJson(temp)
        println(Gson().fromJson(tempInString, Temp::class.java))
        println(Gson().fromJson<Temp>(tempInString))
    }
}

data class Temp(val a: String, val time: LocalDateTime)