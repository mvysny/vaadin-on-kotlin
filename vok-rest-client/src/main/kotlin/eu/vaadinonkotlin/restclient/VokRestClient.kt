package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.net.http.HttpClient

public object VokRestClient {
    /**
     * All REST client calls will reuse this client. Set a different value in your
     * `ServletContextListener.contextInitialized()` to reconfigure.
     *
     */
    public var httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL).build()

    /**
     * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
     * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
     */
    public var gson: Gson = GsonBuilder().registerJavaTimeAdapters().create()
}
