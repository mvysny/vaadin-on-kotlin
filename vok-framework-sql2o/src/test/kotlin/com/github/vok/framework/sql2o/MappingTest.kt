package com.github.vok.framework.sql2o

import com.github.vok.framework.VaadinOnKotlin
import org.junit.*
import java.time.LocalDate
import java.util.*
import javax.validation.constraints.NotNull
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
                con.createQuery("create table if not exists Test ( id bigint primary key auto_increment, name varchar not null, age integer not null, dateOfBirth date, created timestamp )").executeUpdate()
            }
        }

        @JvmStatic @AfterClass
        fun closedb() {
            Sql2oVOKPlugin().destroy()
        }
    }

    @Before @After
    fun clearDb() {
        db { Person.deleteAll() }
    }
}

@Table("Test")
data class Person(override var id: Long? = null,
                  var name: String,
                  var age: Int,
                  @Ignore var ignored: String? = null,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,

                  var created: Date? = null

                  ) : Entity<Long> {
    override fun save() {
        if (id == null) {
            // keeping null until https://github.com/aaberg/sql2o/issues/282 is fixed
//            dateOfBirth = LocalDate.of(1990, 1, 12)
            created = java.sql.Timestamp(System.currentTimeMillis())
        }
        super.save()
    }

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
        expect(listOf("Albedo")) { Person.findAll().map { it.name } }
        p.name = "Rubedo"
        p.save()
        expect(listOf("Rubedo")) { Person.findAll().map { it.name } }
        Person(name = "Nigredo", age = 130).save()
        expect(listOf("Rubedo", "Nigredo")) { Person.findAll().map { it.name } }
    }

    @Test
    fun testDelete() {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        p.delete()
        expect(listOf()) { Person.findAll() }
    }
}
