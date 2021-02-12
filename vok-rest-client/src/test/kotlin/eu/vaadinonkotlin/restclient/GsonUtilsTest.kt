package eu.vaadinonkotlin.restclient

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.google.gson.Gson
import kotlin.test.expect

/**
 * @author Martin Vysny <mavi@vaadin.com>
 */
class GsonUtilsTest : DynaTest({
    lateinit var gson: Gson
    beforeEach { gson = Gson() }

    group("pojo") {
        test("empty") {
            expect(Person()) {
                gson.fromJson("{}", Person::class.java)
            }
        }
        test("simple") {
            expect(Person("Foo", "Bar", 25)) {
                gson.fromJson("""{"name":"Foo", "surname":"Bar", "age": 25}""", Person::class.java)
            }
        }
    }

    group("jsonArray") {
        test("empty") {
            expectList() { gson.fromJsonArray("""[]""", Person::class.java) }
        }
        test("simple") {
            val json = """[{"name":"John", "surname":"Doe"}]"""
            expectList(Person("John", "Doe")) { gson.fromJsonArray(json, Person::class.java) }
        }
    }

    group("jsonMap") {
        test("empty") {
            val json = """{}"""
            expect(mapOf()) { gson.fromJsonMap(json, Person::class.java) }
        }
        test("simple") {
            val json = """{"director": {"name":"John", "surname":"Doe"}}"""
            expect(mapOf("director" to Person("John", "Doe"))) {
                gson.fromJsonMap(json, Person::class.java)
            }
        }
    }
})
