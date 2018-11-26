package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import okhttp3.*
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.FileNotFoundException
import java.io.IOException
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
 * `@GET("users") fun getUserNames(): List<String>`.
 *
 * Usage example:
 * ```
 * val client = createRetrofit("http://localhost:8080/rest/").create(YourClientInterface::class.java)
 * ```
 *
 * Beware: uses [OkHttpClientVokPlugin.okHttpClient] under the hood, which contains a common executor service.
 * If you're not running VoK, don't forget to initialize it in [OkHttpClientVokPlugin.init] and don't forget to call [OkHttpClientVokPlugin.destroy].
 * This is called automatically in VoK apps.
 * @param baseUrl the base URL against which relative paths from the interface are resolved. Must end with a slash.
 * @param gson a configured Gson instance to use, defaults to [OkHttpClientVokPlugin.gson].
 */
fun createRetrofit(baseUrl: String, gson: Gson = OkHttpClientVokPlugin.gson): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory(OkHttpClientVokPlugin.okHttpClient!!)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(UnitConversionFactory)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(SynchronousCallSupportFactory)
        .build()

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
