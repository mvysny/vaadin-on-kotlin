package com.github.vok.framework

import org.slf4j.LoggerFactory
import java.io.Closeable
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
