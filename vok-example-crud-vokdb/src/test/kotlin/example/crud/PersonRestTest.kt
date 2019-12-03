package example.crud

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import eu.vaadinonkotlin.restclient.OkHttpClientVokPlugin
import eu.vaadinonkotlin.restclient.exec
import eu.vaadinonkotlin.restclient.jsonArray
import example.crud.personeditor.MaritalStatus
import example.crud.personeditor.Person
import example.crud.personeditor.usingApp
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import kotlin.test.expect

class PersonRestClient(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    private val client: OkHttpClient = OkHttpClientVokPlugin.okHttpClient!!
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
        javalin = Javalin.create { it.showJavalinBanner = false }
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
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        p.created = p.created!!.withZeroNanos
        val all = client.getAll().onEach { it.created = it.created!!.withZeroNanos }  // JDK11 fix
        expectList(p) { all }
    }
})

val Instant.withZeroNanos: Instant get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
