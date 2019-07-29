package com.fuyongxing.kotlin.extension

fun Any.println() = this.also { println(this) }

fun <T> Iterator<T>.printEach() =
    this.also { this.forEach { println(it) } }

fun <T> Sequence<T>.printEach() =
    this.forEach { println(it) }

fun <T> Array<T>.printEach() = this.iterator().also { this.forEach { println(it) } }