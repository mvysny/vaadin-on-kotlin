package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.ktormvaadin.ActiveEntity
import com.github.mvysny.ktormvaadin.ActiveKtorm
import com.github.mvysny.ktormvaadin.db
import com.github.mvysny.ktormvaadin.deleteAll
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.Driver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.ktorm.database.Database
import org.ktorm.entity.Entity
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

public interface Person : ActiveEntity<Person> {
    public var id: Long?
    public var personName: String
    public var age: Int
    public var dateOfBirth: LocalDate?
    public var created: Instant?
    public var alive: Boolean?
    public var maritalStatus: MaritalStatus?
    override val table: org.ktorm.schema.Table<Person> get() = Persons

    public companion object : Entity.Factory<Person>()
}

public enum class MaritalStatus { Single, Married, Divorced, Widowed }

public object Persons : Table<Person>("Test") {
    public val id: org.ktorm.schema.Column<Long> = long("id").primaryKey().bindTo { it.id }
    public val name: org.ktorm.schema.Column<String> = varchar("name").bindTo { it.personName }
    public val age: org.ktorm.schema.Column<Int> = int("age").bindTo { it.age }
    public val dateOfBirth: org.ktorm.schema.Column<LocalDate> = date("dateOfBirth").bindTo { it.dateOfBirth }
    public val created: org.ktorm.schema.Column<Instant> = timestamp("created").bindTo { it.created }
    public val alive: org.ktorm.schema.Column<Boolean> = boolean("alive").bindTo { it.alive }
    public val maritalStatus: org.ktorm.schema.Column<MaritalStatus> = enum<MaritalStatus>("maritalStatus").bindTo { it.maritalStatus }
}

abstract class AbstractDbTest {
    companion object {
        private lateinit var ds: HikariDataSource

        @BeforeAll
        @JvmStatic
        fun setupDb() {
            val config = HikariConfig().apply {
                driverClassName = Driver::class.java.name
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
                username = "sa"
                password = ""
            }
            ds = HikariDataSource(config)
            ActiveKtorm.database = Database.connect(ds)
            db {
                transaction.connection.createStatement().execute(
                    """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                alive boolean,
                maritalStatus varchar
                 )"""
                )
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownDb() {
            ds.close()
        }
    }
    @BeforeEach @AfterEach
    fun clearDb() { Persons.deleteAll() }
}

abstract class AbstractVaadinDbTest : AbstractDbTest() {
    @BeforeEach fun fakeVaadin() { MockVaadin.setup() }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }
}
