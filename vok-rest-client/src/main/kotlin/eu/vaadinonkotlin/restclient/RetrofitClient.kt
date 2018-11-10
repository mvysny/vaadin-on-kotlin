package eu.vaadinonkotlin.restclient

import com.github.vokorm.Filter
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.SortClause
import eu.vaadinonkotlin.VOKPlugin
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type

/**
 * Necessary so that Retrofit supports synchronous calls and checks that Calls has been successfully executed.
 * When you register this factory, Retrofit will support calls like `@GET("users") fun getUsers(): List<String>`.
 */
object SynchronousCallSupportFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *> {
        return object : CallAdapter<Any?, Any?> {
            override fun responseType(): Type = returnType

            override fun adapt(call: Call<Any?>): Any? {
                val result = call.execute()
                if (!result.isSuccessful) {
                    val msg = "${result.code()}: ${result.errorBody()?.string()} (${call.request().url()})"
                    if (result.code() == 404) throw FileNotFoundException(msg)
                    throw IOException(msg)
                }
                return result.body()
            }
        }
    }
}

fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val msg = "${code()}: ${body()!!.string()} (${request().url()})"
        if (code() == 404) throw java.io.FileNotFoundException(msg)
        throw java.io.IOException(msg)
    }
    return this
}

/**
 * This function configures [Retrofit] for synchronous clients, so that you can have interface with methods such as
 * `@GET("users") fun getUserNames(): List<String>`.
 *
 * Usage example:
 * ```
 * val client = createRetrofit("http://localhost:8080/rest/").create(YourClientInterface::class.java)
 * ```
 *
 * Beware: uses [RetrofitClientVokPlugin.okHttpClient] under the hood, which contains a common executor service.
 * If you're not running VoK, don't forget to initialize it in [RetrofitClientVokPlugin.init] and don't forget to call [RetrofitClientVokPlugin.destroy].
 * This is called automatically in VoK apps.
 * @param baseUrl the base URL against which relative paths from the interface are resolved. Must end with a slash.
 * @param gson a configured Gson instance to use, defaults to [RetrofitClientVokPlugin.gson].
 */
fun createRetrofit(baseUrl: String, gson: Gson = RetrofitClientVokPlugin.gson): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory(RetrofitClientVokPlugin.okHttpClient!!)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(UnitConversionFactory)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(SynchronousCallSupportFactory)
        .build()

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
 * Makes sure that [okHttpClient] is properly destroyed.
 */
class RetrofitClientVokPlugin : VOKPlugin {
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
 * Converts the response to [Unit] (throws it away). This adds support for functions returning `Unit?`. Unfortunately
 * it is not possible to support functions returning just `Unit` - Retrofit will throw an exception for those.
 */
object UnitConversionFactory : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        if (type == Unit::class.java) {
            return Converter<ResponseBody, Unit> { }
        }
        return null
    }
}

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
 */
fun <T> OkHttpClient.exec(request: Request, responseBlock: (ResponseBody) -> T): T =
        newCall(request).execute().use {
            responseBlock(it.checkOk().body()!!)
        }

/**
 * Parses the response as a JSON and converts it to a Java object with given [clazz] using [RetrofitClientVokPlugin.gson].
 */
fun <T> ResponseBody.json(clazz: Class<T>): T = RetrofitClientVokPlugin.gson.fromJson(charStream(), clazz)

/**
 * Parses the response as a JSON array and converts it into a list of Java object with given [clazz] using [RetrofitClientVokPlugin.gson].
 */
fun <T> ResponseBody.jsonArray(clazz: Class<T>): List<T> = RetrofitClientVokPlugin.gson.fromJsonArray(charStream(), clazz)

val LongRange.length: Long get() = if (isEmpty()) 0 else endInclusive - start + 1

/**
 * Uses the CRUD endpoint and serves instances of given item of type [itemClass] over given [client] using [RetrofitClientVokPlugin.gson].
 * Expect the CRUD endpoint to be exposed in the following manner:
 * * `GET /rest/users` returns all users
 * * `GET /rest/users?select=count` returns a single number - the count of all users. This is only necessary for [getCount]
 * or if you plan to use this client as a backend for Vaadin Grid.
 * * `GET /rest/users/22` returns one users
 * * `POST /rest/users` will create an user
 * * `PATCH /rest/users/22` will update an user
 * * `DELETE /rest/users/22` will delete an user
 *
 * Since this client is also a [DataLoader], you can use the [DataLoaderAdapter] class from the `vok-framework-sql2o`/`vok-framework-v10-sql2o`
 * module to turn it into a `DataProvider` which you can feed into Vaadin Grid.
 * @param baseUrl the base URL, such as `http://localhost:8080/rest/users/`, must end with a slash.
 */
class CrudClient<T: Any>(val baseUrl: String, val itemClass: Class<T>,
                    val client: OkHttpClient = RetrofitClientVokPlugin.okHttpClient!!) : DataLoader<T> {
    init {
        require(baseUrl.endsWith("/")) { "$baseUrl must end with /" }
    }

    /**
     * Fetches data from the back end. The items must match given [filter]
     * @param filter optional filter which defines filtering to be used for counting the
     * number of items. If null all items are considered.
     * @param sortBy optionally sort the beans according to given fields. By default sorts ASC; if you prepend the field with the "-"
     * character the sorting will be DESC.
     * @param range offset and limit to fetch
     * @return a list of items matching the query, may be empty.
     */
    fun getAll(sortBy: List<SortClause> = listOf(), range: LongRange = 0..Long.MAX_VALUE): List<T> {
        val url = buildUrl(baseUrl) {
            if (range != 0..Long.MAX_VALUE) {
                addQueryParameter("offset", range.first.toString())
                addQueryParameter("limit", range.length.toString())
            }
            if (!sortBy.isEmpty()) {
                addQueryParameter("sort_by", sortBy.joinToString(",") { "${if(it.asc)"+" else "-"}${it.columnName}" })
            }
        }
        val request = Request.Builder().url(url).build()
        return client.exec(request) { response -> response.jsonArray(itemClass) }
    }

    fun getCount(): Long {
        val request = Request.Builder().url("$baseUrl?select=count").build()
        return client.exec(request) { response -> response.string().toLong() }
    }

    fun getOne(id: String): T {
        val request = Request.Builder().url("$baseUrl$id").build()
        return client.exec(request) { response -> response.json(itemClass) }
    }

    fun create(entity: T) {
        val body = RequestBody.create(mediaTypeJson, RetrofitClientVokPlugin.gson.toJson(entity))
        val request = Request.Builder().post(body).url(baseUrl).build()
        client.exec(request) {}
    }

    fun update(id: String, entity: T) {
        val body = RequestBody.create(mediaTypeJson, RetrofitClientVokPlugin.gson.toJson(entity))
        val request = Request.Builder().patch(body).url("$baseUrl$id").build()
        client.exec(request) {}
    }

    fun delete(id: String) {
        val request = Request.Builder().delete().url("$baseUrl$id").build()
        client.exec(request) {}
    }

    private fun buildUrl(baseUrl: String, block: HttpUrl.Builder.()->Unit): HttpUrl {
        val url = requireNotNull(HttpUrl.parse(baseUrl)) { "Unparsable url: $baseUrl" }
        return url.newBuilder().apply { block() } .build()!!
    }

    companion object {
        val mediaTypeJson = MediaType.parse("application/json; charset=utf-8")
    }

    override fun fetch(filter: Filter<T>?, sortBy: List<SortClause>, range: IntRange): List<T> = getAll(sortBy, range.first.toLong()..range.endInclusive.toLong())

    override fun getCount(filter: Filter<T>?): Int = getCount().toInt()
}
