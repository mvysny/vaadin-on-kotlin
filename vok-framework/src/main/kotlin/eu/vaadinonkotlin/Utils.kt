package eu.vaadinonkotlin

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.*
import java.io.*
import java.lang.reflect.Method
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Closes [this] quietly - if [Closeable.close] fails, an INFO message is logged. The exception is not
 * rethrown.
 */
public fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: Exception) {
        LoggerFactory.getLogger(javaClass).info("Failed to close $this", e)
    }
}

public val Instant.toDate: Date get() = Date(toEpochMilli())

public fun Iterable<String?>.filterNotBlank(): List<String> =
        filterNotNull().filter { it.isNotBlank() }

public infix operator fun Instant.plus(other: Duration): Instant = this.plus(other.toJavaDuration())
public infix operator fun Date.plus(other: Duration): Date = Date(time + other.inWholeMilliseconds)

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
public class Listeners<T: Serializable>(private val listenerType: Class<T>): Serializable {
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
    public val fire: T get() = Proxy.newProxyInstance(listenerType.classLoader, arrayOf(listenerType)) { _, method: Method, args: Array<Any>? ->
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

/**
 * Converts this class to its non-primitive counterpart. For example, converts `int.class` to `Integer.class`.
 * @return converts class of primitive type to appropriate non-primitive class; other classes are simply returned as-is.
 */
@Suppress("UNCHECKED_CAST")
public val <T> Class<T>.nonPrimitive: Class<T>
    get() = when (this) {
        Integer.TYPE -> Integer::class.java as Class<T>
        java.lang.Long.TYPE -> Long::class.java as Class<T>
        java.lang.Float.TYPE -> Float::class.java as Class<T>
        java.lang.Double.TYPE -> java.lang.Double::class.java as Class<T>
        java.lang.Short.TYPE -> Short::class.java as Class<T>
        java.lang.Byte.TYPE -> Byte::class.java as Class<T>
        else -> this
    }
