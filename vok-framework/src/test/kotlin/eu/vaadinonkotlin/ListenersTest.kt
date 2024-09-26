package eu.vaadinonkotlin

import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.test.expect
import kotlin.test.fail

interface MyListener : Serializable {
    fun onFoo(param1: String)
    fun onBar(param2: Int)
}

class ListenersTest {
    @Test fun `empty listeners do nothing`() {
        val listeners = listeners<MyListener>()
        listeners.fire.onFoo("foo")
        listeners.fire.onBar(25)
    }

    @Test fun `listeners get invoked`() {
        val listeners = listeners<MyListener>()
        var called = 0
        listeners.add(object: MyListener {
            override fun onFoo(param1: String) {
                expect("foo") { param1 }
                called++
            }

            override fun onBar(param2: Int) {
                expect(25) { param2 }
                called++
            }
        })
        listeners.fire.onFoo("foo")
        listeners.fire.onBar(25)
        expect(2) { called }
    }

    @Test fun `unregistered listeners are not invoked`() {
        val listeners = listeners<MyListener>()
        val listener = object : MyListener {
            override fun onFoo(param1: String) = fail("shouldn't be called")
            override fun onBar(param2: Int) = fail("shouldn't be called")
        }
        listeners.add(listener)
        listeners.remove(listener)
        listeners.fire.onFoo("foo")
        listeners.fire.onBar(1)
    }

    @Test fun toStringTests() {
        val listeners = listeners<MyListener>()
        val listener = object : MyListener {
            override fun onFoo(param1: String) = fail("shouldn't be called")
            override fun onBar(param2: Int) = fail("shouldn't be called")
            override fun toString(): String = "MyListener"
        }
        listeners.add(listener)
        expect("Listeners(interface eu.vaadinonkotlin.MyListener, listeners=[MyListener])") { listeners.toString() }
    }

    @Test fun serializable() {
        val listeners = listeners<MyListener>()
        val listener = object : MyListener {
            override fun onFoo(param1: String) = fail("shouldn't be called")
            override fun onBar(param2: Int) = fail("shouldn't be called")
        }
        listeners.add(listener)
        listeners.cloneBySerialization()
    }
}
