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
import javax.persistence.TypedQuery
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

/**
 * Returns all JPA entities of given type.
 * @param clazz the JPA entity class, not null.
 * @param  entity type
 * @return all classes, may be empty.
 */
fun <T> EntityManager.findAll(clazz: Class<T>): List<T> {
    return createQuery("select b from ${clazz.simpleName} b", clazz).getResultList()
}

/**
 * Finds given JPA entity. Fails if there is no such entity.
 * @param clazz entity class, not null.
 * @param id the entity id
 * @return the JPA instance, not null.
 */
fun <T> EntityManager.findById(clazz: Class<T>, id: Any): T {
    return find(clazz, id) ?: throw IllegalArgumentException("Parameter id: invalid value $id: no such ${clazz.simpleName}")
}

/**
 * Deletes given entity.
 * @param clazz entity class
 * @param id entity id
 * @return true if the entity was deleted, false if there is no such entity.
 */
fun EntityManager.deleteById(clazz: Class<*>, id: Any): Boolean {
    return createQuery("delete from ${clazz.simpleName} b where b.id=:id", clazz).setParameter("id", id).executeUpdate() != 0
}

/**
 * Deletes all instances of given JPA entity.
 * @param clazz the JPA class to delete.
 */
fun EntityManager.deleteAll(clazz: Class<*>) {
    createQuery("delete from ${clazz.simpleName}", clazz).executeUpdate()
}

/**
 * [TypedQuery.getSingleResult] funguje iba na primitivnych typoch ako Long atd, nefunguje na JPA entitach.
 * @param query the query
 * @param  the entity type
 * @return the entity or null if no entity was found
 */
fun <T> TypedQuery<T>.single(): T? {
    val list = resultList
    val size = list.size
    if (size > 1) {
        throw RuntimeException("query $this: expected 0 or 1 results but got $size")
    } else if (size == 1) {
        return list[0]
    } else {
        return null
    }
}
