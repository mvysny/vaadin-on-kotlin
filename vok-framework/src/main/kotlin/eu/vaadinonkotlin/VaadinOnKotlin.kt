package eu.vaadinonkotlin

import com.github.mvysny.karibudsl.v10.karibuDslI18n
import eu.vaadinonkotlin.vaadin.vt
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

public object VaadinOnKotlin {
    /**
     * Initializes the Vaadin-On-Kotlin framework. Just call this from your context listener.
     */
    public fun init() {
        if (!::asyncExecutor.isInitialized || asyncExecutor.isShutdown) {
            // TomEE also has by default 5 threads, so I guess this is okay :-D
            asyncExecutor = Executors.newScheduledThreadPool(5, threadFactory)
        }
        karibuDslI18n = { key -> vt["dsl.$key"] }
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
            asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)
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
     * The thread factory used by the [async] method. By default, the factory
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
