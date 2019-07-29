package com.fuyongxing.kotlin.extension

fun String.withParentUrl(parent: String) = "${parent.trimEnd('/')}/${this.trimStart('/')}"