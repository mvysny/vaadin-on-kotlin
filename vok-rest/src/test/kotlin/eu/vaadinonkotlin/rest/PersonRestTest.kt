package eu.vaadinonkotlin.rest

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import com.github.mvysny.vokdataloader.FullTextFilter
import com.github.mvysny.vokdataloader.SortClause
import com.github.mvysny.vokdataloader.asc
import com.github.mvysny.vokdataloader.buildFilter
import com.github.vokorm.db
import com.google.gson.GsonBuilder
import eu.vaadinonkotlin.restclient.*
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import kotlin.test.expect

fun Javalin.configureRest(): Javalin {
    val gson = GsonBuilder().create()
    gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud2("/rest/person", Person.getCrudHandler(true))
    return this
}

// Demoes direct access via okhttp
class PersonRestClient(val baseUrl: String) {
    private val client: OkHttpClient = OkHttpClientVokPlugin.okHttpClient!!
    fun helloWorld(): String {
        val request = Request.Builder().url("${baseUrl}helloworld").build()
        return client.exec(request) { response -> response.string() }
    }
    fun getAll(): List<Person> {
        val request = Request.Builder().url(baseUrl).build()
        return client.exec(request) { response -> response.jsonArray(Person::class.java) }
    }
}

fun DynaNodeGroup.usingRestClient() {
    beforeGroup { OkHttpClientVokPlugin().init() }
    afterGroup { OkHttpClientVokPlugin().destroy() }
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create { it.showJavalinBanner = false }
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingDb()  // to have access to the database.
    usingRestClient()

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
                expect((10..80).toList()) { crud.getAll(range = 10L..1000L).map { it.age!! - 15 } }
                expect((10..20).toList()) { crud.getAll(range = 10L..20L).map { it.age!! - 15} }
            }

            test("sort") {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect((0..80).toList()) { crud.getAll(null, listOf(SortClause("age", true))).map { it.age!! - 15 } }
                expect((0..80).toList().reversed()) { crud.getAll(null, listOf(SortClause("age", false))).map { it.age!! - 15 } }
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
                expect(0) { crud.getCount(buildFilter { Person::age ge 130 })}
                expect(5) { crud.getCount(buildFilter { Person::age lt 20 })}
                expect(81) { crud.getCount(buildFilter { Person::personName eq "Duke Leto Atreides" })}
                expect(0) { crud.getCount(buildFilter { Person::personName startsWith "duke " })}
                expect(81) { crud.getCount(buildFilter { Person::personName istartsWith "duke " })}
                expect((0..4).toList()) { crud.getAll(buildFilter { Person::age lt 20 }, listOf(SortClause("age", true))).map { it.age!! - 15 } }
                expect(81) { crud.getCount(buildFilter { Person::dateOfBirth eq LocalDate.of(1980, 5, 1) })}
                expect(0) { crud.getCount(FullTextFilter("personName", "duke")) }
            }

            test("filter on same fields") {
                (0..80).forEach {
                    Person(personName = "Duke Leto Atreides", age = it + 15, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
                }
                expect(0) { crud.getCount(buildFilter { Person::age lt 20 and (Person::age gt 30) })}
                expectList() { crud.getAll(buildFilter { Person::age lt 20 and (Person::age gt 30) })}
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
                expectList() { client.getAll(filter = buildFilter { Person2::personName istartsWith "baron" }) }
                expectList(p2) { client.getAll(filter = buildFilter { Person2::personName istartsWith "duke" }) }
            }

            test("sorting") {
                expectList() { client.getAll(sortBy = listOf(Person2::personName.asc)) }
            }
        }
    }
})

data class Person2(var id: Long? = null, var age: Int? = null, var personName: String? = null)

val Instant.withZeroNanos: Instant get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
