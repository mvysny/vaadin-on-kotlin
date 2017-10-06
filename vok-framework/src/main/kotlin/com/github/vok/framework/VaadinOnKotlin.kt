package com.github.vok.framework

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

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
