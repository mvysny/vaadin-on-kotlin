package eu.vaadinonkotlin.rest

import com.github.vokorm.*
import com.gitlab.mvysny.jdbiorm.OrderBy
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.condition.FullTextCondition
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import eu.vaadinonkotlin.restclient.*
import io.javalin.Javalin
import org.eclipse.jetty.server.Server
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.http.HttpClient
import kotlin.test.expect

class MyJavalinServlet : HttpServlet() {
    private val javalin = Javalin.createStandalone { it.gsonMapper(VokRest.gson) } .apply {
        get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
        crud2("/rest/person", Person.getCrudHandler(true))
    } .javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

// Demoes direct access via httpclient
class PersonRestClient(val baseUrl: String) {
    private val client: HttpClient = VokRestClient.httpClient
    fun helloWorld(): String {
        val request = "${baseUrl}helloworld".buildUrl().buildRequest()
        return client.exec(request) { response -> response.bodyAsString() }
    }
    fun getAll(): List<Person> {
        val request = baseUrl.buildUrl().buildRequest()
        return client.exec(request) { response -> response.jsonArray(Person::class.java) }
    }
}

abstract class AbstractJavalinTest : AbstractDbTest() {
    companion object {
        lateinit var server: Server
        @BeforeAll @JvmStatic fun startJavalin() {
            val ctx = WebAppContext()
            // This used to be EmptyResource, but it got removed in Jetty 12. Let's use some dummy resource instead.
            ctx.baseResource = ctx.resourceFactory.newClassPathResource("java/lang/String.class")
            ctx.addServlet(MyJavalinServlet::class.java, "/rest/*")
            server = Server(9876)
            server.handler = ctx
            server.start()
        }
        @AfterAll @JvmStatic fun stopJavalin() { server.stop() }
    }
}


private fun <T> DataProvider<T, Condition>.getAll(condition: Condition? = null, sortBy: List<OrderBy> = listOf(), range: IntRange = 0..10000) =
    fetch(Query(range.start, range.length, sortBy.map { QuerySortOrder(it.property.name.name, if (it.order == OrderBy.Order.ASC) SortDirection.ASCENDING else SortDirection.DESCENDING) }, null, condition)).toList()
private fun <T> DataProvider<T, Condition>.getCount(condition: Condition? = null): Int =
    size(Query<T, Condition>(condition))

class PersonRestTest : AbstractJavalinTest() {
    @Test fun helloWorld() {
        val client = PersonRestClient("http://localhost:9876/rest/person/")
        expect("Hello World") { client.helloWorld() }
        expect(listOf()) { client.getAll() }
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        expect(listOf(p)) { client.getAll() }
    }

    @Nested inner class crud {
        private val crud = CrudClient("http://localhost:9876/rest/person/", Person::class.java)
        @Nested inner class getAll() {
            @Test fun simple() {
                expect(listOf()) { crud.getAll() }
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                expect(listOf(p)) { crud.getAll() }
            }

            @Test fun range() {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect((0..80).toList()) { crud.getAll().map { it.age!! - 15 } }
                expect((10..80).toList()) { crud.getAll(range = 10..1000).map { it.age!! - 15 } }
                expect((10..20).toList()) { crud.getAll(range = 10..20).map { it.age!! - 15} }
            }

            @Test fun sort() {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect((0..80).toList()) { crud.getAll(null, listOf(Person::age.asc)).map { it.age!! - 15 } }
                expect((0..80).toList().reversed()) { crud.getAll(null, listOf(Person::age.desc)).map { it.age!! - 15 } }
                expect((0..80).toSet()) { crud.getAll(null, listOf(Person::personName.asc)).map { it.age!! - 15 } .toSet() }
            }

            @Test fun count() {
                expect(0) { crud.getCount() }
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(81) { crud.getCount() }
            }

            @Test fun filter() {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(0) { crud.getCount(buildCondition { Person::age ge 130 })}
                expect(5) { crud.getCount(buildCondition { Person::age lt 20 })}
                expect(81) { crud.getCount(buildCondition { Person::personName eq "Duke Leto Atreides" })}
                expect(0) { crud.getCount(buildCondition { Person::personName like "duke%" })}
                expect(81) { crud.getCount(buildCondition { Person::personName likeIgnoreCase "duke%" })}
                expect((0..4).toList()) { crud.getAll(buildCondition { Person::age lt 20 }, listOf(Person::age.asc)).map { it.age!! - 15 } }
                expect(81) { crud.getCount(buildCondition { Person::dateOfBirth eq LocalDate.of(1980, 5, 1) })}
                expect(0) { crud.getCount(FullTextCondition.of(Person::personName.exp, "duke")) }
            }

            @Test fun `filter on same fields`() {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(0) { crud.getCount(buildCondition { Person::age lt 20 and (Person::age gt 30) })}
                expect(listOf()) { crud.getAll(buildCondition { Person::age lt 20 and (Person::age gt 30) })}
            }
        }

        @Nested inner class getOne {
            @Test fun simple() {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                expect(p) { crud.getOne(p.id!!.toString()) }
            }
            @Test fun `non-existing`() {
                val ex = assertThrows<IOException> {
                    crud.getOne("555")
                }
                expect("404: No such entity with ID 555 (GET http://localhost:9876/rest/person/555)") { ex.message }
            }
            @Test fun malformedid() {
                val ex = assertThrows<IOException> {
                    crud.getOne("foobar")
                }
                expect("404: Malformed ID: foobar (GET http://localhost:9876/rest/person/foobar)") { ex.message }
            }
        }

        @Test fun create() {
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now().withZeroNanos)
            crud.create(p)
            val actual = db { Person.findAll() }
            p.id = actual[0].id!!
            expect(listOf(p)) { actual }
        }

        @Nested inner class delete {
            @Test fun simple() {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                crud.delete(p.id!!.toString())
                expect(listOf()) { Person.findAll() }
            }
            @Test fun nonExisting() {
                // never fail with 404: http://www.tugberkugurlu.com/archive/http-delete-http-200-202-or-204-all-the-time
                crud.delete("555")
            }
            @Test fun invalidId() {
                val ex = assertThrows<IOException> {
                    crud.delete("invalid_id")
                }
                expect("404: Malformed ID: invalid_id (DELETE http://localhost:9876/rest/person/invalid_id)") { ex.message }
            }
        }

        @Nested inner class update {
            @Test fun simple() {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                p.personName = "Leto Atreides"
                crud.update(p.id!!.toString(), p)
                expect(listOf(p)) { Person.findAll() }
            }
            @Test fun nonExisting() {
                val p = Person(id = 45, personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
                val ex = assertThrows<IOException> {
                    crud.update(p.id!!.toString(), p)
                }
                expect("404: No such entity with ID 45 (PATCH http://localhost:9876/rest/person/45)") { ex.message }
            }
        }
    }
    @Nested inner class `bind client to non-Entity class` {
        @Nested inner class `get all` {
            private val client: CrudClient<Person2> = CrudClient("http://localhost:9876/rest/person/", Person2::class.java)

            @Test fun `simple smoke test`() {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                val p2 = Person2(p.id, 45, "Duke Leto Atreides")
                val all = client.getAll()
                expect(listOf(p2)) { all }
            }

            @Test fun filters() {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                val p2 = Person2(p.id, 45, "Duke Leto Atreides")
                expect(listOf()) { client.getAll(buildCondition { Person2::personName likeIgnoreCase "baron%" }) }
                expect(listOf(p2)) { client.getAll(buildCondition { Person2::personName likeIgnoreCase "duke%" }) }
            }

            @Test fun sorting() {
                expect(listOf()) { client.getAll(sortBy = listOf(Person2::personName.asc)) }
            }
        }
    }
}

data class Person2(var id: Long? = null, var age: Int? = null, var personName: String? = null)

val Instant.withZeroNanos: Instant get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
