package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.vokorm.*
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.JdbiOrm
import com.gitlab.mvysny.jdbiorm.Table
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.Driver
import org.jdbi.v3.core.annotation.JdbiProperty
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*

@Table("Test")
data class Person(override var id: Long? = null,
                  @field:ColumnName("name") var personName: String = "",
                  var age: Int = -1,
                  @field:JdbiProperty(map = false) var ignored: String? = null,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,
                  var created: Date? = null,
                  var alive: Boolean? = null,
                  var maritalStatus: MaritalStatus? = null
) : KEntity<Long> {
    override fun save(validate: Boolean) {
        if (id == null) {
            created = java.sql.Timestamp(System.currentTimeMillis())
        }
        super.save(validate)
    }

    companion object : Dao<Person, Long>(Person::class.java)
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}

abstract class AbstractDbTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDb() {
            val config = HikariConfig().apply {
                driverClassName = Driver::class.java.name  // the org.h2.Driver class
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                username = "sa"
                password = ""
            }
            JdbiOrm.setDataSource(HikariDataSource(config))
            db {
                handle.createUpdate(
                    """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                alive boolean,
                maritalStatus varchar
                 )"""
                ).execute()
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownDb() {
            JdbiOrm.destroy()
        }
    }
    @BeforeEach @AfterEach
    fun clearDb() = Person.deleteAll()
}

abstract class AbstractVaadinDbTest : AbstractDbTest() {
    @BeforeEach fun fakeVaadin() { MockVaadin.setup() }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }
}
