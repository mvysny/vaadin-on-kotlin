package com.github.kotlinee.framework

import com.github.kotlinee.framework.vaadin.UrlParamShortener
import com.vaadin.server.VaadinService
import com.vaadin.server.VaadinSession
import com.vaadin.ui.UI
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.servlet.http.Cookie
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Kotlinee {
    /**
     * Initializes the KotlinEE framework. Just call this from your context listener.
     */
    fun kotlineeInit() = synchronized(this) {
        // TomEE also has by default 5 threads, so I guess this is okay :-D
        executor = Executors.newScheduledThreadPool(5, threadFactory)
    }

    /**
     * Destroys the KotlinEE framework. Just call this from your context listener.
     */
    fun kotlineeDestroy() = synchronized(this) {
        if (isStarted) {
            executor!!.shutdown()
            executor!!.awaitTermination(1, TimeUnit.DAYS)
            executor = null
            entityManagerFactory.close()
        }
    }

    /**
     * True if [kotlineeInit] has been called.
     */
    val isStarted: Boolean
    get() = synchronized(this) { executor != null }

    private var executor: ScheduledExecutorService? = null

    private fun checkStarted() {
        if (!isStarted) throw IllegalStateException("kotlineeInit() has not been called, or Kotlinee is already destroyed")
    }

    /**
     * The executor used by [async] and [scheduleAtFixedRate]. You can submit your own tasks as you wish.
     */
    val asyncExecutor: ScheduledExecutorService
        get() = synchronized(this) { checkStarted(); executor!! }

    /**
     * The thread factory used by the [async] method. By default the factory
     * creates non-daemon threads named "async-ID".
     *
     * Needs to be set before [kotlineeInit] is called.
     */
    @Volatile
    var threadFactory: ThreadFactory = object : ThreadFactory {
        private val id = AtomicInteger()
        override fun newThread(r: Runnable): Thread? {
            val thread = Thread(r)
            thread.name = "async-${id.incrementAndGet()}"
            return thread
        }
    }

    internal val log = LoggerFactory.getLogger(javaClass)

    /**
     * Used for data persistence - the JDBC/EntityManager/JPA thingy. By default uses the "sample" persistence unit
     * present in `META-INF/persistence.xml` but you can of course use any factory you wish.
     */
    @Volatile
    var entityManagerFactory: EntityManagerFactory = Persistence.createEntityManagerFactory("sample")
    set(value) { field.close(); field = value }

    /**
     * Shorthand for [entityManagerFactory].toDataSource()
     */
    fun getDataSource() = entityManagerFactory.toDataSource()
}

/**
 * Submits a value-returning task for execution and returns a
 * Future representing the pending results of the task. The
 * [Future.get] method will return the task's result upon
 * successful completion.
 *
 * If you would like to immediately block waiting
 * for a task, you can use constructions of the form
 * `result = exec.submit(aCallable).get()`
 *
 * @param block the task to submit
 * @param <R> the type of the task's result
 * @return a Future representing pending completion of the task
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 */
fun <R> async(block: () -> R): Future<R> = Kotlinee.asyncExecutor.submit(Callable<R> {
    try {
        block.invoke()
    } catch (t: Throwable) {
        Kotlinee.log.error("Async failed: $t", t)
        throw t
    }
})

/**
 * Creates and executes a periodic action that becomes enabled first
 * after the given initial delay, and subsequently with the given
 * period; that is executions will commence after
 * `initialDelay` then `initialDelay+period`, then
 * `initialDelay + 2 * period`, and so on.
 *
 * If any execution of the task
 * encounters an exception, subsequent executions are suppressed.
 * Otherwise, the task will only terminate via cancellation or
 * termination of the executor.  If any execution of this task
 * takes longer than its period, then subsequent executions
 * may start late, but will not concurrently execute.
 *
 * @param command the task to execute
 * @param initialDelay the time to delay first execution, in millis. You can use expressions like `5.days + 2.seconds` to compute this value.
 * @param period the period between successive executions, in millis
 * @return a ScheduledFuture representing pending completion of
 *         the task, and whose `get()` method will throw an
 *         exception upon cancellation
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 * @throws IllegalArgumentException if period less than or equal to zero
 */
fun scheduleAtFixedRate(initialDelay: Long, period: Long, command: ()->Unit): ScheduledFuture<*> = Kotlinee.asyncExecutor.scheduleAtFixedRate(
        {
            try {
                command.invoke()
            } catch (t: Throwable) {
                // if nobody is using Future to wait for the result of this op, the exception is lost. better log it here.
                Kotlinee.log.error("Async failed: $t", t)
                throw t
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS)

val Int.days: Long get() = toLong().days
val Long.days: Long get() = TimeUnit.DAYS.toMillis(this)
val Int.hours: Long get() = toLong().hours
val Long.hours: Long get() = TimeUnit.HOURS.toMillis(this)
val Int.minutes: Long get() = toLong().minutes
val Long.minutes: Long get() = TimeUnit.MINUTES.toMillis(this)
val Int.seconds: Long get() = toLong().seconds
val Long.seconds: Long get() = TimeUnit.SECONDS.toMillis(this)

/**
 * Manages session-scoped objects. If the object is not yet present in the session, it is created (using the zero-arg
 * constructor), stored into the session and retrieved.
 *
 * To use this feature, simply define a global property returning desired object as follows:
 * `val Session.loggedInUser: LoggedInUser by SessionScoped.get()`
 * Then simply read this property from anywhere, to retrieve the instance. Note that your class needs to be [Serializable] (required when
 * storing stuff into session).
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock!
 */
class SessionScoped<R>(private val clazz: Class<R>): ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = getOrCreate()

    private fun getOrCreate(): R = checkUIThread().session.getOrCreate()

    private fun VaadinSession.getOrCreate(): R {
        var result: R? = getAttribute(clazz)
        if (result == null) {
            // look up the zero-arg constructor. the constructor should be private if the user follows our recommendations.
            val constructor = clazz.declaredConstructors.first { it.parameterCount == 0 }
            constructor.isAccessible = true
            result = clazz.cast(constructor.newInstance())!!
            setAttribute(clazz, result)
        }
        return result
    }

    companion object {
        /**
         * Gets a provider of given object. The object is created if missing from the session.
         * @param R the object type
         */
        inline fun <reified R> get() where R : kotlin.Any, R : java.io.Serializable = SessionScoped(R::class.java)
    }
}

/**
 * Just a namespace object for attaching your [SessionScoped] objects.
 */
object Session {
    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun get(key: String): Any? = checkUIThread().session.getAttribute(key)

    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun <T: Any> get(key: KClass<T>): T? = checkUIThread().session.getAttribute(key.java)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun set(key: String, value: Any?) = checkUIThread().session.setAttribute(key, value)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun <T: Any> set(key: KClass<T>, value: T?) = checkUIThread().session.setAttribute(key.java, value)

    /**
     * Shortens the URL parameters to a simple number. If you need to pass complex objects as View parameters, then this
     * class is for you.
    */
    val urlParamShortener: UrlParamShortener by SessionScoped.get()
}

/**
 * You can use `Cookies["mycookie"]` to retrieve a cookie named "mycookie" (or null if no such cookie exists.
 * You can also use `Cookies += cookie` to add a pre-created cookie to a session.
 */
object Cookies {
    /**
     * Finds a cookie by name.
     * @param name cookie name
     * @return cookie or null if there is no such cookie.
     */
    operator fun get(name: String): Cookie? = VaadinService.getCurrentRequest().cookies?.firstOrNull { it.name == name }

    /**
     * Overwrites given cookie, or deletes it.
     * @param name cookie name
     * @param cookie the cookie to overwrite. If null, the cookie is deleted.
     */
    operator fun set(name: String, cookie: Cookie?) {
        if (cookie == null) {
            val cookie = Cookie(name, null)
            cookie.maxAge = 0  // delete immediately
            cookie.path = "/"
            VaadinService.getCurrentResponse().addCookie(cookie)
        } else {
            VaadinService.getCurrentResponse().addCookie(cookie)
        }
    }
}

infix operator fun Cookies.plusAssign(cookie: Cookie) = set(cookie.name, cookie)

infix operator fun Cookies.minusAssign(cookie: Cookie) = set(cookie.name, null)

/**
 * Checks that this thread runs with Vaadin UI set.
 * @return the UI instance, not null.
 * @throws IllegalStateException if not run in the UI thread.
 */
fun checkUIThread() = UI.getCurrent() ?: throw IllegalStateException("Not in UI thread")
