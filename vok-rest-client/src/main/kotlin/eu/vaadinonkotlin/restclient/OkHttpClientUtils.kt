package eu.vaadinonkotlin.restclient

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.vaadinonkotlin.VOKPlugin
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.apache.hc.core5.net.URIBuilder
import java.io.FileNotFoundException
import java.io.IOException

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
 * Documents a HTTP failure.
 * @property statusCode the HTTP status code, one of [javax.servlet.http.HttpServletResponse] `SC_*` constants.
 * @property method the request method, e.g. `"GET"`
 * @property requestUrl the URL requested from the server
 * @property response the response body received from the server, may provide further information to the nature of the failure.
 * May be blank.
 */
public class HttpResponseException(
    public val statusCode: Int,
    public val method: String,
    public val requestUrl: String,
    public val response: String,
    cause: Throwable? = null
) : IOException("$statusCode: $response", cause) {
    override fun toString(): String = "${javaClass.simpleName}: $message ($method $requestUrl)"
}

/**
 * Fails if the response is not in 200..299 range; otherwise returns [this].
 * @throws FileNotFoundException if the HTTP response was 404
 * @throws HttpResponseException if the response is not in 200..299 ([Response.isSuccessful] returns false)
 * @throws IOException on I/O error.
 */
public fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val response = body!!.string()
        if (code == 404) throw FileNotFoundException("$code: $response (${request.method} ${request.url})")
        throw HttpResponseException(code, request.method, request.url.toString(), response)
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
        public var gson: Gson = GsonBuilder().registerJavaTimeAdapters().create()
    }
}

private fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder = apply {
    Converters.registerAll(this)
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
 * Parses this string as a `http://` or `https://` URL. You can configure the URL
 * (e.g. add further query parameters) in [block]. For example:
 * ```
 * val url: HttpUrl = baseUrl.buildUrl {
 *   if (range != 0..Long.MAX_VALUE) {
 *     addParameter("offset", range.first.toString())
 *     addParameter("limit", range.length.toString())
 *   }
 * }
 * ```
 * @throws IllegalArgumentException if the URL is unparseable
 */
public inline fun String.buildUrl(block: URIBuilder.()->Unit = {}): HttpUrl {
    val uri = URIBuilder(this).apply(block).build()
    // temporary, until we can get rid of OkHttp
    return uri.toString().toHttpUrl()
}

/**
 * Builds a new OkHttp [Request] using given URL. You can optionally configure the request in [block]. Use [exec] to
 * execute the request with given OkHttp client and obtain a response. By default, the `GET` request gets built.
 */
public inline fun HttpUrl.buildRequest(block: Request.Builder.()->Unit = {}): Request = Request.Builder().url(this).apply(block).build()
