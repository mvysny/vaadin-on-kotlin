package com.github.vok.framework

import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinResponse
import com.vaadin.server.VaadinService
import com.vaadin.server.VaadinSession
import com.vaadin.ui.UI
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.time.Duration
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.Cookie
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object VaadinOnKotlin {
    /**
     * Initializes the Vaadin-On-Kotlin framework. Just call this from your context listener.
     */
    fun init() = synchronized(this) {
        // TomEE also has by default 5 threads, so I guess this is okay :-D
        executor = Executors.newScheduledThreadPool(5, threadFactory)
        val plugins = pluginsLoader.toList()
        plugins.forEach { it.init() }
        log.info("Vaadin On Kotlin initialized with plugins ${plugins.map { it.javaClass.simpleName }}")
    }

    /**
     * Destroys the Vaadin-On-Kotlin framework. Just call this from your context listener.
     */
    fun destroy() = synchronized(this) {
        if (isStarted) {
            pluginsLoader.forEach { it.destroy() }
            executor!!.shutdown()
            executor!!.awaitTermination(1, TimeUnit.DAYS)
            executor = null
        }
    }

    /**
     * True if [init] has been called.
     */
    val isStarted: Boolean
    get() = synchronized(this) { executor != null }

    private var executor: ScheduledExecutorService? = null

    private fun checkStarted() {
        if (!isStarted) throw IllegalStateException("init() has not been called, or VaadinOnKotlin is already destroyed")
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
     * Needs to be set before [init] is called.
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
     * Discovers VOK plugins, so that they can be inited in [init] and closed on [destroy]. Uses a standard [ServiceLoader]
     * machinery for discovery.
     */
    private val pluginsLoader = ServiceLoader.load(VOKPlugin::class.java)
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
fun <R> async(block: () -> R): Future<R> = VaadinOnKotlin.asyncExecutor.submit(Callable<R> {
    try {
        block.invoke()
    } catch (t: Throwable) {
        // log the exception - if nobody is waiting on the Future, the exception would have been lost.
        LoggerFactory.getLogger(block::class.java).error("Async failed: $t", t)
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
fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, command: ()->Unit): ScheduledFuture<*> = VaadinOnKotlin.asyncExecutor.scheduleAtFixedRate(
        {
            try {
                command.invoke()
            } catch (t: Throwable) {
                // if nobody is using Future to wait for the result of this op, the exception is lost. better log it here.
                LoggerFactory.getLogger(command::class.java).error("Async failed: $t", t)
                throw t
            }
        }, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS)

/**
 * Manages session-scoped objects. If the object is not yet present in the session, it is created (using the zero-arg
 * constructor), stored into the session and retrieved.
 *
 * To use this feature, simply define a global property returning desired object as follows:
 * `val Session.loggedInUser: LoggedInUser by lazySession()`
 * Then simply read this property from anywhere, to retrieve the instance. Note that your class needs to be [Serializable] (required when
 * storing stuff into session).
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock (that is, there is current session available).
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
}

/**
 * Gets a session-retriever for given object. The object is auto-created and bound to the session if missing from the session.
 * The object is bound under the string key of class full name.
 * @param R the object type
 */
inline fun <reified R> lazySession() where R : Any, R : Serializable = SessionScoped(R::class.java)

/**
 * Just a namespace object for attaching your [SessionScoped] objects.
 */
object Session {

    /**
     * Returns the current [VaadinSession]; fails if there is no session, most probably since we are not in the UI thread.
     */
    val current: VaadinSession get() = VaadinSession.getCurrent() ?: throw IllegalStateException("Not in UI thread")

    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun get(key: String): Any? = current.getAttribute(key)

    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun <T: Any> get(key: KClass<T>): T? = current.getAttribute(key.java)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun set(key: String, value: Any?) = current.setAttribute(key, value)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun <T: Any> set(key: KClass<T>, value: T?) = current.setAttribute(key.java, value)
}

val currentRequest: VaadinRequest get() = VaadinService.getCurrentRequest() ?: throw IllegalStateException("No current request")
val currentResponse: VaadinResponse get() = VaadinService.getCurrentResponse() ?: throw IllegalStateException("No current response")

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
    operator fun get(name: String): Cookie? = currentRequest.cookies?.firstOrNull { it.name == name }

    /**
     * Overwrites given cookie, or deletes it.
     * @param name cookie name
     * @param cookie the cookie to overwrite. If null, the cookie is deleted.
     */
    operator fun set(name: String, cookie: Cookie?) {
        if (cookie == null) {
            val newCookie = Cookie(name, null)
            newCookie.maxAge = 0  // delete immediately
            newCookie.path = "/"
            currentResponse.addCookie(newCookie)
        } else {
            currentResponse.addCookie(cookie)
        }
    }
}

infix operator fun Cookies.plusAssign(cookie: Cookie) = set(cookie.name, cookie)

infix operator fun Cookies.minusAssign(cookie: Cookie) = set(cookie.name, null)

/**
 * Checks that this thread runs with Vaadin UI set.
 * @return the UI instance, not null.
 * @throws IllegalStateException if not run in the UI thread or [UI.init] is ongoing.
 */
fun checkUIThread() = UI.getCurrent() ?: throw IllegalStateException("Not in UI thread, or UI.init() is currently ongoing")
