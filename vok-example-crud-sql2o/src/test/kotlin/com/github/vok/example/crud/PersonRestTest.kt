package com.github.vok.example.crud

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.example.crud.personeditor.MaritalStatus
import com.github.vok.example.crud.personeditor.Person
import com.github.vok.example.crud.personeditor.usingApp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.javalin.Javalin
import khttp.responses.Response
import java.io.IOException
import java.io.Reader
import java.time.LocalDate
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
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
            p.save()
            expectList(p) { client.personCrud.getAll() }
        }

        test("getOne") {
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
            p.save()
            expect(p) { client.personCrud.getOne(p.id!!.toString()) }
            expectThrows(IOException::class, "404: No such entity with ID 555") {
                client.personCrud.getOne("555")
            }
            expectThrows(IOException::class, "Malformed ID: foobar") {
                client.personCrud.getOne("foobar")
            }
        }
    }
})

class CrudClient<T>(val baseUrl: String, val beanClass: Class<T>, val gson: Gson = GsonBuilder().create()) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }

    fun getAll(): List<T> {
        val text = khttp.get(baseUrl).checkOk().text
        val type = TypeToken.getParameterized(List::class.java, beanClass).type
        return gson.fromJson<List<T>>(text, type)
    }

    fun getOne(id: String): T {
        val text = khttp.get("$baseUrl/$id").checkOk().text
        return gson.fromJson(text, beanClass)
    }
}
