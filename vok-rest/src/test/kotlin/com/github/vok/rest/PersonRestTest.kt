package com.github.vok.rest

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.restclient.*
import com.github.vokorm.db
import com.github.vokorm.findAll
import com.google.gson.GsonBuilder
import io.javalin.Javalin
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.*
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.test.expect

fun Javalin.configureRest(): Javalin {
    val gson = GsonBuilder().create()
    gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud("/rest/person", Person.getCrudHandler(true))
    return this
}

// Demoes Retrofit + annotations client
interface PersonRestClient {
    @GET("helloworld")
    @Throws(IOException::class)
    fun helloWorld(): String

    @GET(".")
    @Throws(IOException::class)
    fun getAll(): List<Person>
}

// Demoes direct access via okhttp
class PersonRestClient2(val baseUrl: String) {
    private val client: OkHttpClient = RetrofitClientVokPlugin.okHttpClient!!
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
    beforeGroup { RetrofitClientVokPlugin().init() }
    afterGroup { RetrofitClientVokPlugin().destroy() }
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingDb()  // to have access to the database.
    usingRestClient()

    test("hello world") {
        val client = createRetrofit("http://localhost:9876/rest/person/").create(PersonRestClient::class.java)
        expect("Hello World") { client.helloWorld() }
        expectList() { client.getAll() }
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        expectList(p) { client.getAll() }
    }

    test("hello world 2") {
        val client = PersonRestClient2("http://localhost:9876/rest/person/")
        expect("Hello World") { client.helloWorld() }
        expectList() { client.getAll() }
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        expectList(p) { client.getAll() }
    }

    group("crud") {
        lateinit var crud: PersonCrudClient
        beforeEach { crud = createRetrofit("http://localhost:9876/rest/person/").create(PersonCrudClient::class.java) }
        test("getAll()") {
            expectList() { crud.getAll() }
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
            p.save()
            expectList(p) { crud.getAll() }
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
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
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
    group("crud2") {
        lateinit var crud: CrudClient<Person>
        beforeEach { crud = CrudClient("http://localhost:9876/rest/person/", Person::class.java) }
        test("getAll()") {
            expectList() { crud.getAll() }
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
            p.save()
            expectList(p) { crud.getAll() }
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
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
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
})

interface PersonCrudClient {
    @GET(".")
    @Throws(IOException::class)
    fun getAll(): List<Person>

    @GET("{id}")
    @Throws(IOException::class)
    fun getOne(@Path("id") id: String): Person

    @POST(".")
    @Throws(IOException::class)
    fun create(@Body entity: Person): Unit?

    @PATCH("{id}")
    @Throws(IOException::class)
    fun update(@Path("id") id: String, @Body entity: Person): Unit?

    @DELETE("{id}")
    @Throws(IOException::class)
    fun delete(@Path("id") id: String): Unit?
}
