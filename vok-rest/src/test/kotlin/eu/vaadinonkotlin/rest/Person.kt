package eu.vaadinonkotlin.rest

import com.github.mvysny.dynatest.DynaNodeGroup
import eu.vaadinonkotlin.VaadinOnKotlin
import eu.vaadinonkotlin.vokdb.dataSource
import com.github.vokorm.*
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.JdbiOrm
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.h2.Driver
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.Instant
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A very simple bean representing a database table. The SELECT column -> bean property mapping is done by vok-orm.
 * Notice how Kotlin generates toString, equals, hashcode and all getters/setters automatically (for data classes).
 *
 * See [vok-orm](https://github.com/mvysny/vok-orm) for more details.
 * @property id person ID
 * @property personName person name
 * @property age the person age, 15..100
 * @property dateOfBirth date of birth, optional.
 * @property created when the record was created
 * @property maritalStatus the marital status
 * @property alive whether the person is alive (true) or deceased (false).
 */
data class Person(
        override var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
        @field:ColumnName("name")
        var personName: String? = null,

        @field:NotNull
        @field:Min(15)
        @field:Max(100)
        var age: Int? = null,

        var dateOfBirth: LocalDate? = null,

        @field:NotNull
        var created: Instant? = null,

        @field:NotNull
        var maritalStatus: MaritalStatus? = null,

        @field:NotNull
        var alive: Boolean? = null

) : KEntity<Long> {
    // this brings in tons of useful static methods such as findAll(), findById() etc.
    companion object : Dao<Person, Long>(Person::class.java)

    override fun save(validate: Boolean) {
        if (id == null) {
            if (created == null) created = Instant.now()
        }
        super.save(validate)
    }
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}

fun DynaNodeGroup.usingDb() {
    beforeGroup {
        val config = HikariConfig().apply {
            driverClassName = Driver::class.java.name  // the org.h2.Driver class
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
        JdbiOrm.setDataSource(HikariDataSource(config))
        val flyway: Flyway = Flyway.configure()
                .dataSource(VaadinOnKotlin.dataSource)
                .load()
        flyway.migrate()
    }
    afterGroup { JdbiOrm.destroy() }

    beforeEach { Person.deleteAll() }
    afterEach { Person.deleteAll() }
}
