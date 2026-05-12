package example.crudflow

import eu.vaadinonkotlin.restclient.*
import example.crudflow.person.MaritalStatus
import example.crudflow.person.Person
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.Resource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.nio.file.Path
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
            // Jetty 12 removed EmptyResource and as of 12.1.8 rejects non-directory resources
            // as baseResource, so we provide a minimal directory-like Resource ourselves.
            ctx.baseResource = EmptyResource()
            ctx.addServlet(JavalinRestServlet::class.java, "/rest/*")
            server = Server(9876)
            server.handler = ctx
            server.start()
        }
        @AfterAll @JvmStatic fun stopJavalin() { server.stop() }
    }
}

class EmptyResource : Resource() {
    override fun getPath(): Path? = null
    override fun isDirectory(): Boolean = true
    override fun isReadable(): Boolean = true
    override fun getURI(): URI? = null
    override fun getName(): String = "EmptyResource"
    override fun getFileName(): String? = null
    override fun resolve(subUriPath: String?): Resource? = null
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
