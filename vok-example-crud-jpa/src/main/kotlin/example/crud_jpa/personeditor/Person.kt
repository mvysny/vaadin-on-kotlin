package example.crud_jpa.personeditor

import java.io.Serializable
import java.time.LocalDate
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A very simple JPA entity. Notice how Kotlin generates toString, equals, hashcode and all getters/setters automatically (for data classes).
 * @property id person ID
 * @property name person name
 */
@Entity
data class Person(
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
        var name: String? = null,

        @field:NotNull
        @field:Min(15)
        @field:Max(100)
        var age: Int? = null,

        var dateOfBirth: LocalDate? = null,

        var created: Date? = null,

        @field:NotNull
        @field:Enumerated(EnumType.STRING)
        var maritalStatus: MaritalStatus? = null,

        @field:NotNull
        var alive: Boolean? = null

) : Serializable

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}
