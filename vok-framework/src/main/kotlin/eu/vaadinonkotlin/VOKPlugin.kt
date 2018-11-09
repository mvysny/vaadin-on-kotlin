package eu.vaadinonkotlin

/**
 * Implement this interface to be invoked from [VaadinOnKotlin.init] and [VaadinOnKotlin.destroy]
 * methods. The plugins are initialized in random order.
 *
 * VOK uses [java.util.ServiceLoader] to discover instances of this interface. To register, just create a
 * file named `META-INF/services/com.github.vok.framework.VOKPlugin` in `src/main/resources`
 * and list all full class names of classes implementing this interface.
 *
 * You can e.g. use this feature to attach extension fields to [VaadinOnKotlin] and initialize them.
 */
interface VOKPlugin {
    /**
     * Called from [VaadinOnKotlin.init].
     */
    fun init()
    /**
     * Called from [VaadinOnKotlin.destroy].
     */
    fun destroy()
}
