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
    /**
     * Internal: Only serves for singleton service definition. Don't use to look up your application services.
     *
     * Register your singleton services as `val Services.yourService: YourService get() = singletons.getOrCreate(YourService::class) { YourService() }`.
     * The service will be created lazily, and at most once.
     */
    public val singletons: Singletons = Singletons()
}

/**
 * A registry of singleton services.
 */
public class Singletons {
    private val instances = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Registers the service instance, but only if it hasn't been registered yet. Fails if the service is already registered.
     *
     * Can be used for testing purposes, to allow registering of mock/fake services before the app's actual Bootstrap code is called.
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
