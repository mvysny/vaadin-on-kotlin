package com.github.vok.framework

import com.github.karibu.testing.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.UI
import java.util.*
import kotlin.test.expect

class VaadinUtilsTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    group("vt") {
        test("smoke test") {
            expect("!{my_sample_property}!") { vt["my_sample_property"] }
        }
        test("vt works in UI.init()") {
            class MyUI : UI() {
                override fun init(request: VaadinRequest?) {
                    locale = Locale.ENGLISH
                    description = vt["filter.all"]
                }
            }
            MockVaadin.setup { MyUI() }
            expect("All") { UI.getCurrent().description }
        }
    }
})
