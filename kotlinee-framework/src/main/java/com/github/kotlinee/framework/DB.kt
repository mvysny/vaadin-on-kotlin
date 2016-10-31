/**
 * Database-related utility methods:
 *
 * * To run a code in a transaction, just call [db]
 * * To obtain the data source, just read the [dataSource] global property.
 * * To obtain an Extended EntityManager, just read the [extendedEntityManager] property.
 */
package com.github.kotlinee.framework

import org.hibernate.internal.SessionImpl
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.PrintWriter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.Connection
import javax.persistence.*
import javax.servlet.ServletRequestEvent
import javax.servlet.ServletRequestListener
import javax.servlet.annotation.WebListener
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("com.example.pokusy.DB")

/**
 * A basic implementation of [DataSource] which polls given factory for connections.
 * @property emf poll this factory for JDBC connections.
 */
class EntityManagerFactoryDataSource(val emf: EntityManagerFactory): DataSource {
    override fun setLogWriter(out: PrintWriter?) = throw UnsupportedOperationException()

    override fun setLoginTimeout(seconds: Int) = throw UnsupportedOperationException()

    override fun getParentLogger() = throw UnsupportedOperationException()

    override fun getLogWriter() = throw UnsupportedOperationException()

    override fun getLoginTimeout() = throw UnsupportedOperationException()

    override fun isWrapperFor(iface: Class<*>?) = iface == DataSource::class.java

    override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException()

    override fun getConnection(): Connection? = emf.createEntityManager().connection

    override fun getConnection(username: String?, password: String?): Connection? = connection
}

private class DisableTransactionControlEMDelegate(val em: EntityManager): EntityManager by em {
    override fun getTransaction(): EntityTransaction? = throw IllegalStateException("Transactions are managed by the db() function")
}

/**
 * Returns a new [EntityManager] which delegates all calls `this`, with the following exceptions:
 * * The [EntityManager.getTransaction] will fail with [IllegalStateException].
 */
fun EntityManager.withTransactionControlDisabled(): EntityManager = DisableTransactionControlEMDelegate(this)

/**
 * Provides a [DataSource] which creates JDBC connections using this factory.
 */
fun EntityManagerFactory.toDataSource(): DataSource = EntityManagerFactoryDataSource(this)

/**
 * Provides the JDBC connection used by this entity manager.
 */
val EntityManager.connection: Connection
get() = PersistenceContext(this).connection

/**
 * Provides access to a single JDBC connection and its [EntityManager], and several utility methods.
 *
 * The [db] function executes block in context of this class.
 * @property em the entity manager reference
 */
class PersistenceContext(val em: EntityManager) : Closeable {
    /**
     * The underlying JDBC connection.
     */
    val connection: Connection by lazy(LazyThreadSafetyMode.NONE) { session.connection() }

    /**
     * Hibernate session; you can e.g. create a criteria query from this session:
     * https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/querycriteria.html
     */
    val session: SessionImpl
    get() = em.unwrap(SessionImpl::class.java)

    override fun close() {
        em.close()
    }
}

private val contexts: ThreadLocal<PersistenceContext> = ThreadLocal()

/**
 * Makes sure given block is executed in a DB transaction. When the block finishes normally, the transaction commits;
 * if the block throws any exception, the transaction is rolled back.
 *
 * Example of use: `db { em.persist() }`
 * @param block the block to run in the transaction. Builder-style provides helpful methods and values, e.g. [PersistenceContext.em]
 */
fun <R> db(block: PersistenceContext.()->R): R {
    var context = contexts.get()
    if (context != null) {
        return context.block()
    } else {
        val em = Kotlinee.entityManagerFactory.createEntityManager()
        context = PersistenceContext(em.withTransactionControlDisabled())
        try {
            contexts.set(context)
            return context.use {
                val transaction = em.transaction
                transaction.begin()
                var success = false
                val result: R
                try {
                    result = it.block()
                    transaction.commit()
                    success = true
                } finally {
                    if (!success) {
                        try {
                            transaction.rollback()
                        } catch (t: Throwable) {
                            log.error("Failed to rollback the transaction", t)
                        }
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
inline fun <reified T: Any> EntityManager.findAll(): List<T> =
    createQuery("select b from ${T::class.java.simpleName} b", T::class.java).resultList

/**
 * Retrieves given JPA entity. Fails if there is no such entity.
 * @param id the entity id
 * @return the JPA instance, not null.
 */
inline fun <reified T: Any> EntityManager.get(id: Any): T =
        find(T::class.java, id) ?: throw IllegalArgumentException("Parameter id: invalid value $id: no such ${T::class.java.simpleName}")

/**
 * Deletes given entity.
 * @param id entity id
 * @return true if the entity was deleted, false if there is no such entity.
 */
inline fun <reified T: Any> EntityManager.deleteById(id: Any): Boolean {
    return createQuery("delete from ${T::class.java.simpleName} b where b.id=:id").setParameter("id", id).executeUpdate() != 0
}

/**
 * Deletes all instances of given JPA entity.
 * @return the number of entities deleted.
 */
inline fun <reified T: Any> EntityManager.deleteAll() = createQuery("delete from ${T::class.java.simpleName}").executeUpdate()

/**
 * [TypedQuery.getSingleResult] works only for simple types such as Long - it does not work when retrieving a single JPA entity
 * and will fail with `NoResultException: No entity found for query` when no entities are found. Often, null is a better alternative.
 * @return the entity or null if no entity was found
 */
fun <T> TypedQuery<T>.singleOrNull(): T? {
    val list = resultList
    if (list.size > 1) throw IllegalStateException("query $this: expected 0 or 1 results but got ${list.size}")
    return list.firstOrNull()
}

/**
 * Internal, do not use. Manages and closes delegate [EntityManager]s for [extendedEntityManager]
 */
@WebListener
class ExtendedEMManager: ServletRequestListener {
    override fun requestDestroyed(sre: ServletRequestEvent?) {
        isOngoingServletRequest.set(null)
        purgeDelegate()
    }

    private fun purgeDelegate() {
        extendedEMDelegate.get()?.apply {
            extendedEMDelegate.set(null)
            close()
        }
    }

    override fun requestInitialized(sre: ServletRequestEvent?) {
        isOngoingServletRequest.set(true)
        purgeDelegate()
    }

    companion object {
        internal fun getExtendedEMDelegate(): EntityManager {
            val ongoingServletRequest = isOngoingServletRequest.get() ?: false
            if (!ongoingServletRequest) throw IllegalStateException("Not called from servlet thread")
            var delegate = extendedEMDelegate.get()
            if (delegate == null) {
                delegate = Kotlinee.entityManagerFactory.createEntityManager()
                extendedEMDelegate.set(delegate)
            }
            return delegate
        }

        private val isOngoingServletRequest = ThreadLocal<Boolean>()
        private val extendedEMDelegate = ThreadLocal<EntityManager>()
    }
}

/**
 * The extended entity manager, which stays valid even after the transaction is committed and
 * the servlet request finishes. Automatically allocates and releases a delegate EntityManager.
 *
 * Can only be used from Vaadin/web servlet thread - cannot be used from async threads.
 */
val extendedEntityManager: EntityManager =
    // cannot use Kotlin delegation here: kotlin will not poll for the delegate repeatedly;
    // instead it will poll once and remember the delegate instance (the EM). The EM will get eventually
    // closed after the http request
    // finishes, which would render the delegator unusable.
    Proxy.newProxyInstance(EntityManager::class.java.classLoader, arrayOf(EntityManager::class.java),
            InvocationHandler { proxy, method, args ->
                if (method!!.name == "close") {
                    // do nothing, the underlying EM is managed by the ExtendedEMManager web listener
                    return@InvocationHandler null
                }
                method.invoke(ExtendedEMManager.getExtendedEMDelegate(), *(args ?: arrayOf()))
            }) as EntityManager
