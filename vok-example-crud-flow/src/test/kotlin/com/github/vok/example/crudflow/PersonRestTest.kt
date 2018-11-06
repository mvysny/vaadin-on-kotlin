package com.github.vok.example.crudflow

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.example.crudflow.person.MaritalStatus
import com.github.vok.example.crudflow.person.Person
import com.github.vok.example.crudflow.person.usingApp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.javalin.Javalin
import khttp.responses.Response
import java.io.IOException
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

fun Response.checkOk(): Response {
    if (statusCode !in 200..299) throw IOException("$statusCode: $text ($url)")
    return this
}

class PersonRestClient2(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    val gson: Gson = GsonBuilder().create()
    fun helloWorld(): String = khttp.get("$baseUrl/person/helloworld").checkOk().text
    fun getAll(): List<Person> {
        val text = khttp.get("$baseUrl/person").checkOk().text
        val type = TypeToken.getParameterized(List::class.java, Person::class.java).type
        return gson.fromJson<List<Person>>(text, type)
    }
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingApp()  // to bootstrap the app to have access to the database.

    lateinit var client: PersonRestClient2
    beforeEach { client = PersonRestClient2("http://localhost:9876/rest") }

    test("hello world") {
        expect("Hello World") { client.helloWorld() }
    }

    test("get all users") {
        expectList() { client.getAll() }
        val p = Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        val all = client.getAll()
        p.created = all[0].created
        expectList(p) { all }
    }
})
