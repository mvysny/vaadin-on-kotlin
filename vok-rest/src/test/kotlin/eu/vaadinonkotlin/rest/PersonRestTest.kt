package eu.vaadinonkotlin.rest

import com.github.mvysny.dynatest.*
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

@DynaTestDsl
fun DynaNodeGroup.usingJavalin() {
    lateinit var server: Server
    beforeGroup {
        val ctx = WebAppContext()
        // This used to be EmptyResource, but it got removed in Jetty 12. Let's use some dummy resource instead.
        ctx.baseResource = ctx.resourceFactory.newClassPathResource("java/lang/String.class")
        ctx.addServlet(MyJavalinServlet::class.java, "/rest/*")
        server = Server(9876)
        server.handler = ctx
        server.start()
    }
    afterGroup { server.stop() }
}


private fun <T> DataProvider<T, Condition>.getAll(condition: Condition? = null, sortBy: List<OrderBy> = listOf(), range: IntRange = 0..10000) =
    fetch(Query(range.start, range.length, sortBy.map { QuerySortOrder(it.property.name.name, if (it.order == OrderBy.Order.ASC) SortDirection.ASCENDING else SortDirection.DESCENDING) }, null, condition)).toList()
private fun <T> DataProvider<T, Condition>.getCount(condition: Condition? = null): Int =
    size(Query<T, Condition>(condition))

class PersonRestTest : DynaTest({

    usingJavalin()
    usingDb()  // to have access to the database.

    test("hello world") {
        val client = PersonRestClient("http://localhost:9876/rest/person/")
        expect("Hello World") { client.helloWorld() }
        expectList() { client.getAll() }
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        expectList(p) { client.getAll() }
    }

    group("crud") {
        lateinit var crud: CrudClient<Person>
        beforeEach { crud = CrudClient("http://localhost:9876/rest/person/", Person::class.java) }
        group("getAll()") {
            test("simple") {
                expectList() { crud.getAll() }
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                expectList(p) { crud.getAll() }
            }

            test("range") {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect((0..80).toList()) { crud.getAll().map { it.age!! - 15 } }
                expect((10..80).toList()) { crud.getAll(range = 10..1000).map { it.age!! - 15 } }
                expect((10..20).toList()) { crud.getAll(range = 10..20).map { it.age!! - 15} }
            }

            test("sort") {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect((0..80).toList()) { crud.getAll(null, listOf(Person::age.asc)).map { it.age!! - 15 } }
                expect((0..80).toList().reversed()) { crud.getAll(null, listOf(Person::age.desc)).map { it.age!! - 15 } }
                expect((0..80).toSet()) { crud.getAll(null, listOf(Person::personName.asc)).map { it.age!! - 15 } .toSet() }
            }

            test("count") {
                expect(0) { crud.getCount() }
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(81) { crud.getCount() }
            }

            test("filter") {
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

            test("filter on same fields") {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(0) { crud.getCount(buildCondition { Person::age lt 20 and (Person::age gt 30) })}
                expectList() { crud.getAll(buildCondition { Person::age lt 20 and (Person::age gt 30) })}
            }
        }

        group("getOne") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                expect(p) { crud.getOne(p.id!!.toString()) }
            }
            test("non-existing") {
                expectThrows(IOException::class, "404: No such entity with ID 555") {
                    crud.getOne("555")
                }
            }
            test("malformed id") {
                expectThrows(IOException::class, "Malformed ID: foobar") {
                    crud.getOne("foobar")
                }
            }
        }

        test("create") {
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now().withZeroNanos)
            crud.create(p)
            val actual = db { Person.findAll() }
            p.id = actual[0].id!!
            expectList(p) { actual }
        }

        group("delete") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                crud.delete(p.id!!.toString())
                expectList() { Person.findAll() }
            }
            test("non-existing") {
                // never fail with 404: http://www.tugberkugurlu.com/archive/http-delete-http-200-202-or-204-all-the-time
                crud.delete("555")
            }
            test("invalid id") {
                expectThrows(IOException::class, "404: Malformed ID") {
                    crud.delete("invalid_id")
                }
            }
        }

        group("update") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                p.personName = "Leto Atreides"
                crud.update(p.id!!.toString(), p)
                expectList(p) { Person.findAll() }
            }
            test("non-existing") {
                val p = Person(id = 45, personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
                expectThrows(IOException::class, "404: No such entity with ID 45") {
                    crud.update(p.id!!.toString(), p)
                }
            }
        }
    }
    group("bind client to non-Entity class") {
        group("get all") {
            lateinit var client: CrudClient<Person2>
            beforeEach { client = CrudClient("http://localhost:9876/rest/person/", Person2::class.java) }

            test("simple smoke test") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                val p2 = Person2(p.id, 45, "Duke Leto Atreides")
                val all = client.getAll()
                expectList(p2) { all }
            }

            test("filters") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                val p2 = Person2(p.id, 45, "Duke Leto Atreides")
                expectList() { client.getAll(buildCondition { Person2::personName likeIgnoreCase "baron%" }) }
                expectList(p2) { client.getAll(buildCondition { Person2::personName likeIgnoreCase "duke%" }) }
            }

            test("sorting") {
                expectList() { client.getAll(sortBy = listOf(Person2::personName.asc)) }
            }
        }
    }
})

data class Person2(var id: Long? = null, var age: Int? = null, var personName: String? = null)

val Instant.withZeroNanos: Instant get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
