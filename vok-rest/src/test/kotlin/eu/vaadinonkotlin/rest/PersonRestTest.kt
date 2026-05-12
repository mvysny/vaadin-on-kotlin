package eu.vaadinonkotlin.rest

import com.github.mvysny.ktormvaadin.db
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import eu.vaadinonkotlin.restclient.*
import io.javalin.Javalin
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.Resource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import kotlin.test.expect

class MyJavalinServlet : HttpServlet() {
    private val javalin = Javalin.createStandalone { it.gsonMapper(VokRest.gson) } .apply {
        get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
        crud2("/rest/person", Persons.getCrudHandler(allowModification = true))
    } .javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

class PersonRestClient(val baseUrl: String) {
    private val client: HttpClient = VokRestClient.httpClient
    fun helloWorld(): String {
        val request = "${baseUrl}helloworld".buildUrl().buildRequest()
        return client.exec(request) { response -> response.bodyAsString() }
    }
}

abstract class AbstractJavalinTest : AbstractDbTest() {
    companion object {
        lateinit var server: Server
        @BeforeAll @JvmStatic fun startJavalin() {
            val ctx = WebAppContext()
            ctx.baseResource = EmptyResource()
            ctx.addServlet(MyJavalinServlet::class.java, "/rest/*")
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

private fun newPerson(
    name: String = "Duke Leto Atreides",
    age: Int = 45,
    dateOfBirth: LocalDate? = LocalDate.of(1980, 5, 1),
    maritalStatus: MaritalStatus = MaritalStatus.Single,
    alive: Boolean = false
): Person = Person {
    this.personName = name
    this.age = age
    this.dateOfBirth = dateOfBirth
    this.created = Instant.now().withZeroNanos
    this.maritalStatus = maritalStatus
    this.alive = alive
}

private fun savePerson(person: Person): Person {
    db { database.sequenceOf(Persons).add(person) }
    return person
}

private fun seed81People() {
    db {
        (0..80).forEach { i ->
            val p = newPerson(age = i + 15)
            database.sequenceOf(Persons).add(p)
        }
    }
}

class PersonRestTest : AbstractJavalinTest() {
    private val crud = CrudClient("http://localhost:9876/rest/person/", PersonDto::class.java)

    @Test fun helloWorld() {
        val client = PersonRestClient("http://localhost:9876/rest/person/")
        expect("Hello World") { client.helloWorld() }
    }

    @Nested inner class getAll {
        @Test fun empty() {
            expect(listOf()) { crud.getAll() }
        }

        @Test fun simple() {
            val saved = savePerson(newPerson())
            val all = crud.getAll()
            expect(1) { all.size }
            expect(saved.personName) { all[0].personName }
            expect(saved.age) { all[0].age }
            expect(saved.dateOfBirth) { all[0].dateOfBirth }
            expect(saved.maritalStatus) { all[0].maritalStatus }
            expect(saved.alive) { all[0].alive }
        }

        @Test fun pagination() {
            seed81People()
            expect((0..80).toList()) { crud.getAll().map { it.age!! - 15 } }
            expect((10..80).toList()) { crud.getAll(offset = 10).map { it.age!! - 15 } }
            expect((10..20).toList()) { crud.getAll(offset = 10, limit = 11).map { it.age!! - 15 } }
        }

        @Test fun sortAsc() {
            seed81People()
            val sorted = crud.getAll(sortBy = listOf(QuerySortOrder("age", SortDirection.ASCENDING)))
            expect((0..80).toList()) { sorted.map { it.age!! - 15 } }
        }

        @Test fun sortDesc() {
            seed81People()
            val sorted = crud.getAll(sortBy = listOf(QuerySortOrder("age", SortDirection.DESCENDING)))
            expect((0..80).toList().reversed()) { sorted.map { it.age!! - 15 } }
        }

        @Test fun multiColumnSort() {
            db {
                database.sequenceOf(Persons).add(newPerson(name = "alpha", age = 20))
                database.sequenceOf(Persons).add(newPerson(name = "beta", age = 20))
                database.sequenceOf(Persons).add(newPerson(name = "alpha", age = 30))
            }
            val sorted = crud.getAll(sortBy = listOf(
                QuerySortOrder("age", SortDirection.ASCENDING),
                QuerySortOrder("name", SortDirection.DESCENDING)
            ))
            expect(listOf("beta" to 20, "alpha" to 20, "alpha" to 30)) {
                sorted.map { it.personName to it.age }
            }
        }

        @Test fun count() {
            expect(0) { crud.size(com.vaadin.flow.data.provider.Query()) }
            seed81People()
            expect(81) { crud.size(com.vaadin.flow.data.provider.Query()) }
        }

        @Test fun `eq filter on string column`() {
            savePerson(newPerson(name = "Leto"))
            savePerson(newPerson(name = "Paul"))
            expect(listOf("Leto")) { crud.getAll(filter = mapOf("name" to "Leto")).map { it.personName } }
        }

        @Test fun `eq filter on int column`() {
            seed81People()
            expect(listOf(20)) { crud.getAll(filter = mapOf("age" to "20")).map { it.age } }
        }

        @Test fun `eq filter on boolean column`() {
            savePerson(newPerson(name = "Alive", alive = true))
            savePerson(newPerson(name = "Dead", alive = false))
            expect(listOf("Alive")) { crud.getAll(filter = mapOf("alive" to "true")).map { it.personName } }
        }

        @Test fun `eq filter on enum column`() {
            savePerson(newPerson(name = "Married Soul", maritalStatus = MaritalStatus.Married))
            savePerson(newPerson(name = "Single Soul", maritalStatus = MaritalStatus.Single))
            expect(listOf("Married Soul")) {
                crud.getAll(filter = mapOf("maritalStatus" to "Married")).map { it.personName }
            }
        }

        @Test fun `eq filter on date column`() {
            savePerson(newPerson(name = "Old", dateOfBirth = LocalDate.of(1900, 1, 1)))
            savePerson(newPerson(name = "Young", dateOfBirth = LocalDate.of(2000, 1, 1)))
            expect(listOf("Old")) {
                crud.getAll(filter = mapOf("dateOfBirth" to "1900-01-01")).map { it.personName }
            }
        }

        @Test fun `multiple eq filters AND together`() {
            savePerson(newPerson(name = "Match", age = 30, alive = true))
            savePerson(newPerson(name = "WrongAge", age = 31, alive = true))
            savePerson(newPerson(name = "WrongAlive", age = 30, alive = false))
            expect(listOf("Match")) {
                crud.getAll(filter = mapOf("age" to "30", "alive" to "true")).map { it.personName }
            }
        }

        @Test fun `unknown column returns 400`() {
            val ex = assertThrows<HttpResponseException> {
                crud.getAll(filter = mapOf("nonexistent" to "x"))
            }
            expect(400) { ex.statusCode }
        }

        @Test fun `bad value coercion returns 400`() {
            val ex = assertThrows<HttpResponseException> {
                crud.getAll(filter = mapOf("age" to "not-a-number"))
            }
            expect(400) { ex.statusCode }
        }

        @Test fun `bad sort direction returns 400`() {
            // CrudClient can't produce an invalid sort direction (it uses Vaadin's SortDirection enum),
            // so we hit the raw URL to exercise the server's sort parser.
            val raw = "http://localhost:9876/rest/person?sort=age:nope".buildUrl().buildRequest()
            val ex = assertThrows<HttpResponseException> { VokRestClient.httpClient.exec(raw) {} }
            expect(400) { ex.statusCode }
        }
    }

    @Nested inner class getOne {
        @Test fun simple() {
            val p = savePerson(newPerson())
            val fetched = crud.getOne(p.id!!.toString())
            expect(p.personName) { fetched.personName }
        }

        @Test fun `non-existing returns 404`() {
            assertThrows<java.io.FileNotFoundException> { crud.getOne("555") }
        }

        @Test fun `malformed id returns 404`() {
            assertThrows<java.io.FileNotFoundException> { crud.getOne("foobar") }
        }
    }

    @Nested inner class create {
        @Test fun `inserts row with generated id`() {
            val dto = PersonDto(
                personName = "Paul Atreides", age = 17,
                dateOfBirth = LocalDate.of(2009, 1, 1),
                created = Instant.now().withZeroNanos,
                maritalStatus = MaritalStatus.Single, alive = true,
            )
            crud.create(dto)
            val stored = db { database.sequenceOf(Persons).toList() }
            expect(1) { stored.size }
            expect("Paul Atreides") { stored[0].personName }
            expect(17) { stored[0].age }
            expect(LocalDate.of(2009, 1, 1)) { stored[0].dateOfBirth }
            expect(MaritalStatus.Single) { stored[0].maritalStatus }
            expect(true) { stored[0].alive }
            // id is auto-generated by H2; just assert one was assigned
            expect(true, "expected generated id, got ${stored[0].id}") { (stored[0].id ?: 0L) > 0L }
        }

        @Test fun `unknown property returns 400`() {
            val raw = "http://localhost:9876/rest/person".buildUrl()
                .buildRequest { post("""{"nope":1}""", eu.vaadinonkotlin.MediaType.jsonUtf8) }
            val ex = assertThrows<HttpResponseException> { VokRestClient.httpClient.exec(raw) {} }
            expect(400) { ex.statusCode }
        }
    }

    @Nested inner class update {
        @Test fun `partial body only updates supplied columns`() {
            val saved = savePerson(newPerson(name = "Old Name", age = 50, alive = true))
            // PATCH only `age` — name and alive should stay.
            crud.update(saved.id!!.toString(), PersonDto(age = 99))
            val refreshed = db { database.sequenceOf(Persons).find { Persons.id eq saved.id!! } }!!
            expect("Old Name") { refreshed.personName }
            expect(99) { refreshed.age }
            expect(true) { refreshed.alive }
        }

        @Test fun `nullable column can be cleared by sending null`() {
            val saved = savePerson(newPerson(dateOfBirth = LocalDate.of(1980, 5, 1)))
            val rawBody = """{"dateOfBirth":null}"""
            val req = "http://localhost:9876/rest/person/${saved.id}".buildUrl()
                .buildRequest { patch(rawBody, eu.vaadinonkotlin.MediaType.jsonUtf8) }
            VokRestClient.httpClient.exec(req) {}
            val refreshed = db { database.sequenceOf(Persons).find { Persons.id eq saved.id!! } }!!
            expect(null) { refreshed.dateOfBirth }
        }

        @Test fun `non-existing id returns 404`() {
            assertThrows<java.io.FileNotFoundException> {
                crud.update("99999", PersonDto(personName = "ghost"))
            }
        }

        @Test fun `empty body returns 400`() {
            val req = "http://localhost:9876/rest/person/1".buildUrl()
                .buildRequest { patch("", eu.vaadinonkotlin.MediaType.jsonUtf8) }
            val ex = assertThrows<HttpResponseException> { VokRestClient.httpClient.exec(req) {} }
            expect(400) { ex.statusCode }
        }
    }

    @Nested inner class delete {
        @Test fun `removes the row`() {
            val saved = savePerson(newPerson())
            crud.delete(saved.id!!.toString())
            expect(0) { db { database.sequenceOf(Persons).toList().size } }
        }

        @Test fun `non-existing id returns 404`() {
            assertThrows<java.io.FileNotFoundException> { crud.delete("99999") }
        }
    }
}

val Instant.withZeroNanos: Instant
    get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
