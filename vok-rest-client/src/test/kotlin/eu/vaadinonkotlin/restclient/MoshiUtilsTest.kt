package eu.vaadinonkotlin.restclient

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.test.expect

/**
 * @author Martin Vysny <mavi@vaadin.com>
 */
class MoshiUtilsTest : DynaTest({
    lateinit var moshi: Moshi
    beforeEach { moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }

    group("pojo") {
        test("empty") {
            expect(Person()) {
                moshi.adapter(Person::class.java).fromJson("{}")
            }
        }
        test("simple") {
            expect(Person("Foo", "Bar", 25)) {
                moshi.adapter(Person::class.java).fromJson("""{"name":"Foo", "surname":"Bar", "age": 25}""")
            }
        }
    }

    group("jsonArray") {
        test("empty") {
            expectList() { moshi.fromJsonArray("""[]""", Person::class.java) }
        }
        test("simple") {
            val json = """[{"name":"John", "surname":"Doe"}]"""
            expectList(Person("John", "Doe")) { moshi.fromJsonArray(json, Person::class.java) }
        }
    }

    group("jsonMap") {
        test("empty") {
            val json = """{}"""
            expect(mapOf()) { moshi.fromJsonMap(json, Person::class.java) }
        }
        test("simple") {
            val json = """{"director": {"name":"John", "surname":"Doe"}}"""
            expect(mapOf("director" to Person("John", "Doe"))) {
                moshi.fromJsonMap(json, Person::class.java)
            }
        }
    }
})
