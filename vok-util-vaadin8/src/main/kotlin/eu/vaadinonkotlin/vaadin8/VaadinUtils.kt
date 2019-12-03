package eu.vaadinonkotlin.vaadin8

import com.vaadin.data.BeanPropertySet
import com.vaadin.data.PropertyDefinition
import eu.vaadinonkotlin.I18n
import eu.vaadinonkotlin.getI18nProvider
import com.vaadin.ui.UI
import java.util.*
import kotlin.reflect.KProperty1

/**
 * Shortcut for VOK Translate. Retrieves the [I18n] for the current UI and the current locale.
 * Fails if there is no UI (the function is not called from Vaadin's UI thread).
 *
 * To configure the current user locale, just call [UI.setLocale] in your [UI.init] method:
 *
 * * As a first step, you can check whether the user has configured his language in his settings. Just get the current
 * user from the session, then do a database lookup for the user settings and retrieve the locale. If it's not null, use it.
 * * If user haven't configured his locale or your app doesn't support this kind of functionality, fall back and simply poll the browser
 * for the [com.vaadin.server.WebBrowser.locale]: `Page.getCurrent().webBrowser.locale`
 * * If the browser provided `null` locale, fall back to [java.util.Locale.ENGLISH].
 * * Set the value computed by the steps above to the UI: [UI.setLocale]
 *
 * Example usage:
 * ```
 * label(vt["createUser.caption"])
 * ```
 */
val vt: I18n
    get() {
    val ui = checkUIThread()
    val locale = ui.locale
    check(locale != null) { "UI.getCurrent().locale can't really return null" }
    val provider: (Locale) -> I18n = getI18nProvider(ui.session.configuration.isProductionMode)
    return provider(locale)
}

/**
 * Checks that this thread runs with Vaadin UI set.
 * @return the UI instance, not null.
 * @throws IllegalStateException if not run in the UI thread or [UI.init] is ongoing.
 */
fun checkUIThread() = UI.getCurrent() ?: throw IllegalStateException("Not called in the UI thread")

/**
 * Returns the Vaadin's [PropertyDefinition] for Kotlin [KProperty1].
 */
@Suppress("UNCHECKED_CAST")
inline val <reified T: Any, V> KProperty1<T, V>.definition: PropertyDefinition<T, V?>
    get() =
        BeanPropertySet.get(T::class.java).getProperty(name).get() as PropertyDefinition<T, V?>
