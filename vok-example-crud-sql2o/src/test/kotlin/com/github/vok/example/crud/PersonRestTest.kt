package com.github.vok.example.crud

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.example.crud.personeditor.Person
import com.github.vok.example.crud.personeditor.usingApp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.javalin.Javalin
import khttp.responses.Response
import java.io.IOException
import java.io.Reader
import kotlin.test.expect

fun Response.checkOk(): Response {
    if (statusCode !in 200..299) throw IOException("$statusCode: $text ($url)")
    return this
}

val Response.reader: Reader get() {
    val e = encoding
    return raw.reader(e).buffered()
}

class PersonRestClient2(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    fun helloWorld(): String = khttp.get("$baseUrl/person/helloworld").checkOk().text
    val personCrud: CrudClient<Person> get() = CrudClient("$baseUrl/person", Person::class.java)
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        GsonBuilder().create().configureToJavalin()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingApp()  // to bootstrap the app to have access to the database.

    lateinit var client: PersonRestClient2
    beforeEach { client = PersonRestClient2("http://localhost:9876/rest") }

    test("hello world") {
        expect("Hello World") { client.helloWorld() }
    }

    group("crud") {
        test("getAll()") {
            expectList() { client.personCrud.getAll() }
        }
    }
})

class CrudClient<T>(val baseUrl: String, val beanClass: Class<T>, val gson: Gson = GsonBuilder().create()) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }

    fun getAll(): List<T> {
        val response = khttp.get(baseUrl).checkOk()
        val type = TypeToken.getParameterized(List::class.java, beanClass).type
//        val text = response.reader.readText()
        val text = response.text
        return gson.fromJson<List<T>>(text, type)
    }
}
