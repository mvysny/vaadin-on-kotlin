package com.github.vok.framework.sql2o

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

/**
 * @author mavi
 */
class DaoTest : DynaTest({

    withDatabase {
        test("FindById") {
            expect(null) { Person.findById(25) }
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p) { Person.findById(p.id!!) }
        }

        test("GetById") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p) { Person[p.id!!] }
        }

        test("Count") {
            expect(0) { Person.count() }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
        }

        test("DeleteAll") {
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
            Person.deleteAll()
            expect(0) { Person.count() }
        }

        test("DeleteById") {
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
            Person.deleteById(Person.findAll().filter { it.name == "Albedo" } .first().id!!)
            expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
        }

        test("DeleteBy") {
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            Person.deleteBy { "name = :name"("name" to "Albedo") }  // raw sql where
            expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
            Person.deleteBy { Person::name eq "Rubedo" }  // fancy type-safe criteria
            expect(listOf("Nigredo")) { Person.findAll().map { it.name } }
        }
    }
})
