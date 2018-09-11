package com.github.vok.framework

import org.slf4j.LoggerFactory
import java.util.*

val emptyResourceBundle: ResourceBundle = object : ListResourceBundle() {
    override fun getContents(): Array<Array<Any>> = arrayOf()
}

/**
 * Localizes VoK messages such as exception messages, texts found in the filter components etc. To obtain instance of
 * this class, just use `voki18n` which will lookup proper [locale] from the current UI.
 * The standard Java [ResourceBundle] mechanism is used.
 *
 * The following resource bundles are searched:
 * * The `VokMessages*.properties` bundle, located in the root package. Create one if you need to customize the localization
 * strings in your app.
 * * If the message is not found, the standard message bundle of `com.github.vok.VokMessages*.properties` is consulted.
 *
 * Consult the standard message bundle for the list of messages.
 *
 * Currently there is no support for parameters nor expressions in the messages.
 * @property locale the locale to use, not null.
 */
class I18n(val locale: Locale) {
    private val standardMessages: ResourceBundle = ResourceBundle.getBundle("com.github.vok.VokMessages", locale)
    private val customMessages: ResourceBundle = try {
        ResourceBundle.getBundle("VokMessages", locale)
    } catch (ex: MissingResourceException) {
        // @todo mavi we generally expect that the users won't supply their own VokMessages, therefore this
        //  will be called for every I18n retrieval.
        log.debug("Custom message bundle VokMessages for locale $locale is missing, ignoring", ex)
        emptyResourceBundle
    }

    /**
     * Retrieves the message stored under given [key]. If no such message exists, the key itself is returned.
     */
    operator fun get(key: String): String {
        if (customMessages.containsKey(key)) {
            return customMessages.getString(key)
        }
        if (standardMessages.containsKey(key)) {
            return standardMessages.getString(key)
        }
        return key;
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(I18n::class.java)
    }
}
