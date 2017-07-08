package com.github.vok.framework.sql2o

import com.github.vok.framework.VaadinOnKotlin
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.expect

abstract class AbstractDbTest {
    companion object {
        @JvmStatic @BeforeClass
        fun initdb() {
            VaadinOnKotlin.dataSourceConfig.apply {
                minimumIdle = 0
                maximumPoolSize = 30
                this.jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                this.username = "sa"
                this.password = ""
            }
            Sql2oVOKPlugin().init()
            db {
                con.createQuery("create table if not exists Test ( id bigint primary key auto_increment, name varchar not null, age integer not null )").executeUpdate()
            }
        }

        @JvmStatic @AfterClass
        fun closedb() {
            Sql2oVOKPlugin().destroy()
        }
    }

    @Before
    fun clearDb() {
        db { con.deleteAll<Person>() }
    }
}

@Table("Test")
data class Person(override var id: Long? = null, var name: String, var age: Int, @Ignore var ignored: String? = null, @Transient var ignored2: Any? = null) : Entity<Long> {
    companion object : Dao<Person>
}

class MappingTest : AbstractDbTest() {
    @Test
    fun testFindAll() {
        expect(listOf()) { Person.findAll() }
        val p = Person(name = "Zaphod", age = 42, ignored2 = Object())
        p.save()
        expect(true) { p.id != null }
        p.ignored2 = null
        expect(listOf(p)) { Person.findAll() }
    }

    @Test
    fun testSave() {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        expect(listOf("Albedo")) { db { con.findAll<Person>().map { it.name } } }
        p.name = "Rubedo"
        p.save()
        expect(listOf("Rubedo")) { db { con.findAll<Person>().map { it.name } } }
        Person(name = "Nigredo", age = 130).save()
        expect(listOf("Rubedo", "Nigredo")) { db { con.findAll<Person>().map { it.name } } }
    }

    @Test
    fun testDelete() {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        p.delete()
        expect(listOf()) { Person.findAll() }
    }
}
