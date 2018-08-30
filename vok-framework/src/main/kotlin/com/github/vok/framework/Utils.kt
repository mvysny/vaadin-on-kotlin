package com.github.vok.framework

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.reflect.Proxy
import java.time.Duration
import java.time.Instant
import java.util.*
import java.io.*
import kotlin.reflect.KClass

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: Exception) {
        LoggerFactory.getLogger(javaClass).info("Failed to close $this", e)
    }
}

val Instant.toDate: Date get() = Date(toEpochMilli())

fun Iterable<String?>.filterNotBlank() = filterNotNull().filter { it.isNotBlank() }

val Int.days: Duration get() = toLong().days
val Long.days: Duration get() = Duration.ofDays(this)
val Int.hours: Duration get() = toLong().hours
val Long.hours: Duration get() = Duration.ofHours(this)
val Int.minutes: Duration get() = toLong().minutes
val Long.minutes: Duration get() = Duration.ofMinutes(this)
val Int.seconds: Duration get() = toLong().seconds
val Long.seconds: Duration get() = Duration.ofSeconds(this)

operator fun Duration.times(other: Int): Duration = multipliedBy(other.toLong())

inline fun <reified T: Serializable> Listeners() = Listeners(T::class)
class Listeners<T: Serializable>(val listenerType: KClass<T>): Serializable {
    init {
        require(listenerType.java.isInterface) { "$listenerType must be an interface" }
    }
    private val listeners = LinkedList<T>()
    fun add(listener: T) {
        listeners.add(listener)
    }
    fun remove(listener: T) {
        listeners.remove(listener)
    }
    @Suppress("UNCHECKED_CAST")
    val fire: T = Proxy.newProxyInstance(listenerType.java.classLoader, arrayOf(listenerType.java)) { _, method, args ->
        for (listener in listeners) {
            method.invoke(listener, *args)
        }
    } as T
}
