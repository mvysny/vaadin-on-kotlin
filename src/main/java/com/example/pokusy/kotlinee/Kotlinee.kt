package com.example.pokusy.kotlinee

import com.vaadin.server.VaadinSession
import com.vaadin.ui.UI
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val log = LoggerFactory.getLogger("kotlinee")

/**
 * Initializes the KotlinEE framework. Just call this from your context listener.
 */
fun kotlineeInit() {
    // TomEE also has by default 5 threads, so I guess this is okay :-D
    executor = Executors.newScheduledThreadPool(5, AsyncThreadFactory)
}

/**
 * Destroys the KotlinEE framework. Just call this from your context listener.
 */
fun kotlineeDestroy() {
    executor!!.shutdown()
    executor!!.awaitTermination(1, TimeUnit.DAYS)
    executor = null
}

private object AsyncThreadFactory : ThreadFactory {
    private val id = AtomicInteger()
    override fun newThread(r: Runnable): Thread? {
        val thread = Thread(r)
        thread.name = "async-${id.incrementAndGet()}"
        return thread
    }
}

private var executor: ScheduledExecutorService? = null

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
fun <R> async(block: ()->R): Future<R> = executor!!.submit(Callable<R> {
    try {
        block.invoke()
    } catch (t: Throwable) {
        log.error("Async failed: $t", t)
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
 * @param initialDelay the time to delay first execution, in millis. You can use expressions like `5 * TimeUnit.DAYS + 2 * TimeUnit.SECONDS` to compute this value.
 * @param period the period between successive executions, in millis
 * @return a ScheduledFuture representing pending completion of
 *         the task, and whose `get()` method will throw an
 *         exception upon cancellation
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 * @throws IllegalArgumentException if period less than or equal to zero
 */
fun scheduleAtFixedRate(initialDelay: Long, period: Long, command: ()->Unit): ScheduledFuture<*> = executor!!.scheduleAtFixedRate(
        {
            try {
                command.invoke()
            } catch (t: Throwable) {
                log.error("Async failed: $t", t)
                throw t
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS)

infix operator fun Long.times(timeUnit: TimeUnit): Long = timeUnit.toMillis(this)
infix operator fun Int.times(timeUnit: TimeUnit): Long = toLong() * timeUnit

/**
 * Manages session-scoped objects. If the object is not yet present in the session, it is created (using the zero-arg
 * constructor), stored into the session and retrieved.
 *
 * To use this feature, simply define a global property returning desired object as follows:
 * `val loggedInUser: LoggedInUser by SessionScoped.get()`
 * Then simply read this property from anywhere, to retrieve the instance. Note that your class needs to be [Serializable] (required when
 * storing stuff into session).
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock!
 */
class SessionScoped<R>(private val clazz: Class<R>): ReadOnlyProperty<Nothing?, R> {
    override fun getValue(thisRef: Nothing?, property: KProperty<*>): R = getOrCreate()

    private fun getOrCreate(): R = UI.getCurrent().session.getOrCreate()

    private fun VaadinSession.getOrCreate(): R {
        var result: R? = getAttribute(clazz)
        if (result == null) {
            // look up the zero-arg constructor. the constructor should be private if the user follows our recommendations.
            val constructor = clazz.declaredConstructors.first { it.parameterCount == 0 }
            constructor.isAccessible = true
            result = constructor.newInstance() as R
            setAttribute(clazz, result)
        }
        return result!!
    }

    companion object {
        inline fun <reified R> get() where R : kotlin.Any, R : java.io.Serializable = SessionScoped(R::class.java)
    }
}
