package com.github.vok.framework

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import java.util.*
import kotlin.test.expect

class I18nTest : DynaTest({
    group("debug mode") {
        i18nTestBattery(false)
    }
    group("production mode") {
        i18nTestBattery(true)
    }
})

fun DynaNodeGroup.i18nTestBattery(production: Boolean) {
    fun i18n(locale: Locale) = getI18nProvider(production)(locale)

    test("simple key retrieval") {
        expect("Clear") { i18n(Locale.ENGLISH)["filter.clear"] }
        expect("Clear") { i18n(Locale.SIMPLIFIED_CHINESE)["filter.clear"] }
    }
    test("simple missing key retrieval") {
        expect("!{foo.bar.baz}!") { i18n(Locale.ENGLISH)["foo.bar.baz"] }
    }
    test("custom i18n: check that the testing resource of VokMessages_ja_JP.properties is loaded and used properly") {
        expect("Set") { i18n(Locale.ENGLISH)["filter.set"] }
        expect("Set") { i18n(Locale.JAPANESE)["filter.set"] }
        expect("Test of custom translation string") { i18n(Locale.JAPAN)["filter.set"] }
    }
}
