package example.crudflow

import eu.vaadinonkotlin.restclient.*
import example.crudflow.person.MaritalStatus
import example.crudflow.person.Person
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.time.LocalDate
import kotlin.test.expect

class PersonRestClient(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    private val client: HttpClient = VokRestClient.httpClient
    fun helloWorld(): String {
        val request = "${baseUrl}/person/helloworld".buildUrl().buildRequest()
        return client.exec(request) { response -> response.bodyAsString() }
    }
    fun getAll(): List<Person> {
        val request = "${baseUrl}/person".buildUrl().buildRequest()
        return client.exec(request) { response -> response.jsonArray(Person::class.java) }
    }
    fun getAllRaw(): String {
        val request = "${baseUrl}/person".buildUrl().buildRequest()
        return client.exec(request) { response -> response.bodyAsString() }
    }
}

abstract class AbstractJavalinTest : AbstractAppTest() {
    companion object {
        lateinit var server: Server
        @BeforeAll @JvmStatic fun startJavalin() {
            val ctx = WebAppContext()
            // This used to be EmptyResource, but it got removed in Jetty 12. Let's use some dummy resource instead.
            ctx.baseResource = ctx.resourceFactory.newClassPathResource("java/lang/String.class")
            ctx.addServlet(JavalinRestServlet::class.java, "/rest/*")
            server = Server(9876)
            server.handler = ctx
            server.start()
        }
        @AfterAll @JvmStatic fun stopJavalin() { server.stop() }
    }
}

class PersonRestTest : AbstractJavalinTest() {
    private val client = PersonRestClient("http://localhost:9876/rest")

    @Test fun helloWorld() {
        expect("Hello World") { client.helloWorld() }
    }

    @Test fun `LocalDate serialization`() {
        val p = Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        val all = client.getAllRaw()
        expect(true, all) { all.contains(""""dateOfBirth":"1980-05-01"""") }
    }

    @Test fun `get all users`() {
        expect(listOf()) { client.getAll() }
        val p = Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        val all = client.getAll()
        p.created = all[0].created
        expect(listOf(p)) { all }
    }
}
