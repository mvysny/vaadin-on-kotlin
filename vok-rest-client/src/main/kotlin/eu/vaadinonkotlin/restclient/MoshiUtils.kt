package eu.vaadinonkotlin.restclient

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Buffer
import okio.BufferedSource
import java.lang.reflect.Type

/**
 * Parses [json] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Moshi.fromJsonArray(json: String, itemClass: Class<T>): List<T> =
    fromJsonArray(Buffer().writeUtf8(json), itemClass)

/**
 * Parses JSON from a [source] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Moshi.fromJsonArray(source: BufferedSource, itemClass: Class<T>): List<T> {
    val type: Type = Types.newParameterizedType(List::class.java, itemClass)
    return adapter<List<T>>(type).fromJson(source) ?: listOf()
}

/**
 * Parses JSON from a [reader] as a map of items with class [valueClass] and returns that.
 */
public fun <T> Moshi.fromJsonMap(source: BufferedSource, valueClass: Class<T>): Map<String, T> {
    val type: Type = Types.newParameterizedType(Map::class.java, String::class.java, valueClass)
    return adapter<Map<String, T>>(type).fromJson(source) ?: mapOf()
}

/**
 * Parses [json] as a map of items with class [valueClass] and returns that.
 */
public fun <T> Moshi.fromJsonMap(json: String, valueClass: Class<T>): Map<String, T> =
    fromJsonMap(Buffer().writeUtf8(json), valueClass)
