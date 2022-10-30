package eu.vaadinonkotlin

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

public object VaadinOnKotlin {
    /**
     * Initializes the Vaadin-On-Kotlin framework. Just call this from your context listener.
     */
    public fun init() {
        if (!::asyncExecutor.isInitialized) {
            // TomEE also has by default 5 threads, so I guess this is okay :-D
            asyncExecutor = Executors.newScheduledThreadPool(5, threadFactory)
        }
        val plugins: List<VOKPlugin> = pluginsLoader.toList()
        plugins.forEach { it.init() }
        isStarted = true
        log.info("Vaadin On Kotlin initialized with plugins ${plugins.map { it.javaClass.simpleName }}")
    }

    /**
     * Destroys the Vaadin-On-Kotlin framework. Just call this from your context listener.
     */
    public fun destroy() {
        if (isStarted) {
            isStarted = false
            Services.singletons.destroy()
            pluginsLoader.forEach { it.destroy() }
            asyncExecutor.shutdown()
            asyncExecutor.awaitTermination(1, TimeUnit.DAYS)
        }
    }

    /**
     * True if [init] has been called.
     */
    @Volatile
    public var isStarted: Boolean = false
        private set

    /**
     * The executor used by [async] and [scheduleAtFixedRate]. You can submit your own tasks as you wish.
     *
     * You can set your own custom executor, but do so before [init] is called.
     */
    @Volatile
    public lateinit var asyncExecutor: ScheduledExecutorService

    /**
     * The thread factory used by the [async] method. By default the factory
     * creates non-daemon threads named "async-ID".
     *
     * Needs to be set before [init] is called.
     */
    @Volatile
    public var threadFactory: ThreadFactory = object : ThreadFactory {
        private val id = AtomicInteger()
        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            thread.name = "async-${id.incrementAndGet()}"
            // not a good idea to create daemon threads: if the executor is not shut
            // down properly and the JVM terminates, daemon threads are killed on the spot,
            // without even calling finally blocks on them, as the JVM halts.
            // See Section 7.4.2 of the "Java Concurrency In Practice" Book for more info.
            return thread
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Discovers VOK plugins, so that they can be inited in [init] and closed on [destroy]. Uses a standard [ServiceLoader]
     * machinery for discovery.
     */
    private val pluginsLoader: ServiceLoader<VOKPlugin> = ServiceLoader.load(VOKPlugin::class.java)
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
 * @param logException if true (default) and the [block] throws an exception, the exception is logged via slf4j.
 * This prevents the exception from being lost silently if nobody calls [Future.get].
 * @return a Future representing pending completion of the task
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 */
public fun <R> async(logException: Boolean = true, block: () -> R): Future<R> = VaadinOnKotlin.asyncExecutor.submit(Callable<R> {
    try {
        block.invoke()
    } catch (t: Throwable) {
        // log the exception - if nobody is waiting on the Future, the exception would have been lost.
        if (logException) {
            LoggerFactory.getLogger(block::class.java)
                .error("Async failed: $t", t)
        }
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
 * @param period the period between successive executions, in millis.
 * @param logException if true (default) and the [command] throws an exception, the exception is logged via slf4j.
 * @return a ScheduledFuture representing pending completion of
 *         the task, and whose `get()` method will throw an
 *         exception upon cancellation
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 * @throws IllegalArgumentException if period less than or equal to zero
 */
public fun scheduleAtFixedRate(initialDelay: Duration, period: Duration, logException: Boolean = true, command: ()->Unit): ScheduledFuture<*>
        = VaadinOnKotlin.asyncExecutor.scheduleAtFixedRate({
            try {
                command.invoke()
            } catch (t: Throwable) {
                // if nobody is using Future to wait for the result of this op, the exception is lost. better log it here.
                if (logException) {
                    LoggerFactory.getLogger(command::class.java)
                        .error("Async failed: $t", t)
                }
                throw t
            }
        }, initialDelay.inWholeMilliseconds, period.inWholeMilliseconds, TimeUnit.MILLISECONDS)
