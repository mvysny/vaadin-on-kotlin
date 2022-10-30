package eu.vaadinonkotlin

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A namespace object for attaching your service classes.
 *
 * Register your stateless services simply as `val Services.yourService: YourService get() = YourService()`
 *
 * Register your singleton services as `val Services.yourService: YourService get() = singletons.getOrCreate(YourService::class) { YourService() }`.
 * The service will be created lazily, and at most once.
 *
 * WARNING: Calling [VaadinOnKotlin.destroy] will clean up the singleton instances. If you need to
 * deinit your services, you have to do it beforehand.
 *
 * See [VoK: Services](https://www.vaadinonkotlin.eu/services/) documentation for more details.
 */
public object Services {
    public val singletons: Singletons get() = Singletons
}

/**
 * A registry of singleton services.
 */
public object Singletons {
    private val instances = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Registers the service instance, but only if it hasn't been registered yet.
     *
     * For testing purposes only, to allow registering of mock/fake services.
     */
    public operator fun <T: Any> set(serviceClass: KClass<T>, instance: T) {
        check(instances.putIfAbsent(serviceClass.java, instance) == null) { "Service $serviceClass is already registered" }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T: Any> getOrCreate(serviceClass: KClass<T>, instanceInitializer: () -> T): T {
        check(VaadinOnKotlin.isStarted) { "VoK is not started" }
        return instances.computeIfAbsent(serviceClass.java) { instanceInitializer() } as T
    }

    internal fun destroy() {
        instances.clear()
    }
}
