package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.github.mvysny.kaributools.caption
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinRequest
import java.lang.IllegalStateException
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

    group("checkUIThread") {
        afterEach { MockVaadin.tearDown() }
        test("Fails if there is no UI") {
            MockVaadin.tearDown()
            expectThrows(IllegalStateException::class) {
                checkUIThread()
            }
        }
        test("Succeeds when called from the UI.init") {
            var initCalled = false
            class MyUI : UI() {
                override fun init(request: VaadinRequest?) {
                    checkUIThread()
                    initCalled = true
                }
            }
            MockVaadin.setup(uiFactory = { MyUI() })
            expect(true) { initCalled }
        }
        test("Succeeds after UI is initialized") {
            UI.setCurrent(null)
            MockVaadin.setup()
            checkUIThread()
        }
    }
})
