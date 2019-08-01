package com.fuyongxing.networking.dnspod

import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

const val format: String = "json"
const val error_on_empty: String = "yes"

interface DNSPodService {
    @FormUrlEncoded
    @POST("Record.List")
    fun get(
        @Field("login_token") login_token: String,
        @Field("format") format: String,
        @Field("error_on_empty") error_on_empty: String,
        @Field("domain") domain: String,
        @Field("sub_domain") sub_domain: String?
    ): Call<DNSPodGetRecordsResponse>

    @FormUrlEncoded
    @POST("Record.Modify")
    fun put(
        @Field("login_token") login_token: String,
        @Field("format") format: String,
        @Field("error_on_empty") error_on_empty: String,
        @Field("domain") domain: String,
        @Field("sub_domain") sub_domain: String,
        @Field("record_id") record_id: String,
        @Field("value") value: String,
        @Field("record_line_id") record_line_id: String,
        @Field("record_type") record_type: String = "A"
    ): Call<DNSPodPutRecordResponse>
}

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://dnsapi.cn/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val dnsPodService: DNSPodService = retrofit.create(DNSPodService::class.java)

data class Status(
    val code: String,
    val created_at: String,
    val message: String
)

data class DNSPodGetRecordsResponse(
    val status: Status,
    val domain: Domain,
    val info: Info,
    val records: List<Record>
) {
    data class Domain(
        val ext_status: String,
        val grade: String,
        val id: Int,
        val name: String,
        val owner: String,
        val punycode: String,
        val ttl: Int
    )

    data class Info(
        val record_total: String,
        val records_num: String,
        val sub_domains: String
    )

    data class Record(
        val id: String,
        val name: String,
        val value: String,
        val status: String,
        val enabled: String,
        val line: String,
        val line_id: String,
        val monitor_status: String,
        val mx: String,
        val remark: String,
        val ttl: String,
        val type: String,
        val updated_on: String,
        val use_aqb: String,
        val weight: Any
    ) {
        companion object {
            val tableHeader = "SUB DOMAIN".padEnd(12) +
                    "IP ADDRESS".padEnd(17)
        }

        override fun toString(): String {
            return "${name.padEnd(12)}${value.padEnd(17)}"
        }
    }
}

data class DNSPodPutRecordResponse(
    val status: Status,
    val record: Record
) {
    data class Record(
        val id: String,
        val name: String,
        val value: String,
        val status: String
    )
}


fun records(apiId:String,apiKey:String,domain:String): Observable<List<DNSPodGetRecordsResponse.Record>> = Observable.create<List<DNSPodGetRecordsResponse.Record>> { observer ->
    val response =
        dnsPodService.get(
            "$apiId,$apiKey",
            format,
            error_on_empty,
            domain,
            null
        ).execute()
    if (!response.isSuccessful || (response.body()?.status?.code ?: "?") != "1") {
        logger.warn { "error occurred while update record. ${response.raw()}" }
        logger.warn { "${response.body()}" }
        throw java.lang.Exception("error occurred when getting DNS records")
    }
    observer.onNext(response.body()!!.records)
    observer.onComplete()
}.filter { records ->
    (!records.isNullOrEmpty()).also {
        if (!it) logger.warn { "no sub domains of domain($domain) are found" }
    }
}