package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Reader
import java.io.StringReader
import java.lang.reflect.Type

/**
 * Parses [json] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Gson.fromJsonArray(json: String, itemClass: Class<T>): List<T> =
    fromJsonArray(StringReader(json), itemClass)

/**
 * Parses JSON from a [reader] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Gson.fromJsonArray(reader: Reader, itemClass: Class<T>): List<T> {
    val type: Type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(reader, type)
}

/**
 * Parses JSON from a [reader] as a map of items with class [valueClass] and returns that.
 */
public fun <T> Gson.fromJsonMap(reader: Reader, valueClass: Class<T>): Map<String, T> {
    val type: Type = TypeToken.getParameterized(
        Map::class.java,
        String::class.java,
        valueClass
    ).type
    return fromJson<Map<String, T>>(reader, type)
}

/**
 * Parses [json] as a map of items with class [valueClass] and returns that.
 */
public fun <T> Gson.fromJsonMap(json: String, valueClass: Class<T>): Map<String, T> =
    fromJsonMap(StringReader(json), valueClass)
