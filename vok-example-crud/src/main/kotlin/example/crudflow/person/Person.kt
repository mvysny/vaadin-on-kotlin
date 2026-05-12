package example.crudflow.person

import com.github.mvysny.ktormvaadin.ActiveEntity
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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

interface Person : ActiveEntity<Person> {
    var id: Long?

    @get:NotNull
    @get:Size(min = 1, max = 200)
    var name: String?

    @get:NotNull
    @get:Min(15)
    @get:Max(100)
    var age: Int?

    var dateOfBirth: LocalDate?

    @get:NotNull
    var created: Instant?

    @get:NotNull
    var maritalStatus: MaritalStatus?

    @get:NotNull
    var alive: Boolean?

    override val table: Table<Person> get() = Persons

    companion object : Entity.Factory<Person>()
}

enum class MaritalStatus { Single, Married, Divorced, Widowed }

object Persons : Table<Person>("Person") {
    val id: Column<Long> = long("id").primaryKey().bindTo { it.id }
    val name: Column<String> = varchar("name").bindTo { it.name }
    val age: Column<Int> = int("age").bindTo { it.age }
    val dateOfBirth: Column<LocalDate> = date("dateOfBirth").bindTo { it.dateOfBirth }
    val created: Column<Instant> = timestamp("created").bindTo { it.created }
    val maritalStatus: Column<MaritalStatus> = enum<MaritalStatus>("maritalStatus").bindTo { it.maritalStatus }
    val alive: Column<Boolean> = boolean("alive").bindTo { it.alive }
}
