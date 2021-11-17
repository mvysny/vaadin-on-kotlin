package eu.vaadinonkotlin

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Instant
import java.util.*
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
