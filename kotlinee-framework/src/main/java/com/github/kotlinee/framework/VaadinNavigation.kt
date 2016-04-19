package com.github.kotlinee.framework

import com.google.gwt.thirdparty.guava.common.collect.BiMap
import com.google.gwt.thirdparty.guava.common.collect.HashBiMap
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.navigator.ViewProvider
import com.vaadin.ui.UI
import java.net.URLDecoder
import java.net.URLEncoder
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
 * Internal class which enumerates views.
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
        return if (name == VIEW_NAME_USE_DEFAULT) javaClass.simpleName.upperCamelToLowerHyphen() else name
    }

    override fun onStartup(c: MutableSet<Class<*>>?, ctx: ServletContext?) {
        c?.forEach { viewNameToClass.put(it.toViewName(), it.asSubclass(View::class.java)) }
    }
}

/**
 * Set this view provider to the Navigator:
 *
 * `navigator.addProvider(autoViewProvider)`
 *
 * The view provider will auto-discover all of your views and will create names for them, see [ViewName] for more details.
 * To navigate to a view, just call the [navigateTo] helper method which will generate the correct URI fragment and will navigate.
 * You can parse the parameters back later on in your [View.enter], by calling `event.parameterList`.
 */
val autoViewProvider = AutoViewProvider.Companion

private const val VIEW_NAME_USE_DEFAULT = "USE_DEFAULT"

/**
 * Navigates to given view. As a convention, you should introduce a static method `navigateTo(params)` to all of your views,
 * whichi will then simply call this function.
 * @param view the class of the view, not null.
 * @param params an optional list of string params. The View will receive the params via
 * [ViewChangeListener.ViewChangeEvent.getParameters], use [.getParameters] to parse them back in.
 */
fun navigateTo(view: Class<out View>, params: List<String>?) {
    val mapping = AutoViewProvider.getMapping(view)
    val param = params?.map { URLEncoder.encode(it, "UTF-8") }?.joinToString("/", "/") ?: ""
    UI.getCurrent().navigator.navigateTo("$mapping$param")
}

/**
 * Parses the parameters back from the fragment. See [navigateTo] for details.
 * @param event the event received in [ViewChangeListener]
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
