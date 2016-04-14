package com.example.pokusy

import com.vaadin.server.VaadinSession
import com.vaadin.ui.UI
import java.io.Serializable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Session-bound class which caches the last added person. Do not construct by hand - use [lastAddedPersonCache] to retrieve instead!
 * @author mvy
 */
class LastAddedPersonCache: Serializable {
    init {
        println("LastAddedPersonCache created")
    }
    var lastAdded: Person? = null
}

/**
 * Retrieves the session-scoped instance of this class.
 */
val lastAddedPersonCache: LastAddedPersonCache by SessionScoped.get()

/**
 * Returns given object from the session. If the object is not yet present in the session, it is created (using the zero-arg
 * constructor), stored into the session and retrieved.
 *
 * To use, simply define a global property returning desired object as follows:
 * `val loggedInUser: LoggedInUser by SessionScoped.get()`
 * Then simply read this property from anywhere, to retrieve the instance.
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock!
 */
class SessionScoped<R>(private val clazz: Class<R>): ReadOnlyProperty<Nothing?, R> {
    override fun getValue(thisRef: Nothing?, property: KProperty<*>): R = getOrCreate()

    private fun getOrCreate(): R = UI.getCurrent().session.getOrCreate()

    private fun VaadinSession.getOrCreate(): R {
        var result: R? = getAttribute(clazz)
        if (result == null) {
            result = clazz.newInstance()
            setAttribute(clazz, result)
        }
        return result!!
    }

    companion object {
        inline fun <reified R: Any> get() = SessionScoped(R::class.java)
    }
}
