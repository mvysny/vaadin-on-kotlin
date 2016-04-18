package com.github.kotlinee.framework

import com.vaadin.server.*
import com.vaadin.ui.Component
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.expect

object MockVaadin {
    // prevent GC on Vaadin Session and Vaadin UI as they are only soft-referenced from the Vaadin itself.
    private val strongRefSession = ThreadLocal<VaadinSession>()
    private val strongRefUI = ThreadLocal<UI>()
    /**
     * Creates new mock session and UI for a test. Just call this in your @Before
     */
    fun setup() {
        val config = DefaultDeploymentConfiguration(MockVaadin::class.java, Properties())
        VaadinSession(VaadinServletService(VaadinServlet(), config)).apply {
            VaadinSession.setCurrent(this)
            strongRefSession.set(this)
        }
        object : UI() {
            override fun init(request: VaadinRequest?) {
            }
        }.apply {
            strongRefUI.set(this)
            UI.setCurrent(this)
        }
    }
}

class VaadinUtilsTest {
    @Before
    fun setupMockVaadin() = MockVaadin.setup()

    @Test
    fun walkTest() {
        val expected = mutableSetOf<Component>()
        val root = VerticalLayout().apply {
            expected.add(this)
            button("Foo") {
                expected.add(this)
            }
            horizontalLayout {
                expected.add(this)
                label {
                    expected.add(this)
                }
            }
            verticalLayout {
                expected.add(this)
            }
        }
        expect(expected) { root.walk().toSet() }
        expect(root) { root.walk().toList()[0] }
    }
}
