package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import eu.vaadinonkotlin.VOKPlugin
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type

/**
 * Destroys the [OkHttpClient] including the dispatcher, connection pool, everything. WARNING: THIS MAY AFFECT
 * OTHER http clients if they share e.g. dispatcher executor service.
 */
public fun OkHttpClient.destroy() {
    dispatcher.executorService.shutdown()
    connectionPool.evictAll()
    cache?.close()
}

/**
 * Fails if the response is not in 200..299 range; otherwise returns [this].
 * @throws IOException if the response is not in 200..299 ([Response.isSuccessful] returns false). Uses [FileNotFoundException] for 404.
 */
public fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val msg = "$code: ${body!!.string()} (${request.url})"
        if (code == 404) throw FileNotFoundException(msg)
        throw IOException(msg)
    }
    return this
}

/**
 * Makes sure that [okHttpClient] is properly destroyed.
 */
public class OkHttpClientVokPlugin : VOKPlugin {
    override fun init() {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient()
        }
    }

    override fun destroy() {
        okHttpClient?.destroy()
        okHttpClient = null
    }

    public companion object {
        /**
         * All REST client calls will reuse this client. Automatically destroyed in [destroy] (triggered by [com.github.vok.framework.VaadinOnKotlin.destroy]).
         */
        public var okHttpClient: OkHttpClient? = null
        /**
         * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
         * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
         */
        public var gson: Gson = GsonBuilder().create()
    }
}

/**
 * Parses the response as a JSON and converts it to a Java object with given [clazz] using [OkHttpClientVokPlugin.gson].
 */
public fun <T> ResponseBody.json(clazz: Class<T>): T = OkHttpClientVokPlugin.gson.fromJson(charStream(), clazz)

/**
 * Parses the response as a JSON array and converts it into a list of Java object with given [clazz] using [OkHttpClientVokPlugin.gson].
 */
public fun <T> ResponseBody.jsonArray(clazz: Class<T>): List<T> = OkHttpClientVokPlugin.gson.fromJsonArray(charStream(), clazz)

/**
 * Parses [json] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Gson.fromJsonArray(json: String, itemClass: Class<T>): List<T> {
    val type: Type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(json, type)
}

/**
 * Parses JSON from a [reader] as a list of items with class [itemClass] and returns that.
 */
public fun <T> Gson.fromJsonArray(reader: Reader, itemClass: Class<T>): List<T> {
    val type: Type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(reader, type)
}

/**
 * Runs given [request] synchronously and then runs [responseBlock] with the response body.
 * Everything including the [Response] and [ResponseBody] is properly closed afterwards.
 *
 * The [responseBlock] is only called on HTTP 200..299 SUCCESS. [checkOk] is used, to check for
 * possible failure reported as HTTP status code, prior calling the block.
 * @param responseBlock runs on success. Takes a [ResponseBody] and produces the object of type [T].
 * You can use [json], [jsonArray] or other utility methods to convert JSON to a Java object.
 * @return whatever has been returned by [responseBlock]
 */
public fun <T> OkHttpClient.exec(request: Request, responseBlock: (ResponseBody) -> T): T =
        newCall(request).execute().use {
            val body: ResponseBody = it.checkOk().body!!
            body.use {
                responseBlock(body)
            }
        }

/**
 * Parses the response as a JSON map and converts it into a map of objects with given [valueClass] using [OkHttpClientVokPlugin.gson].
 */
public fun <V> ResponseBody.jsonMap(valueClass: Class<V>): Map<String, V> = OkHttpClientVokPlugin.gson.fromJsonMap(charStream(), valueClass)

/**
 * Parses [json] as a map of items with class [valueClass] and returns that.
 */
public fun <T> Gson.fromJsonMap(reader: Reader, valueClass: Class<T>): Map<String, T> {
    val type: Type = TypeToken.getParameterized(Map::class.java, String::class.java, valueClass).type
    return fromJson<Map<String, T>>(reader, type)
}

/**
 * Parses this string as a `http://` or `https://` URL. You can configure the URL
 * (e.g. add further query parameters) in [block]. For example:
 * ```
 * val url: HttpUrl = baseUrl.buildUrl {
 *   if (range != 0..Long.MAX_VALUE) {
 *     addQueryParameter("offset", range.first.toString())
 *     addQueryParameter("limit", range.length.toString())
 *   }
 * }
 * ```
 * @throws IllegalArgumentException if the URL is unparseable
 */
public inline fun String.buildUrl(block: HttpUrl.Builder.()->Unit = {}): HttpUrl = toHttpUrl().newBuilder().apply {
    block()
}.build()

/**
 * Builds a new OkHttp [Request] using given URL. You can optionally configure the request in [block]. Use [exec] to
 * execute the request with given OkHttp client and obtain a response. By default the `GET` request gets built.
 */
public inline fun HttpUrl.buildRequest(block: Request.Builder.()->Unit = {}): Request = Request.Builder().url(this).apply {
    block()
}.build()
