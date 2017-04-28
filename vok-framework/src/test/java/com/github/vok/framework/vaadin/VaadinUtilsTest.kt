package com.github.vok.framework.vaadin

import com.vaadin.server.*
import com.vaadin.ui.*
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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
        val session = VaadinSession(VaadinServletService(VaadinServlet(), config))
        VaadinSession.setCurrent(session)
        strongRefSession.set(session)
        val ui = object : UI() {
            override fun init(request: VaadinRequest?) {
            }
        }
        strongRefUI.set(ui)
        UI.setCurrent(ui)
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

    @Test
    fun buttonListenerSerializable() {
        { println("foo") }.serializeDeserialize()()
        Button().apply {
            onLeftClick { println("bla") }
        }.serializeToBytes()
    }

    @Test
    fun imageListenerSerializable() {
        Image().apply {
            onLeftClick { println("bla") }
        }.serializeToBytes()
    }

    private fun <T: HasComponents> T.dummyContent(size: Int): T {
        (0..size - 1).forEach { label("$it") }
        return this
    }

    @Test
    fun testComponentContainerIndices() {
        Assume.assumeFalse(AbsoluteLayout() is AbstractOrderedLayout)
        Assume.assumeTrue(VerticalLayout() is AbstractOrderedLayout)
        fun testComponentContainerIndices(container: ()-> ComponentContainer) {
            expect(0..-1) { container().indices }
            expect(0..99) { container().dummyContent(100).indices }
            expect(0..9) { container().dummyContent(10).indices }
            expect(0..19) { container().dummyContent(20).indices }
        }

        testComponentContainerIndices { CssLayout() }
        testComponentContainerIndices { AbsoluteLayout() }
        testComponentContainerIndices { VerticalLayout() }
    }


    @Test
    fun testComponentContainerGetComponentAt() {
        fun testComponentContainerGetComponentAt(container: ()-> ComponentContainer) {
            expect("25") { (container().dummyContent(100).getComponentAt(25) as Label).value }
            expect("0") { (container().dummyContent(100).getComponentAt(0) as Label).value }
            expect("99") { (container().dummyContent(100).getComponentAt(99) as Label).value }
        }

        testComponentContainerGetComponentAt { CssLayout() }
        testComponentContainerGetComponentAt { AbsoluteLayout() }
        testComponentContainerGetComponentAt { VerticalLayout() }
    }

    @Test
    fun testComponentContainerRemoveComponentAt() {
        fun ComponentContainer.join() = filterIsInstance<Label>().joinToString("", transform = { it.value })
        fun testComponentContainerRemoveComponentAt(containre: ()-> ComponentContainer) {
            expect("1234") {
                containre().dummyContent(5).apply {
                    removeComponentAt(0)
                }.join()
            }
            expect("0134") {
                containre().dummyContent(5).apply {
                    removeComponentAt(2)
                }.join()
            }
            expect("0123") {
                containre().dummyContent(5).apply {
                    removeComponentAt(4)
                }.join()
            }
        }

        testComponentContainerRemoveComponentAt { CssLayout() }
        testComponentContainerRemoveComponentAt { AbsoluteLayout() }
        testComponentContainerRemoveComponentAt { VerticalLayout() }
    }

    @Test
    fun testComponentContainerRemoveComponentsAt() {
        fun ComponentContainer.join() = filterIsInstance<Label>().joinToString("", transform = { it.value })
        fun testComponentContainerRemoveComponentsAt(container: ()-> ComponentContainer) {
            expect("01234") {
                container().dummyContent(5).apply {
                    removeComponentsAt(0..-1)
                }.join()
            }
            expect("1234") {
                container().dummyContent(5).apply {
                    removeComponentsAt(0..0)
                }.join()
            }
            expect("234") {
                container().dummyContent(5).apply {
                    removeComponentsAt(0..1)
                }.join()
            }
            expect("") {
                container().dummyContent(5).apply {
                    removeComponentsAt(0..4)
                }.join()
            }
            expect("04") {
                container().dummyContent(5).apply {
                    removeComponentsAt(1..3)
                }.join()
            }
            expect("012") {
                container().dummyContent(5).apply {
                    removeComponentsAt(3..4)
                }.join()
            }
        }

        testComponentContainerRemoveComponentsAt { CssLayout() }
        testComponentContainerRemoveComponentsAt { AbsoluteLayout() }
        testComponentContainerRemoveComponentsAt { VerticalLayout() }
    }
}

fun Any.serializeToBytes(): ByteArray = ByteArrayOutputStream().use { it -> ObjectOutputStream(it).writeObject(this); it }.toByteArray()
inline fun <reified T: Any> ByteArray.deserialize(): T = ObjectInputStream(inputStream()).readObject() as T
inline fun <reified T: Any> T.serializeDeserialize() = serializeToBytes().deserialize<T>()

/**
 * Expects that [actual] list of objects matches [expected] list of objects. Fails otherwise.
 * @param expected expected list of objects
 * @param actual actual list of objects
 */
fun <T> expectList(vararg expected: T, actual: ()->List<T>) = expect(expected.toList(), actual)
