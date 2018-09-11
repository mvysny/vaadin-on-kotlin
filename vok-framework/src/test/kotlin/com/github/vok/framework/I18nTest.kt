package com.github.vok.framework

import com.github.mvysny.dynatest.DynaTest
import java.util.*
import kotlin.test.expect

class I18nTest : DynaTest({
    test("simple key retrieval") {
        expect("Clear") { I18n(Locale.ENGLISH)["filter.clear"] }
        expect("Clear") { I18n(Locale.SIMPLIFIED_CHINESE)["filter.clear"] }
    }
    test("simple missing key retrieval") {
        expect("!{foo.bar.baz}!") { I18n(Locale.ENGLISH)["foo.bar.baz"] }
    }
    test("custom i18n: check that the testing resource of VokMessages_ja_JP.properties is loaded and used properly") {
        expect("Set") { I18n(Locale.ENGLISH)["filter.set"] }
        expect("Set") { I18n(Locale.JAPANESE)["filter.set"] }
        expect("Test of custom translation string") { I18n(Locale.JAPAN)["filter.set"] }
    }
})
