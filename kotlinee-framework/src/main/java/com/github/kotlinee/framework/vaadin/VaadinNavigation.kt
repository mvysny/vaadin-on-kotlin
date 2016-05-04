package com.github.kotlinee.framework.vaadin

import com.github.kotlinee.framework.Session
import com.google.gwt.thirdparty.guava.common.collect.BiMap
import com.google.gwt.thirdparty.guava.common.collect.HashBiMap
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.navigator.ViewProvider
import com.vaadin.ui.UI
import java.io.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import javax.servlet.ServletContainerInitializer
import javax.servlet.ServletContext
import javax.servlet.annotation.HandlesTypes

private fun String.upperCamelToLowerHyphen(): String {
    val sb = StringBuilder()
    for (i in 0..this.length - 1) {
        var c = this[i]
        if (Character.isUpperCase(c)) {
            c = Character.toLowerCase(c)
            if (shouldPrependHyphen(i)) {
                sb.append('-')
            }
        }
        sb.append(c)
    }
    return sb.toString()
}

private fun String.shouldPrependHyphen(i: Int): Boolean {
    if (i == 0) {
        // Never put a hyphen at the beginning
        return false
    } else if (!Character.isUpperCase(this[i - 1])) {
        // Append if previous char wasn't upper case
        return true
    } else if (i + 1 < this.length && !Character.isUpperCase(this[i + 1])) {
        // Append if next char isn't upper case
        return true
    } else {
        return false
    }
}

/**
 * Internal class which enumerates views. Do not use directly - instead, just add [autoViewProvider] to your [com.vaadin.navigator.Navigator]
 */
@HandlesTypes(View::class)
class AutoViewProvider : ServletContainerInitializer {
    companion object : ViewProvider {
        override fun getViewName(viewAndParameters: String): String? {
            val viewName = parseViewName(viewAndParameters)
            return if (viewNameToClass.containsKey(viewName)) viewName else null
        }

        private fun parseViewName(viewAndParameters: String) : String {
            val viewName = viewAndParameters.removePrefix("!")
            val firstSlash = viewName.indexOf('/')
            return viewName.substring(0..(if(firstSlash < 0) viewName.length - 1 else firstSlash))
        }

        override fun getView(viewName: String): View? = viewNameToClass.get(viewName)?.newInstance()

        /**
         * Maps view name to the view class.
         */
        private val viewNameToClass: BiMap<String, Class<out View>> = HashBiMap.create()

        internal fun <T: View> getMapping(clazz: Class<T>): String =
            viewNameToClass.inverse()[clazz] ?: throw IllegalArgumentException("$clazz is not known view class")
    }

    private fun Class<*>.toViewName(): String {
        val name = getAnnotation(ViewName::class.java)?.value ?: VIEW_NAME_USE_DEFAULT
        return if (name == VIEW_NAME_USE_DEFAULT) simpleName.removeSuffix("View").upperCamelToLowerHyphen() else name
    }

    override fun onStartup(c: MutableSet<Class<*>>?, ctx: ServletContext?) {
        c?.forEach { viewNameToClass.put(it.toViewName(), it.asSubclass(View::class.java)) }
    }
}

/**
 * Set this view provider to the [com.vaadin.navigator.Navigator]:
 *
 * `navigator.addProvider(autoViewProvider)`
 *
 * The view provider will auto-discover all of your views and will create names for them, see [ViewName] for more details.
 * To navigate to a view, just call the [navigateTo] helper method which will generate the correct URI fragment and will navigate.
 * You can parse the parameters back later on in your [View.enter], by calling `event.parameterList`.
 */
val autoViewProvider = AutoViewProvider

private const val VIEW_NAME_USE_DEFAULT = "USE_DEFAULT"

fun navigateToView(view: Class<out View>, params: List<String>? = null) {
    val mapping = AutoViewProvider.getMapping(view)
    val param = params?.map { URLEncoder.encode(it, "UTF-8") }?.joinToString("/", "/") ?: ""
    UI.getCurrent().navigator.navigateTo("$mapping$param")
}

/**
 * Asks the current UI navigator to navigate to given view.
 *
 * As a convention, you should introduce a static method `navigateTo(params)` to all of your views,
 * which will then simply call this function.
 * @param V the class of the view, not null.
 * @param params an optional list of string params. The View will receive the params via
 * [ViewChangeListener.ViewChangeEvent.getParameters], use [parameterList] to parse them back in.
 */
inline fun <reified V : View> navigateToView(params: List<String>? = null) = navigateToView(V::class.java, params)

/**
 * Parses the parameters back from the URI fragment. See [navigateTo] for details. Call in [ViewChangeListener.ViewChangeEvent] provided to you in the
 * [View.enter] method.
 *
 * Note that the parameters are not named - instead, this is a simple list of values.
 *
 * To obtain a particular parameter or null if the URL has no such parameter, just call [List.getOrNull] on this list.
 * @return list of parameters, empty if there are no parameters.
 */
val ViewChangeListener.ViewChangeEvent.parameterList: List<String>
    get() = parameters.trim().split('/').map { URLDecoder.decode(it, "UTF-8") }

/**
 * By default the view will be assigned a colon-separated name, derived from your view class name. The trailing View is dropped.
 * For example, UserListView will be mapped to user-list. You can attach this annotation to a view, to modify this behavior.
 * It is often a good practice to mark one particular view as the root view, by annotating the class with `ViewName("")`.
 * This view will be shown initially when the user enters your application.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class ViewName(val value: String = VIEW_NAME_USE_DEFAULT)

/**
 * I cannot transfer large objects as parameters via Navigator fragment URLs. Not even serialized/base64-encoded: 2kb URLs are simply gross ;)
 *
 * So, instead, I will temporarily remember such objects in session and will assign them short IDs. Kinda like URL shorteners, but with
 * a catch: bookmarkable URLs are valid only until the server is restarted. Also, at most 30 items are supported per key, to avoid session over-population.
 *
 * @param T storing values of this type.
 * @property sessionKey the list is stored under this Vaadin session key.
 * @author mvy
 */
class UrlParamShortener<T : Serializable> private constructor(private val sessionKey: String) : Serializable {
    /**
     * Soft reference may be GC-ed randomly. A round-robin with 30 items should suffice.
     */
    private var oldestItemIndex = 0
    /**
     * Mutable list of remembered items.
     */
    private val items = ArrayList<T>()

    /**
     * Returns the value stored under given ID. The ID must have been generated via the [put] item.
     * @param pathParam the path param parsed as ID
     * @return the item, may be null if there is no such item stored under given key or it has been forgotten (the shortener stores at most
     * 30 items).
     */
    operator fun get(pathParam: String): T? {
        val entryOrd = Integer.parseInt(pathParam)
        return if (items.size <= entryOrd) null else items[entryOrd]
    }

    private fun saveToSession() {
        Session[sessionKey] = this
    }

    /**
     * Registers given item, generates its ID and returns it.
     * @param item the item to shorten
     * @return the item ID, an Int which is automatically converted to String
     */
    fun put(item: T): String {
        var index = items.indexOf(item)
        if (index < 0) {
            if (items.size < MAX_LEN) {
                index = items.size
                items.add(item)
            } else {
                index = oldestItemIndex++ // vymazeme najstarsi item
                if (oldestItemIndex >= items.size) {
                    oldestItemIndex = 0
                }
                items[index] = item
            }
        }
        saveToSession()
        return "" + index
    }

    companion object {
        private const val MAX_LEN = 30

        /**
         * Returns the URL shortener for given class. Mostly, a view class uses one shortener to shorten its parameter,
         * just pass the view's class here.
         * @param T the type of item this shortener will store.
         * @param O the owner class. Shorthand for `urlShortener(owner.javaClass.name)`
         * @return the shortener
         */
        inline fun <T : Serializable, reified O: Any> Session.urlShortener(): UrlParamShortener<T> = urlShortener(O::class.java.name)

        /**
         * Returns the URL shortener stored in a session under given key.
         * @param key the key, prefixed with SHORTENER_ to avoid session key conflicts.
         * @return the shortener
         */
        fun <T : Serializable> Session.urlShortener(key: String): UrlParamShortener<T> {
            return fromSession(key)
        }

        private fun <T : Serializable> fromSession(key: String) =
                Session["SHORTENER_$key"] as UrlParamShortener<T>? ?: UrlParamShortener<T>(key)
    }
}
