package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import eu.vaadinonkotlin.VOKPlugin
import okhttp3.*
import java.io.Reader

/**
 * Destroys the [OkHttpClient] including the dispatcher, connection pool, everything. WARNING: THIS MAY AFFECT
 * OTHER http clients if they share e.g. dispatcher executor service.
 */
fun OkHttpClient.destroy() {
    dispatcher().executorService().shutdown()
    connectionPool().evictAll()
    cache()?.close()
}

/**
 * Fails if the response is not in 200..299 range; otherwise returns [this].
 */
fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val msg = "${code()}: ${body()!!.string()} (${request().url()})"
        if (code() == 404) throw java.io.FileNotFoundException(msg)
        throw java.io.IOException(msg)
    }
    return this
}

/**
 * Makes sure that [okHttpClient] is properly destroyed.
 */
class OkHttpClientVokPlugin : VOKPlugin {
    override fun init() {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient()
        }
    }

    override fun destroy() {
        okHttpClient?.destroy()
        okHttpClient = null
    }

    companion object {
        /**
         * All REST client calls will reuse this client. Automatically destroyed in [destroy] (triggered by [com.github.vok.framework.VaadinOnKotlin.destroy]).
         */
        var okHttpClient: OkHttpClient? = null
        /**
         * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
         * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
         */
        var gson: Gson = GsonBuilder().create()
    }
}

/**
 * Parses the response as a JSON and converts it to a Java object with given [clazz] using [OkHttpClientVokPlugin.gson].
 */
fun <T> ResponseBody.json(clazz: Class<T>): T = OkHttpClientVokPlugin.gson.fromJson(charStream(), clazz)

/**
 * Parses the response as a JSON array and converts it into a list of Java object with given [clazz] using [OkHttpClientVokPlugin.gson].
 */
fun <T> ResponseBody.jsonArray(clazz: Class<T>): List<T> = OkHttpClientVokPlugin.gson.fromJsonArray(charStream(), clazz)

/**
 * Parses [json] as a list of items with class [itemClass] and returns that.
 */
fun <T> Gson.fromJsonArray(json: String, itemClass: Class<T>): List<T> {
    val type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(json, type)
}

/**
 * Parses JSON from a [reader] as a list of items with class [itemClass] and returns that.
 */
fun <T> Gson.fromJsonArray(reader: Reader, itemClass: Class<T>): List<T> {
    val type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(reader, type)
}

/**
 * Runs given [request] synchronously and then runs [responseBlock] with the response body. The [Response] is properly closed afterwards.
 * Only calls the block on success; uses [checkOk] to check for failure prior calling the block.
 * @param responseBlock run on success.
 */
fun <T> OkHttpClient.exec(request: Request, responseBlock: (ResponseBody) -> T): T =
        newCall(request).execute().use {
            responseBlock(it.checkOk().body()!!)
        }

/**
 * Parses the response as a JSON map and converts it into a map of objects with given [valueClass] using [OkHttpClientVokPlugin.gson].
 */
fun <V> ResponseBody.jsonMap(valueClass: Class<V>): Map<String, V> = OkHttpClientVokPlugin.gson.fromJsonMap(charStream(), valueClass)

/**
 * Parses [json] as a map of items with class [valueClass] and returns that.
 */
fun <T> Gson.fromJsonMap(reader: Reader, valueClass: Class<T>): Map<String, T> {
    val type = TypeToken.getParameterized(Map::class.java, String::class.java, valueClass).type
    return fromJson<Map<String, T>>(reader, type)
}

/**
 * Parses this string as a `http://` or `https://` URL. You can use the builder to add further query parameters.
 * @throws IllegalArgumentException if the URL is unparsable
 */
fun String.buildUrl(block: HttpUrl.Builder.()->Unit): HttpUrl = HttpUrl.get(this).newBuilder().apply {
    block()
}.build()
