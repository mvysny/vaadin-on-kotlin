package com.github.vok.framework.flow

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinRequest
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
                override fun init(request: VaadinRequest) {
                    locale = Locale.ENGLISH
                    caption = vt["filter.all"]
                }
            }
            MockVaadin.setup(uiFactory = { MyUI() })
            expect("All") { UI.getCurrent().caption }
        }
    }
})
