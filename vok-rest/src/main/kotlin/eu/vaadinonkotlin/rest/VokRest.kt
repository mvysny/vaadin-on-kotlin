package eu.vaadinonkotlin.rest

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.Gson
import com.google.gson.GsonBuilder

public object VokRest {
    /**
     * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
     * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
     *
     * Remember to call [configureToJavalin] in order for Javalin to use Gson.
     */
    public var gson: Gson = GsonBuilder().registerJavaTimeAdapters().create()
}

private fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder = apply {
    Converters.registerAll(this)
}
