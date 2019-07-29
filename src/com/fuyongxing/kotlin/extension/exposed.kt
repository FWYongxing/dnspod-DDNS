package com.fuyongxing.kotlin.extension

import com.fuyongxing.PostgresqlConf
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject
import java.lang.RuntimeException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime


object DatabaseFactory {
    fun init(postgresqlConf: PostgresqlConf) {
        val hikariConfig = HikariConfig()
        hikariConfig.setDriverClassName("org.postgresql.Driver")
        hikariConfig.jdbcUrl =
            "jdbc:postgresql://${postgresqlConf.host}:${postgresqlConf.port}/${postgresqlConf.dataBaseName}"
        hikariConfig.username = postgresqlConf.username
        hikariConfig.password = postgresqlConf.password
        hikariConfig.maximumPoolSize = 10
        hikariConfig.isAutoCommit = false
        hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        hikariConfig.validate()

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            assert(
                "select 1 as one".execAndMap
                { rs ->
                    rs.getString("one") to rs.getInt("one")
                }[0].second == 1
            )
        }

    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}

fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

class jsonb : ColumnType() {
    override fun sqlType() = "jsonb"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "jsonb"
        obj.value = value as String
        stmt.setObject(index, obj)
    }

    override fun valueFromDB(value: Any): Any {
        value as PGobject
        return value.value
    }
}

fun Table.localDateTime(name: String): Column<LocalDateTime> = registerColumn(name, LocalDateTimeColumn())

class LocalDateTimeColumn : ColumnType() {
    override fun sqlType() = "timestamp"

    override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "timestamp"
        obj.value = (value as LocalDateTime).toString()
        stmt.setObject(index, obj)
    }

    override fun valueFromDB(value: Any): Any {
        if (value is LocalDateTime)
            return value
        if (value is Timestamp)
            return value.toLocalDateTime()
        throw RuntimeException("wtf")
    }

}