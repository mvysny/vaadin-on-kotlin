package eu.vaadinonkotlin.restclient

import com.fatboyindustrial.gsonjavatime.Converters
import com.gitlab.mvysny.uribuilder.net.URIBuilder
import com.google.gson.GsonBuilder
import eu.vaadinonkotlin.MediaType
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.util.*

/**
 * Documents a HTTP failure.
 * @property statusCode the HTTP status code, one of [jakarta.servlet.http.HttpServletResponse] `SC_*` constants.
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
public fun <T> HttpResponse<T>.checkOk(): HttpResponse<T> {
    if (!isSuccessful) {
        val response = bodyAsString()
        if (statusCode() == 404) throw FileNotFoundException("${statusCode()}: $response (${request().method()} ${request().uri()})")
        throw HttpResponseException(statusCode(), request().method(), request().uri().toString(), response)
    }
    return this
}

/**
 * True if [HttpResponse.statusCode] is 200..299
 */
public val HttpResponse<*>.isSuccessful: Boolean get() = statusCode() in 200..299

/**
 * Returns the [HttpResponse.body] as [String].
 */
public fun HttpResponse<*>.bodyAsString(): String {
    return when (val body = body()) {
        is String -> body
        is ByteArray -> body.toString(Charsets.UTF_8)
        is InputStream -> body.buffered().reader().readText()
        is Reader -> body.buffered().readText()
        is CharArray -> body.concatToString()
        else -> body.toString()
    }
}

public fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder = apply {
    Converters.registerAll(this)
}

/**
 * Parses the response as a JSON and converts it to a Java object.
 */
public fun <T> HttpResponse<InputStream>.json(clazz: Class<T>): T = HttpClientVokPlugin.gson.fromJson(body().buffered().reader(), clazz)

/**
 * Parses the response as a JSON array and converts it into a list of Java object with given [clazz] using [HttpClientVokPlugin.gson].
 */
public fun <T> HttpResponse<InputStream>.jsonArray(clazz: Class<T>): List<T> = HttpClientVokPlugin.gson.fromJsonArray(body().buffered().reader(), clazz)

/**
 * Runs given [request] synchronously and then runs [responseBlock] with the response body.
 * Everything including the [HttpResponse.body] is properly closed afterward.
 *
 * The [responseBlock] is only called on HTTP 200..299 SUCCESS. [checkOk] is used, to check for
 * possible failure reported as HTTP status code, prior calling the block.
 * @param responseBlock runs on success. Takes a [HttpResponse] and produces the object of type [T].
 * You can use [json], [jsonArray] or other utility methods to convert JSON to a Java object.
 * @return whatever has been returned by [responseBlock]
 */
public fun <T> HttpClient.exec(request: HttpRequest, responseBlock: (HttpResponse<InputStream>) -> T): T {
    val result = send(request, HttpResponse.BodyHandlers.ofInputStream())
    return result.body().use {
        result.checkOk()
        responseBlock(result)
    }
}

/**
 * Parses the response as a JSON map and converts it into a map of objects with given [valueClass] using [HttpClientVokPlugin.gson].
 */
public fun <V> HttpResponse<InputStream>.jsonMap(valueClass: Class<V>): Map<String, V> = HttpClientVokPlugin.gson.fromJsonMap(body().buffered().reader(), valueClass)

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
 * @throws IllegalArgumentException if the URL is unparsable
 */
public inline fun String.buildUrl(block: URIBuilder.()->Unit = {}): URI {
    val uri = URIBuilder(this).apply(block).build()
    require(uri.scheme == "http" || uri.scheme == "https") { "Expected URL scheme 'http' or 'https' but got ${uri.scheme}: $uri" }
    return uri
}

/**
 * Builds a new [HttpRequest] using given URL. You can optionally configure the request in [block]. Use [exec] to
 * execute the request and obtain a response. By default, the `GET` request gets built.
 */
public inline fun URI.buildRequest(block: HttpRequest.Builder.()->Unit = {}): HttpRequest = HttpRequest.newBuilder(this).apply(block).build()

public fun HttpRequest.Builder.post(body: String, mediaType: MediaType) {
    POST(BodyPublishers.ofString(body))
    header("Content-type", mediaType.toString())
}

public fun HttpRequest.Builder.patch(body: String, mediaType: MediaType) {
    method("PATCH", BodyPublishers.ofString(body))
    header("Content-type", mediaType.toString())
}

public fun HttpRequest.Builder.basicAuth(username: String, password: String) {
    val valueToEncode = "$username:$password"
    val h = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.toByteArray())
    header("Authorization", h)
}
