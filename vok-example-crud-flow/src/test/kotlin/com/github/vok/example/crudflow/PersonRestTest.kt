package com.github.vok.example.crudflow

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.example.crudflow.person.MaritalStatus
import com.github.vok.example.crudflow.person.Person
import com.github.vok.example.crudflow.person.usingApp
import com.github.vok.restclient.RetrofitClientVokPlugin
import com.github.vok.restclient.exec
import com.github.vok.restclient.jsonArray
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import kotlin.test.expect

class PersonRestClient(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    private val client: OkHttpClient = RetrofitClientVokPlugin.okHttpClient!!
    fun helloWorld(): String {
        val request = Request.Builder().url("${baseUrl}/person/helloworld").build()
        return client.exec(request) { response -> response.string() }
    }
    fun getAll(): List<Person> {
        val request = Request.Builder().url("${baseUrl}/person").build()
        return client.exec(request) { response -> response.jsonArray(Person::class.java) }
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

    lateinit var client: PersonRestClient
    beforeEach { client = PersonRestClient("http://localhost:9876/rest") }

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
