package eu.vaadinonkotlin.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.mock
import jakarta.servlet.http.Cookie
import kotlin.test.expect

class SessionTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    group("session") {
        test("getOrPut") {
            var invocationCount = 0
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect(1) { invocationCount }
        }
    }
    group("cookies") {
        test("simple add cookie") {
            Cookies += Cookie("foo", "bar")
            val mockResponse = currentResponse.mock
            expect("bar", mockResponse.cookies.joinToString { "${it.name} -> ${it.value}"}) { mockResponse.cookies.firstOrNull { it.name == "foo" } ?.value }
            Cookies.delete("foo")
            expect(null) { mockResponse.cookies.firstOrNull { it.name == "bar" } ?.value }
        }
    }
})
