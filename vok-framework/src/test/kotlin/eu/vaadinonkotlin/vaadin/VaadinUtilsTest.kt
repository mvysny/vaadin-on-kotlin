package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.kaributools.label
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.util.*
import kotlin.test.expect

class VaadinUtilsTest {
    @BeforeEach fun fakeVaadin() { MockVaadin.setup() }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }

    @Nested inner class vttests {
        @Test fun `smoke test`() {
            expect("!{my_sample_property}!") { vt["my_sample_property"] }
        }
        @Test fun `vt works in UI-init()`() {
            class MyUI : UI() {
                override fun init(request: VaadinRequest) {
                    locale = Locale.ENGLISH
                    label = vt["filter.all"]
                }
            }
            MockVaadin.setup(uiFactory = { MyUI() })
            expect("All") { UI.getCurrent().label }
        }
    }

    @Nested inner class CheckUIThread {
        @Test fun `Fails if there is no UI`() {
            MockVaadin.tearDown()
            assertThrows<IllegalStateException> {
                checkUIThread()
            }
        }
        @Test fun `Succeeds when called from the UI-init`() {
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
        @Test fun `Succeeds after UI is initialized`() {
            UI.setCurrent(null)
            MockVaadin.setup()
            checkUIThread()
        }
    }
}
