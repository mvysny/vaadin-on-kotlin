/**
 * Database-related utility methods:
 *
 * * To run a code in a transaction, just call `transaction { em.persist() }`
 * * To obtain the data source, just read the [dataSource] global property.
 */
package com.example.pokusy.kotlinee

import org.hibernate.internal.SessionImpl
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("com.example.pokusy.DB")

private val entityManagerFactory: EntityManagerFactory = Persistence.createEntityManagerFactory("sample")

private object ConnectionDS: DataSource {
    override fun setLogWriter(out: PrintWriter?) {
        throw UnsupportedOperationException()
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException()
    }

    override fun getParentLogger(): Logger? {
        throw UnsupportedOperationException()
    }

    override fun getLogWriter(): PrintWriter? {
        throw UnsupportedOperationException()
    }

    override fun getLoginTimeout(): Int {
        throw UnsupportedOperationException()
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean = iface == DataSource::class.java

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw UnsupportedOperationException()
    }

    override fun getConnection(): Connection? = PersistenceContext.create().connection

    override fun getConnection(username: String?, password: String?): Connection? = connection
}

/**
 * The data source.
 */
val dataSource: DataSource = ConnectionDS

/**
 * Provides the entity manager, the JDBC connection and several utility methods.
 * @property em the entity manager reference
 */
class PersistenceContext(val em: EntityManager) : Closeable {
    companion object {
        fun create() = PersistenceContext(entityManagerFactory.createEntityManager())
    }
    /**
     * The underlying JDBC connection.
     */
    val connection: Connection by lazy(LazyThreadSafetyMode.NONE) { em.unwrap(SessionImpl::class.java).connection() }
    override fun close() {
        em.close()
    }
}

private val contexts: ThreadLocal<PersistenceContext> = ThreadLocal()

/**
 * Makes sure given block is executed in a DB transaction. When the block finishes normally, the transaction commits;
 * if the block throws any exception, the transaction is rolled back.
 * @param block the block to run in the transaction. Builder-style provides helpful methods and values, e.g. [PersistenceContext.em]
 */
fun <R> transaction(block: PersistenceContext.()->R): R {
    var context = contexts.get()
    if (context != null) {
        return context.block()
    } else {
        context = PersistenceContext.create()
        try {
            contexts.set(context)
            return context.use {
                context.em.transaction.begin()
                var success = false
                val result: R
                try {
                    result = it.block()
                    success = true
                } finally {
                    try {
                        if (success) context.em.transaction.commit() else context.em.transaction.rollback()
                    } catch (t: Throwable) {
                        log.warn("Failed to commit/rollback the transaction", t)
                    }
                }
                result
            }
        } finally {
            contexts.set(null)
        }
    }
}
