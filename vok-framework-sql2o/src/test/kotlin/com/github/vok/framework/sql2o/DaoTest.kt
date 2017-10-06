package com.github.vok.framework.sql2o

import com.github.vok.framework.sql2o.vaadin.deleteBy
import org.junit.Test
import kotlin.test.expect

class DaoTest : AbstractDbTest() {
    @Test
    fun testDeleteBy() {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        Person.deleteBy { "name = :name"("name" to "Albedo") }  // raw sql where
        expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
        Person.deleteBy { Person::name eq "Rubedo" }  // fancy type-safe criteria
        expect(listOf("Nigredo")) { Person.findAll().map { it.name } }
    }
}
