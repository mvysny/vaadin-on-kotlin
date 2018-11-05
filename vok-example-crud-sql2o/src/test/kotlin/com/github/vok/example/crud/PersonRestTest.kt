package com.github.vok.example.crud

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.example.crud.personeditor.MaritalStatus
import com.github.vok.example.crud.personeditor.Person
import com.github.vok.example.crud.personeditor.usingApp
import com.github.vokorm.Entity
import com.github.vokorm.db
import com.github.vokorm.findAll
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.javalin.Javalin
import khttp.responses.Response
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.test.expect

fun Response.checkOk(): Response {
    if (statusCode !in 200..299) throw IOException("$statusCode: $text ($url)")
    return this
}

class PersonRestClient2(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    fun helloWorld(): String = khttp.get("$baseUrl/person/helloworld").checkOk().text
    val personCrud: CrudClient<Person> get() = CrudClient("$baseUrl/person", Person::class.java)
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingApp()  // to bootstrap the app to have access to the database.

    lateinit var client: PersonRestClient2
    beforeEach { client = PersonRestClient2("http://localhost:9876/rest") }

    test("hello world") {
        expect("Hello World") { client.helloWorld() }
    }

    group("crud") {
        test("getAll()") {
            expectList() { client.personCrud.getAll() }
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
            p.save()
            expectList(p) { client.personCrud.getAll() }
        }

        group("getOne") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                expect(p) { client.personCrud.getOne(p.id!!.toString()) }
            }
            test("non-existing") {
                expectThrows(IOException::class, "404: No such entity with ID 555") {
                    client.personCrud.getOne("555")
                }
            }
            test("malformed id") {
                expectThrows(IOException::class, "Malformed ID: foobar") {
                    client.personCrud.getOne("foobar")
                }
            }
        }

        test("create") {
            val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
            client.personCrud.create(p)
            val actual = db { Person.findAll() }
            p.id = actual[0].id!!
            expectList(p) { actual }
        }

        group("delete") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                client.personCrud.delete(p.id!!.toString())
                expectList() { Person.findAll() }
            }
            test("non-existing") {
                // never fail with 404: http://www.tugberkugurlu.com/archive/http-delete-http-200-202-or-204-all-the-time
                client.personCrud.delete("555")
            }
            test("invalid id") {
                expectThrows(IOException::class, "404: Malformed ID") {
                    client.personCrud.delete("invalid_id")
                }
            }
        }

        group("update") {
            test("simple") {
                val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
                p.save()
                p.personName = "Leto Atreides"
                client.personCrud.update(p)
                expectList(p) { Person.findAll() }
            }
            test("non-existing") {
                val p = Person(id = 45, personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false, created = Instant.now())
                expectThrows(IOException::class, "404: No such entity with ID 45") {
                    client.personCrud.update(p)
                }
            }
        }
    }
})

class CrudClient<T: Entity<*>>(val baseUrl: String, val beanClass: Class<T>, val gson: Gson = GsonBuilder().create()) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }

    fun getAll(): List<T> {
        val text = khttp.get(baseUrl).checkOk().text
        val type = TypeToken.getParameterized(List::class.java, beanClass).type
        return gson.fromJson<List<T>>(text, type)
    }

    fun getOne(id: String): T {
        val text = khttp.get("$baseUrl/$id").checkOk().text
        return gson.fromJson(text, beanClass)
    }

    fun create(entity: T) {
        khttp.post(baseUrl, data = gson.toJson(entity)).checkOk()
    }

    fun update(entity: T) {
        require(entity.id != null) { "entity has null ID" }
        khttp.patch("$baseUrl/${entity.id!!}", data = gson.toJson(entity)).checkOk()
    }

    fun delete(id: String) {
        khttp.delete("$baseUrl/$id").checkOk()
    }
}
