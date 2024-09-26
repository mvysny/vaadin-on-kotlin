package eu.vaadinonkotlin.restclient

import io.javalin.Javalin
import org.eclipse.jetty.server.Server
import java.io.FileNotFoundException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import kotlin.test.expect

data class Person(var name: String? = null,
                  var surname: String? = null,
                  var age: Int? = null)

class MyJavalinServlet : HttpServlet() {
    private val javalin = Javalin.createStandalone()
        .get("foo") { ctx -> ctx.result(content) }
        .get("fail") { throw RuntimeException() }
        .javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
    companion object {
        var content: String = ""
    }
}

abstract class AbstractJavalinTest {
    companion object {
        lateinit var server: Server
        @BeforeAll @JvmStatic fun startJavalin() {
            val ctx = WebAppContext()
            // This used to be EmptyResource, but it got removed in Jetty 12. Let's use some dummy resource instead.
            ctx.baseResource = ctx.resourceFactory.newClassPathResource("java/lang/String.class")
            ctx.addServlet(MyJavalinServlet::class.java, "/*")
            server = Server(9876)
            server.handler = ctx
            server.start()
        }
        @AfterAll @JvmStatic fun stopJavalin() { server.stop() }
    }
}

class HttpClientUtilsTest : AbstractJavalinTest() {
    fun String.get(): HttpRequest = HttpRequest.newBuilder(URI.create(this)).GET().build()
    private val request: HttpRequest = "http://localhost:9876/foo".get()
    fun client(): HttpClient = VokRestClient.httpClient

    @Test fun testjson() {
        MyJavalinServlet.content = """{"name":"John", "surname":"Doe"}"""
        expect(Person("John", "Doe")) { client().exec(request) { it.json(Person::class.java) } }
    }

    @Test fun test404() {
        val ex = assertThrows<FileNotFoundException> {
            client().exec("http://localhost:9876/bar".get()) {}
        }
        expect("404: Not Found (GET http://localhost:9876/bar)") { ex.message }
    }

    @Test fun test500() {
        val ex = assertThrows<HttpResponseException> {
            client().exec("http://localhost:9876/fail".get()) {}
        }
        expect("500: Server Error") { ex.message }
    }

    @Nested inner class jsonArray() {
        @Test fun empty() {
            MyJavalinServlet.content = """[]"""
            expect(listOf()) { client().exec(request) { it.jsonArray(Person::class.java) } }
        }
        @Test fun simple() {
            MyJavalinServlet.content = """[{"name":"John", "surname":"Doe"}]"""
            expect(listOf(Person("John", "Doe"))) { client().exec(request) { it.jsonArray(Person::class.java) } }
        }
    }

    @Nested inner class jsonMap() {
        @Test fun empty() {
            MyJavalinServlet.content = """{}"""
            expect(mapOf()) { client().exec(request) { it.jsonMap(Person::class.java) } }
        }
        @Test fun simple() {
            MyJavalinServlet.content = """{"director": {"name":"John", "surname":"Doe"}}"""
            expect(mapOf("director" to Person("John", "Doe"))) { client().exec(request) { it.jsonMap(Person::class.java) } }
        }
    }

    @Nested inner class buildUrl() {
        @Test fun simpleBuild() {
            expect("http://hello.com/") {
                "http://hello.com/".buildUrl {  } .toString()
            }
            expect("http://hello.com") {
                "http://hello.com".buildUrl {  } .toString()
            }
        }
        @Test fun `add query parameters`() {
            expect("http://hello.com/?q=foo%20bar") {
                "http://hello.com/".buildUrl { addParameter("q", "foo bar") }.toString()
            }
        }
        @Test fun `fails with invalid URL`() {
            val ex = assertThrows<IllegalArgumentException> {
                "hello.com".buildUrl {}
            }
            expect("Expected URL scheme 'http' or 'https' but got null: hello.com") { ex.message }
        }
        @Test fun `duplicite query parameters supported`() {
            expect("http://hello.com/?q=foo&q=bar") {
                "http://hello.com/".buildUrl { addParameter("q", "foo").addParameter("q", "bar") }.toString()
            }
        }
    }
}
