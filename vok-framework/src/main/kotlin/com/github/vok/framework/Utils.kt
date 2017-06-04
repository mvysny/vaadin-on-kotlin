package com.github.vok.framework

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.*

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
