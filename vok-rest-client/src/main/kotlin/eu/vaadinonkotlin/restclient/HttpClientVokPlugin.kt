package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.vaadinonkotlin.VOKPlugin
import java.net.http.HttpClient

/**
 * Makes sure that [httpClient] is properly destroyed.
 */
public class HttpClientVokPlugin : VOKPlugin {
    override fun init() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build()
        }
    }

    override fun destroy() {
        httpClient = null
    }

    public companion object {
        /**
         * All REST client calls will reuse this client.
         */
        public var httpClient: HttpClient? = null
        /**
         * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
         * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
         */
        public var gson: Gson = GsonBuilder().registerJavaTimeAdapters().create()
    }
}