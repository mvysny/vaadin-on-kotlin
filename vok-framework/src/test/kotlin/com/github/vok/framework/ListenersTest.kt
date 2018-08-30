package com.github.vok.framework

import com.github.mvysny.dynatest.DynaTest
import java.io.Serializable
import kotlin.test.expect

interface MyListener : Serializable {
    fun onFoo(param1: String)
    fun onBar(param2: Int)
}

class ListenersTest : DynaTest({
    test("empty listeners do nothing") {
        val listeners = Listeners<MyListener>()
        listeners.fire.onFoo("foo")
        listeners.fire.onBar(25)
    }

    test("listeners get invoked") {
        val listeners = Listeners<MyListener>()
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

    test("unregistered listeners are not invoked") {
        val listeners = Listeners<MyListener>()
        val listener = object : MyListener {
            override fun onFoo(param1: String) = kotlin.test.fail("shouldn't be called")
            override fun onBar(param2: Int) = kotlin.test.fail("shouldn't be called")
        }
        listeners.add(listener)
        listeners.remove(listener)
        listeners.fire.onFoo("foo")
        listeners.fire.onBar(1)
    }
})
