package com.github.vok.framework.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.google.gson.Gson
import java.time.LocalDate
import kotlin.test.expect

class MappingTest : DynaTest({
    withDatabase {
        test("FindAll") {
            expect(listOf()) { Person.findAll() }
            val p = Person(name = "Zaphod", age = 42, ignored2 = Object())
            p.save()
            expect(true) { p.id != null }
            p.ignored2 = null
            expect(listOf(p)) { Person.findAll() }
        }

        test("Save") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(listOf("Albedo")) { Person.findAll().map { it.name } }
            p.name = "Rubedo"
            p.save()
            expect(listOf("Rubedo")) { Person.findAll().map { it.name } }
            Person(name = "Nigredo", age = 130).save()
            expect(listOf("Rubedo", "Nigredo")) { Person.findAll().map { it.name } }
        }

        test("delete") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            p.delete()
            expect(listOf()) { Person.findAll() }
        }

        test("SaveEnum") {
            val p = Person(name = "Zaphod", age = 42, maritalStatus = MaritalStatus.Divorced)
            p.save()
            class Foo(var maritalStatus: String? = null)
            expect(listOf("Divorced")) { db { con.createQuery("select maritalStatus from Test").executeAndFetch(Foo::class.java).map { it.maritalStatus } } }
            expect(p) { db { Person.findAll()[0] } }
        }

        test("SaveLocalDate") {
            val p = Person(name = "Zaphod", age = 42, dateOfBirth = LocalDate.of(1990, 1, 14))
            p.save()
            expect(LocalDate.of(1990, 1, 14)) { db { Person.findAll()[0].dateOfBirth!! }}
        }

        test("JsonSerializationIgnoresMeta") {
            expect("""{"name":"Zaphod","age":42}""") { Gson().toJson(Person(name = "Zaphod", age = 42)) }
        }

        test("Meta") {
            val meta = Person.meta
            expect("Test") { meta.databaseTableName }  // sicen Person is annotated with @Entity("Test")
            expect("id") { meta.idDbname }
            expect(Person::class.java) { meta.entity }
            expect(setOf("id", "name", "age", "dateOfBirth", "created", "alive", "maritalStatus")) { meta.persistedFieldDbNames }
        }
    }
})
