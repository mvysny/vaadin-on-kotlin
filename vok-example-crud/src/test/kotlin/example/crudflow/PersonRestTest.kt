package example.crudflow

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectList
import example.crudflow.person.MaritalStatus
import example.crudflow.person.Person
import eu.vaadinonkotlin.restclient.OkHttpClientVokPlugin
import eu.vaadinonkotlin.restclient.exec
import eu.vaadinonkotlin.restclient.jsonArray
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.EmptyResource
import org.eclipse.jetty.webapp.WebAppContext
import java.time.LocalDate
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
    fun getAllRaw(): String {
        val request = Request.Builder().url("${baseUrl}/person").build()
        return client.exec(request) { response -> response.string() }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.usingJavalin() {
    lateinit var server: Server
    beforeGroup {
        val ctx = WebAppContext()
        ctx.baseResource = EmptyResource.INSTANCE
        ctx.addServlet(JavalinRestServlet::class.java, "/rest/*")
        server = Server(9876)
        server.handler = ctx
        server.start()
    }
    afterGroup { server.stop() }
}

class PersonRestTest : DynaTest({
    usingApp()  // to bootstrap the app to have access to the database.
    usingJavalin() // bootstrap REST server

    lateinit var client: PersonRestClient
    beforeEach { client = PersonRestClient("http://localhost:9876/rest") }

    test("hello world") {
        expect("Hello World") { client.helloWorld() }
    }

    test("LocalDate serialization") {
        val p = Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        val all = client.getAllRaw()
        expect(true, all) { all.contains(""""dateOfBirth":"1980-05-01"""") }
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
