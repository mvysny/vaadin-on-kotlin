package com.example.pokusy

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

/**
 * @author mvy
 */
@WebListener
class Bootstrap: ServletContextListener {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun contextInitialized(sce: ServletContextEvent?) {
        log.info("Starting up")
        log.info("Running DB migrations")
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
        log.info("Creating executor")
        executor = Executors.newScheduledThreadPool(5, AsyncThreadFactory)  // TomEE also has by default 5 :-D
        log.info("Initialization complete")
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        log.info("Shutting down");
        log.info("Waiting for executor to stop")
        executor!!.shutdown()
        executor!!.awaitTermination(1, DAYS)
        executor = null
        log.info("Executor terminated")
        log.info("Shutdown complete")
    }
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
fun <R> async(block: ()->R): Future<R> = executor!!.submit(block)

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
 * @param initialDelay the time to delay first execution, in millis. You can use `
 * @param period the period between successive executions, in millis
 * @return a ScheduledFuture representing pending completion of
 *         the task, and whose `get()` method will throw an
 *         exception upon cancellation
 * @throws RejectedExecutionException if the task cannot be
 *         scheduled for execution
 * @throws IllegalArgumentException if period less than or equal to zero
 */
fun scheduleAtFixedRate(initialDelay: Long, period: Long, command: ()->Unit): ScheduledFuture<*> = executor!!.scheduleAtFixedRate(
        command, initialDelay, period, TimeUnit.MILLISECONDS)

operator fun Long.times(timeUnit: TimeUnit): Long = timeUnit.toMillis(this)
operator fun Int.times(timeUnit: TimeUnit): Long = toLong() * timeUnit
