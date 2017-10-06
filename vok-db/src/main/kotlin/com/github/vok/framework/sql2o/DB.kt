package com.github.vok.framework.sql2o

import com.github.vok.framework.VOKPlugin
import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.framework.closeQuietly
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.sql2o.Connection
import org.sql2o.Sql2o
import org.sql2o.converters.Convert
import org.sql2o.converters.Converter
import org.sql2o.converters.ConverterException
import org.sql2o.converters.ConvertersProvider
import org.sql2o.quirks.QuirksDetector
import java.io.Closeable
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

class Sql2oVOKPlugin : VOKPlugin {

    override fun init() {
        require(!hikariConfig.jdbcUrl.isNullOrBlank()) { "Please set your database JDBC url, username and password into the VaadinOnKotlin.dataSourceConfig field prior initializing VoK. " }
        dataSource = HikariDataSource(hikariConfig)
    }

    override fun destroy() {
        dataSource?.closeQuietly()
        dataSource = null
    }

    companion object {
        @Volatile
        internal var dataSource: HikariDataSource? = null
    }
}

private val hikariConfig = HikariConfig()

/**
 * Configure this before initializing VoK. At minimum you need to set [HikariConfig.dataSource], or
 * [HikariConfig.driverClassName], [HikariConfig.jdbcUrl], [HikariConfig.username] and [HikariConfig.password].
 */
val VaadinOnKotlin.dataSourceConfig: HikariConfig get() = hikariConfig

val VaadinOnKotlin.dataSource: DataSource get() = Sql2oVOKPlugin.dataSource!!

/**
 * Provides access to a single JDBC connection and its [Connection], and several utility methods.
 *
 * The [db] function executes block in context of this class.
 * @property em the entity manager reference
 */
class PersistenceContext(val con: Connection) : Closeable {
    /**
     * The underlying JDBC connection.
     */
    val jdbcConnection: java.sql.Connection get() = con.jdbcConnection

    override fun close() {
        con.close()
    }
}

private val contexts: ThreadLocal<PersistenceContext> = ThreadLocal()

/**
 * Makes sure given block is executed in a DB transaction. When the block finishes normally, the transaction commits;
 * if the block throws any exception, the transaction is rolled back.
 *
 * Example of use: `db { con.query() }`
 * @param block the block to run in the transaction. Builder-style provides helpful methods and values, e.g. [PersistenceContext.con]
 */
fun <R> db(block: PersistenceContext.()->R): R {
    var context = contexts.get()
    if (context != null) {
        return context.block()
    } else {
        val sql2o = Sql2o(VaadinOnKotlin.dataSource, QuirksDetector.forURL(hikariConfig.jdbcUrl))
        context = PersistenceContext(sql2o.beginTransaction())
        try {
            contexts.set(context)
            return context.use {
                var success = false
                val result: R = try {
                    it.block().also {
                        context.con.commit()
                        success = true
                    }
                } finally {
                    if (!success) context.con.rollback()
                }
                result
            }
        } finally {
            contexts.set(null)
        }
    }
}

private class LocalDateConverter : Converter<LocalDate> {
    override fun toDatabaseParam(`val`: LocalDate?): Any? = `val`  // jdbc 4.2 supports LocalDate fully
    override fun convert(value: Any?): LocalDate? = when (value) {
        null -> null
        is LocalDate -> value
        is java.sql.Date -> value.toLocalDate()
        is java.util.Date -> value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        else -> throw ConverterException("Failed to convert $value of type ${value.javaClass} to LocalDate")
    }
}

class VokConvertersProvider : ConvertersProvider {
    override fun fill(mapToFill: MutableMap<Class<*>, Converter<*>>) {
        mapToFill.apply {
            put(LocalDate::class.java, LocalDateConverter())
        }
    }
}
