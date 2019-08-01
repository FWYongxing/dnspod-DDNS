package com.fuyongxing.networking.dnspod

import com.fuyongxing.kotlin.extension.println
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface HostIPService {
    @GET("get_json.php")
    fun get(): Call<HostIPGetResponse>
}

val hostIPService: HostIPService = Retrofit.Builder()
    .baseUrl("http://api.hostip.info")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(HostIPService::class.java)

data class HostIPGetResponse(
    val ip: String,
    val city: String?,
    val country_code: String?,
    val country_name: String?
)

fun ipLookup(): Single<List<IpLookup>> = Single.defer {
    Single.just<List<IpLookup>>(
        Observable.create<IpLookup> { observer ->
            observer.onNext(IpLookup.fromHostIPGetResponse(hostIPService.get().execute().body()!!))
            observer.onComplete()
        }.blockingIterable().toList()
    )
}

fun main() {
    hostIPService.get().execute().body()!!.println()
}

data class IpLookup(
    val id: Int,
    val name: String,
    val ipv4Address: String?,
    val country: String?,
    val city: String?
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

        fun fromHostIPGetResponse(hostIpgetResponse: HostIPGetResponse) =
            IpLookup(-1, "hostip.info", hostIpgetResponse.ip, hostIpgetResponse.country_name, hostIpgetResponse.city)

    }
}
