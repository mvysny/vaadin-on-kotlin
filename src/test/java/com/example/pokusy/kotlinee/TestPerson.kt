package com.example.pokusy.kotlinee

import java.io.Serializable
import javax.persistence.Entity

@Entity
data class TestPerson(
        @field:javax.persistence.Id
        @field:javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
        var id: Long? = null,

        @field:javax.validation.constraints.NotNull
        @field:javax.validation.constraints.Size(min = 1, max = 200)
        var name: String? = null,

        @field:javax.validation.constraints.NotNull
        @field:javax.validation.constraints.Min(15)
        @field:javax.validation.constraints.Max(100)
        var age: Int? = null
) : Serializable
