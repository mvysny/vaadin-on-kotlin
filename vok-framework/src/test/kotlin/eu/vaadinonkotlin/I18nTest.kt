package eu.vaadinonkotlin

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.expect

class I18nTest {
    @Nested inner class DebugMode() : I18nTests(false)
    @Nested inner class ProductionMode() : I18nTests(true)
}

abstract class I18nTests(val production: Boolean) {
    fun i18n(locale: Locale) = getI18nProvider(production)(locale)

    @Test fun `simple key retrieval`() {
        expect("Clear") { i18n(Locale.ENGLISH)["filter.clear"] }
        expect("Clear") { i18n(Locale.SIMPLIFIED_CHINESE)["filter.clear"] }
    }
    @Test fun `simple missing key retrieval`() {
        expect("!{foo.bar.baz}!") { i18n(Locale.ENGLISH)["foo.bar.baz"] }
    }
    @Test fun `custom i18n - check that the testing resource of VokMessages_ja_JP-properties is loaded and used properly`() {
        expect("Set") { i18n(Locale.ENGLISH)["filter.set"] }
        expect("Set") { i18n(Locale.JAPANESE)["filter.set"] }
        expect("Test of custom translation string") { i18n(Locale.JAPAN)["filter.set"] }
    }
}
