package com.github.vok.framework.sql2o

import com.github.vok.framework.VaadinOnKotlin
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.expect

class MappingTest {
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
                con.createQuery("create table Test ( id bigint primary key auto_increment, name varchar not null, age integer not null )").executeUpdate()
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

    @Test
    fun testFindAll() {
        expect(listOf()) { db { con.findAll<Person>() } }
        val p = Person(name = "Zaphod", age = 42, ignored2 = Object())
        p.save()
        expect(true) { p.id != null }
        p.ignored2 = null
        expect(listOf(p)) { db { con.findAll<Person>() } }
    }
}

@Table("Test")
data class Person(override var id: Long? = null, var name: String, var age: Int, @Ignore var ignored: String? = null, @Transient var ignored2: Any? = null) : Entity<Long>
