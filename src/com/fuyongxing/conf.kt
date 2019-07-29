package com.fuyongxing

data class PostgresqlConf(
    val host: String,
    val port: Int,
    val dataBaseName: String,
    val username: String,
    val password: String
)

data class RabbitMQConf(
    val host: String
)

data class DNSPodConf(
    val apiId: Int,
    val apiKey: String,
    val domain: String,
    val subDomain: String
)