package com.fuyongxing.kotlin.extension

import com.google.common.reflect.TypeToken
import com.google.gson.Gson

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
