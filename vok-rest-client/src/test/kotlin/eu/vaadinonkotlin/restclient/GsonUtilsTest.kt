package eu.vaadinonkotlin.restclient

import com.google.gson.Gson
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

/**
 * @author Martin Vysny <mavi@vaadin.com>
 */
class GsonUtilsTest {
    private val gson = Gson()

    @Nested inner class pojo() {
        @Test fun empty() {
            expect(Person()) {
                gson.fromJson("{}", Person::class.java)
            }
        }
        @Test fun simple() {
            expect(Person("Foo", "Bar", 25)) {
                gson.fromJson("""{"name":"Foo", "surname":"Bar", "age": 25}""", Person::class.java)
            }
        }
    }

    @Nested inner class jsonArray() {
        @Test fun empty() {
            expect(listOf()) { gson.fromJsonArray("""[]""", Person::class.java) }
        }
        @Test fun simple() {
            val json = """[{"name":"John", "surname":"Doe"}]"""
            expect(listOf(Person("John", "Doe"))) { gson.fromJsonArray(json, Person::class.java) }
        }
    }

    @Nested inner class jsonMap() {
        @Test fun empty() {
            val json = """{}"""
            expect(mapOf()) { gson.fromJsonMap(json, Person::class.java) }
        }
        @Test fun simple() {
            val json = """{"director": {"name":"John", "surname":"Doe"}}"""
            expect(mapOf("director" to Person("John", "Doe"))) {
                gson.fromJsonMap(json, Person::class.java)
            }
        }
    }
}
