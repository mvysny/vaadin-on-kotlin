package eu.vaadinonkotlin

import java.io.Serializable
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Allows you to add listeners for a particular event into your component like following:
 * ```
 * val onFilterChangeListeners = listeners<OnClickListener>()
 * ```
 */
public inline fun <reified T: Serializable> listeners(): Listeners<T> = Listeners(T::class.java)

/**
 * Allows you to add listeners for a particular event into your component.
 *
 * Say that you have a click listener:
 * ```
 * interface OnClickListener { fun onClick(button: Int) }
 * ```
 * You can add support for click listeners into your Button component easily:
 * ```
 * class Button {
 *     val onClickListeners = listeners<OnClickListener>()
 * }
 * ```
 * The clients can then simply register their listeners as follows:
 * ```
 * val button = Button()
 * button.onClickListeners.add(object : OnClickListener {})
 * ```
 * The button can fire an event for all listeners as follows:
 * ```
 * onClickListeners.fire.onClick(2)
 * ```
 */
public class Listeners<T: Serializable>(private val listenerType: Class<T>):
    Serializable {
    init {
        require(listenerType.isInterface) { "$listenerType must be an interface" }
    }

    private val listeners: MutableSet<T> = mutableSetOf()

    /**
     * Registers a new listener. Registering same listener multiple times has no further effect.
     *
     * The equality of the listener is measured by using the standard [Any.equals] and [Any.hashCode].
     */
    public fun add(listener: T) {
        listeners.add(listener)
    }

    /**
     * Removes the listener. Removing same listener multiple times has no further effect. Does nothing
     * if the listener has not yet been registered.
     *
     * The equality of the listener is measured by using the standard [Any.equals] and [Any.hashCode].
     */
    public fun remove(listener: T) {
        listeners.remove(listener)
    }

    override fun toString(): String =
            "Listeners($listenerType, listeners=$listeners)"

    /**
     * Use the returned value to fire particular event to all listeners.
     *
     * Returns a proxy of type [T]. Any method call on this proxy is propagated to all
     * listeners.
     *
     * Not serializable!
     */
    @Suppress("UNCHECKED_CAST")
    public val fire: T get() = Proxy.newProxyInstance(
        listenerType.classLoader,
        arrayOf(listenerType)
    ) { _, method: Method, args: Array<Any>? ->
        if (method.name == "toString") {
            "fire($this}"
        } else {
            for (listener in listeners) {
                if (args == null) {
                    method.invoke(listener)
                } else {
                    method.invoke(listener, *args)
                }
            }
        }
    } as T
}