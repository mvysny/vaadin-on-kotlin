package com.github.vok.restclient

import com.github.vok.framework.VOKPlugin
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
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

/**
 * This function configures [Retrofit] for synchronous clients, so that you can have interface with methods such as
 * `@GET("users") fun getUsers(): List<String>`.
 *
 * Usage example:
 * ```
 * val client = createRetrofit("http://localhost:8080/rest").create(YourClientInterface::class.java)
 * ```
 *
 * Beware: uses [RetrofitClientVokPlugin.okHttpClient] under the hood, which contains a common executor service.
 * If you're not running VoK, don't forget to initialize it in [RetrofitClientVokPlugin.init] and don't forget to call [RetrofitClientVokPlugin.destroy].
 * This is called automatically in VoK apps.
 */
fun createRetrofit(baseUrl: String, gson: Gson = GsonBuilder().create()): Retrofit = Retrofit.Builder()
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
        var okHttpClient: OkHttpClient? = null
    }
}

/**
 * Converts the response to [Unit] (throws it away). This adds support for functions returning `Unit?`.
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
fun <T: Any> Gson.fromJsonArray(json: String, itemClass: Class<T>): List<T> {
    val type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(json, type)

}

/**
 * Parses JSON from [reader] as a list of items with class [itemClass] and returns that.
 */
fun <T: Any> Gson.fromJsonArray(reader: Reader, itemClass: Class<T>): List<T> {
    val type = TypeToken.getParameterized(List::class.java, itemClass).type
    return fromJson<List<T>>(reader, type)
}
