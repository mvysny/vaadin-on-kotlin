package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.fake
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

class SessionTest {
    @BeforeEach fun fakeVaadin() { MockVaadin.setup() }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }

    @Nested inner class SessionTests {
        @Test fun getOrPut() {
            var invocationCount = 0
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect("0") { Session.getOrPut(String::class) { "${invocationCount++}" } }
            expect(1) { invocationCount }
        }
    }
    @Nested inner class CookiesTests {
        @Test fun `simple add cookie`() {
            Cookies += Cookie("foo", "bar")
            val mockResponse = currentResponse.fake
            expect("bar", mockResponse.cookies.joinToString { "${it.name} -> ${it.value}"}) { mockResponse.cookies.firstOrNull { it.name == "foo" } ?.value }
            Cookies.delete("foo")
            expect(null) { mockResponse.cookies.firstOrNull { it.name == "bar" } ?.value }
        }
    }
}
