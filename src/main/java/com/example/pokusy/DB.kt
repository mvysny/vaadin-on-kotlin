package com.example.pokusy

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

private class ConnectionDS(): DataSource {
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

    override fun getConnection(): Connection? = TransactionContext.create().connection

    override fun getConnection(username: String?, password: String?): Connection? = connection
}

/**
 * The data source.
 */
val dataSource: DataSource = ConnectionDS()

/**
 * Provides the entity manager, the JDBC connection and several utility methods.
 * @property em the entity manager reference
 */
class TransactionContext(val em: EntityManager) : Closeable {
    companion object {
        fun create() = TransactionContext(entityManagerFactory.createEntityManager())
    }
    /**
     * The underlying JDBC connection.
     */
    val connection: Connection by lazy { em.unwrap(SessionImpl::class.java).connection() }
    override fun close() {
        em.close()
    }
}

private val contexts: ThreadLocal<TransactionContext> = ThreadLocal()

/**
 * Makes sure given block is executed in a DB transaction.
 * @param block the block to run in the transaction. Builder-style provides helpful methods and values, e.g. [TransactionContext.em]
 */
fun <R> transaction(block: TransactionContext.()->R): R {
    var context = contexts.get()
    if (context != null) {
        return context.block()
    } else {
        context = TransactionContext.create()
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
