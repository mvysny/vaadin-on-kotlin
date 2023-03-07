package example.crudflow.person

import com.github.vokorm.KEntity
import com.gitlab.mvysny.jdbiorm.Dao
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate
import java.util.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * A very simple bean representing a database table. The SELECT column -> bean property mapping is done by vok-orm.
 * Notice how Kotlin generates toString, equals, hashcode and all getters/setters automatically (for data classes).
 *
 * See [vok-orm](https://github.com/mvysny/vok-orm) for more details.
 * @property id person ID
 * @property name person name
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
        @field:ColumnName("PERSON_NAME")
        var name: String? = null,

        @field:NotNull
        @field:Min(15)
        @field:Max(100)
        var age: Int? = null,

        var dateOfBirth: LocalDate? = null,

        @field:NotNull
        var created: Date? = null,

        @field:NotNull
        var maritalStatus: MaritalStatus? = null,

        @field:NotNull
        var alive: Boolean? = null

) : KEntity<Long> {
    // this brings in tons of useful static methods such as findAll(), findById() etc.
    companion object : Dao<Person, Long>(Person::class.java)

    override fun save(validate: Boolean) {
        if (created == null) created = Date()
        super.save(validate)
    }
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}
