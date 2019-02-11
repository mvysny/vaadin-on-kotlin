package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.UI
import java.lang.IllegalStateException
import kotlin.test.expect

class SessionTest : DynaTest({
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
            MockVaadin.setup({ MyUI() })
            expect(true) { initCalled }
        }
        test("Succeeds after UI is initialized") {
            UI.setCurrent(null)
            MockVaadin.setup()
            checkUIThread()
        }
    }
})
