package com.github.vok.framework

import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinResponse
import com.vaadin.server.VaadinService
import com.vaadin.server.VaadinSession
import com.vaadin.ui.UI
import java.io.Serializable
import javax.servlet.http.Cookie
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Manages session-scoped objects. If the object is not yet present in the session, it is created (using the zero-arg
 * constructor), stored into the session and retrieved.
 *
 * To use this feature, simply define a global property returning desired object as follows:
 * `val Session.loggedInUser: LoggedInUser by lazySession()`
 * Then simply read this property from anywhere, to retrieve the instance. Note that your class needs to be [Serializable] (required when
 * storing stuff into session).
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock (that is, there is current session available).
 */
class SessionScoped<R>(private val clazz: Class<R>): ReadOnlyProperty<Any?, R> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = getOrCreate()

    private fun getOrCreate(): R = checkUIThread().session.getOrCreate()

    private fun VaadinSession.getOrCreate(): R {
        var result: R? = getAttribute(clazz)
        if (result == null) {
            // look up the zero-arg constructor. the constructor should be private if the user follows our recommendations.
            val constructor = clazz.declaredConstructors.first { it.parameterCount == 0 }
            constructor.isAccessible = true
            result = clazz.cast(constructor.newInstance())!!
            setAttribute(clazz, result)
        }
        return result
    }
}

/**
 * Gets a session-retriever for given object. The object is auto-created and bound to the session if missing from the session.
 * The object is bound under the string key of class full name.
 * @param R the object type
 */
inline fun <reified R> lazySession() where R : Any, R : Serializable = SessionScoped(R::class.java)

/**
 * Just a namespace object for attaching your [SessionScoped] objects.
 */
object Session {

    /**
     * Returns the current [VaadinSession]; fails if there is no session, most probably since we are not in the UI thread.
     */
    val current: VaadinSession get() = VaadinSession.getCurrent() ?: throw IllegalStateException("Not in UI thread")

    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun get(key: String): Any? = current.getAttribute(key)

    /**
     * Returns the attribute stored in this session under given key.
     * @param key the key
     * @return the attribute value, may be null
     */
    operator fun <T: Any> get(key: KClass<T>): T? = current.getAttribute(key.java)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun set(key: String, value: Any?) = current.setAttribute(key, value)

    /**
     * Stores given value under given key in a session. Removes the mapping if value is null
     * @param key the key
     * @param value the value to store, may be null if
     */
    operator fun <T: Any> set(key: KClass<T>, value: T?) = current.setAttribute(key.java, value)
}

val currentRequest: VaadinRequest get() = VaadinService.getCurrentRequest() ?: throw IllegalStateException("No current request")
val currentResponse: VaadinResponse get() = VaadinService.getCurrentResponse() ?: throw IllegalStateException("No current response")

/**
 * You can use `Cookies["mycookie"]` to retrieve a cookie named "mycookie" (or null if no such cookie exists.
 * You can also use `Cookies += cookie` to add a pre-created cookie to a session.
 */
object Cookies {
    /**
     * Finds a cookie by name.
     * @param name cookie name
     * @return cookie or null if there is no such cookie.
     */
    operator fun get(name: String): Cookie? = currentRequest.cookies?.firstOrNull { it.name == name }

    /**
     * Overwrites given cookie, or deletes it.
     * @param name cookie name
     * @param cookie the cookie to overwrite. If null, the cookie is deleted.
     */
    operator fun set(name: String, cookie: Cookie?) {
        if (cookie == null) {
            val newCookie = Cookie(name, null)
            newCookie.maxAge = 0  // delete immediately
            newCookie.path = "/"
            currentResponse.addCookie(newCookie)
        } else {
            currentResponse.addCookie(cookie)
        }
    }

    /**
     * Deletes cookie with given [name]. Does nothing if there is no such cookie.
     */
    fun delete(name: String) = set(name, null)
}

/**
 * Allows you to add a cookie: `Cookies += Cookie("autologin", "secret")`
 */
infix operator fun Cookies.plusAssign(cookie: Cookie) = set(cookie.name, cookie)

/**
 * Allows you to remove a cookie: `Cookies -= Cookie("autologin", "secret")`
 */
infix operator fun Cookies.minusAssign(cookie: Cookie) = set(cookie.name, null)

/**
 * Checks that this thread runs with Vaadin UI set.
 * @return the UI instance, not null.
 * @throws IllegalStateException if not run in the UI thread or [UI.init] is ongoing.
 */
fun checkUIThread() = UI.getCurrent() ?: throw IllegalStateException("Not in UI thread, or UI.init() is currently ongoing")
