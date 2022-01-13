package eu.vaadinonkotlin

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

public val emptyResourceBundle: ResourceBundle = object : ListResourceBundle() {
    override fun getContents(): Array<Array<Any>> = arrayOf()
}

/**
 * Localizes VoK messages such as exception messages, texts found in the filter components etc. To obtain instance of
 * this class, just use the `vt` property
 * which will lookup proper [locale] from the current UI. See the `vt` property documentation for more details.
 *
 * The standard Java [ResourceBundle] mechanism is used.
 *
 * The following resource bundles are searched:
 * * The `VokMessages*.properties` bundle, located in the root package (`src/main/resources/`). Create one if you need to customize the localization
 * strings in your app.
 * * If the message is not found, the standard message bundle of `com.github.vok.VokMessages*.properties` is consulted.
 *
 * Consult the standard message bundle for the list of messages.
 *
 * Currently there is no support for parameters nor expressions in the messages.
 * @property locale the locale to use, not null.
 */
public class I18n internal constructor(public val locale: Locale) {
    private val standardMessages: ResourceBundle = ResourceBundle.getBundle("eu.vaadinonkotlin.VokMessages", locale)
    private val customMessages: ResourceBundle = try {
        ResourceBundle.getBundle("VokMessages", locale)
    } catch (ex: MissingResourceException) {
        // don't log the exception itself, it will clutter the console with uninformative exceptions.
        log.debug("Custom message bundle VokMessages for locale $locale is missing, ignoring")
        emptyResourceBundle
    }

    /**
     * Retrieves the message stored under given [key]. If no such message exists, the function must not fail.
     * Instead, it should provide a key wrapped with indicators that a message is missing, for example
     * `!{key}!`.
     */
    public operator fun get(key: String): String {
        if (customMessages.containsKey(key)) {
            return customMessages.getString(key)
        }
        if (standardMessages.containsKey(key)) {
            return standardMessages.getString(key)
        }
        return "!{$key}!";
    }

    /**
     * Checks whether there is a message under given [key]. If not, [get] will return `!{key}!`.
     */
    public fun contains(key: String): Boolean = customMessages.containsKey(key) || standardMessages.containsKey(key)

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(I18n::class.java)
    }
}

/**
 * Returns a provider which provides instances of [I18n]. On debugging,
 * the provider will always create new instance which allows for hot-redeployment
 * of the i18n bundles. For production, the [I18n] instances are cached
 * so that the key lookup is very quick.
 */
public fun getI18nProvider(production: Boolean): (Locale)-> I18n = when (production) {
    true -> productionI18nProvider
    else -> { locale -> I18n(locale) }
}

private val productionI18nCache = ConcurrentHashMap<Locale, I18n>()
private val productionI18nProvider: (Locale)-> I18n = { locale ->
    productionI18nCache.computeIfAbsent(locale) { l -> I18n(l) }
}
