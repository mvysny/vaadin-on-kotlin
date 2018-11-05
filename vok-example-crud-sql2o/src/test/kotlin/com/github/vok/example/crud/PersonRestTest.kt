package com.github.vok.example.crud

import com.github.mvysny.dynatest.DynaTest
import io.javalin.Javalin
import khttp.responses.Response
import java.io.IOException
import kotlin.test.expect

fun Response.checkOk(): Response {
    if (statusCode !in 200..299) throw IOException("$statusCode: $text ($url)")
    return this
}

class PersonRestClient2(val baseUrl: String) {
    fun helloWorld(): String = khttp.get("$baseUrl/person/helloworld").checkOk().text
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    lateinit var client2: PersonRestClient2
    beforeEach {
        client2 = PersonRestClient2("http://localhost:9876/rest")
    }

    test("hello world") {
        expect("Hello World") { client2.helloWorld() }
    }
})
