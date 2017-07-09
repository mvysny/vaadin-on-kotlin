package com.github.vok.example.crud.personeditor

import com.github.vok.framework.sql2o.Dao
import com.github.vok.framework.sql2o.Entity
import java.time.LocalDate
import java.util.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A very simple JPA entity. Notice how Kotlin generates toString, equals, hashcode and all getters/setters automatically (for data classes).
 * @property id person ID
 * @property name person name
 */
data class Person(
        override var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
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

) : Entity<Long> {
    // this brings in tons of useful static methods such as findAll(), findById() etc.
    companion object : Dao<Person>
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}
