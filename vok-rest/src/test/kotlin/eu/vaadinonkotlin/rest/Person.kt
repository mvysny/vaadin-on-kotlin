package eu.vaadinonkotlin.rest

import com.github.mvysny.ktormvaadin.ActiveEntity
import com.github.mvysny.ktormvaadin.ActiveKtorm
import com.github.mvysny.ktormvaadin.deleteAll
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.vaadinonkotlin.VaadinOnKotlin
import eu.vaadinonkotlin.vaadin.vokdb.dataSource
import org.flywaydb.core.Flyway
import org.h2.Driver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.ktorm.entity.Entity
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.date
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import java.time.Instant
import java.time.LocalDate

/**
 * Test entity. The DB column for [personName] is `name` to keep the wire format URL-friendly.
 */
interface Person : ActiveEntity<Person> {
    var id: Long?
    var personName: String?
    var age: Int?
    var dateOfBirth: LocalDate?
    var created: Instant?
    var maritalStatus: MaritalStatus?
    var alive: Boolean?

    override val table: Table<Person> get() = Persons

    companion object : Entity.Factory<Person>()
}

object Persons : Table<Person>("Person") {
    val id: Column<Long> = long("id").primaryKey().bindTo { it.id }
    val name: Column<String> = varchar("name").bindTo { it.personName }
    val age: Column<Int> = int("age").bindTo { it.age }
    val dateOfBirth: Column<LocalDate> = date("dateOfBirth").bindTo { it.dateOfBirth }
    val created: Column<Instant> = timestamp("created").bindTo { it.created }
    val maritalStatus: Column<MaritalStatus> = enum<MaritalStatus>("maritalStatus").bindTo { it.maritalStatus }
    val alive: Column<Boolean> = boolean("alive").bindTo { it.alive }
}

enum class MaritalStatus { Single, Married, Divorced, Widowed }

/**
 * Plain data class used by [PersonRestTest] to deserialize JSON returned by the REST endpoint. Mirrors the JSON
 * shape produced by Gson serializing a ktorm [Person] entity (whose Map keys are the entity property names).
 */
data class PersonDto(
    var id: Long? = null,
    var personName: String? = null,
    var age: Int? = null,
    var dateOfBirth: LocalDate? = null,
    var created: Instant? = null,
    var maritalStatus: MaritalStatus? = null,
    var alive: Boolean? = null,
)

abstract class AbstractDbTest {
    companion object {
        @BeforeAll @JvmStatic fun startH2() {
            val config = HikariConfig().apply {
                driverClassName = Driver::class.java.name
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
                username = "sa"
                password = ""
            }
            VaadinOnKotlin.dataSource = HikariDataSource(config)
            Flyway.configure().dataSource(VaadinOnKotlin.dataSource).load().migrate()
        }
        @AfterAll @JvmStatic fun stopH2() {
            // Hikari DataSource is closed indirectly when the JVM exits; nothing to do here.
        }
    }

    @BeforeEach @AfterEach fun clearDb() { Persons.deleteAll() }
}
