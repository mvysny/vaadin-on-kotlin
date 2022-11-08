package eu.vaadinonkotlin

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * A namespace object for attaching your service classes.
 *
 * Register your stateless services simply as `val Services.yourService: YourService get() = YourService()`
 *
 * Register your singleton services as `val Services.yourService: YourService get() = singletons.getOrCreate { YourService() }`.
 * The service will be created lazily, and at most once.
 *
 * WARNING: Calling [VaadinOnKotlin.destroy] will clean up the singleton instances. If you need to
 * de-init your services, you have to do it beforehand.
 *
 * See [VoK: Services](https://www.vaadinonkotlin.eu/services/) documentation for more details.
 */
public object Services {
    /**
     * Internal: Only serves for singleton service definition. Don't use to look up your application services.
     *
     * Register your singleton services as `val Services.yourService: YourService get() = singletons.getOrCreate { YourService() }`.
     * The service will be created lazily, and at most once.
     */
    public val singletons: Singletons = Singletons()
}

/**
 * A registry of JVM singleton services (= there's at most 1 instance of the service
 * class in the JVM).
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

    /**
     * Looks up a service by its [serviceClass] in the list of JVM singletons. If it's missing,
     * the [instanceInitializer] will be called, to create the instance of the service.
     *
     * The [instanceInitializer] will be called at most once; while the initializer is running,
     * any further lookups of the same service will block until the initializer is ready.
     *
     * Fails if [VaadinOnKotlin.isStarted] is false. That means that in your bootstrap, first
     * call [VaadinOnKotlin.init], and only then start initializing your singleton services.
     * During shutdown, first de-init all of your singleton services, and only then call
     * [VaadinOnKotlin.destroy].
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T: Any> getOrCreate(serviceClass: KClass<T>, instanceInitializer: () -> T): T {
        check(VaadinOnKotlin.isStarted) { "VoK is not started" }
        return instances.computeIfAbsent(serviceClass.java) { instanceInitializer() } as T
    }

    /**
     * There's an inherent problem with this function. Consider the following use-case:
     * ```
     * interface MyService {}
     * class MyServiceImpl : MyService
     * class MyServiceTestingFake : MyService
     * val Services.getOrCreate { MyServiceImpl() } // will T be MyService or MyServiceImpl? Actually, the latter, which makes it impossible to fake the service.
     * ```
     */
    @Deprecated("The service is stored under the key T, however it's up to Kotlin to guess the value of T. Please use the other getOrCreate() function and specify T explicitly")
    public inline fun <reified T: Any> getOrCreate(noinline instanceInitializer: () -> T): T =
        getOrCreate(T::class, instanceInitializer)

    /**
     * Removes all singleton instances; further service lookups will create a new instance.
     */
    internal fun destroy() {
        instances.clear()
    }
}
