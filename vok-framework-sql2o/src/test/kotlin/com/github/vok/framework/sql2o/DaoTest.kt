package com.github.vok.framework.sql2o

import org.junit.Test
import kotlin.test.expect

/**
 * @author mavi
 */
class DaoTest : AbstractDbTest() {

    @Test
    fun testFindById() {
        expect(null) { Person.findById(25) }
        val p = Person(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person.findById(p.id!!) }
    }

    @Test
    fun testGetById() {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person[p.id!!] }
    }

    @Test
    fun testCount() {
        expect(0) { Person.count() }
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        expect(3) { Person.count() }
    }

    @Test
    fun testDeleteAll() {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        expect(3) { Person.count() }
        Person.deleteAll()
        expect(0) { Person.count() }
    }

    @Test
    fun testDeleteById() {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        expect(3) { Person.count() }
        Person.deleteById(Person.findAll().filter { it.name == "Albedo" } .first().id!!)
        expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
    }
}
