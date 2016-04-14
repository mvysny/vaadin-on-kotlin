package com.example.pokusy

import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A very simple JPA entity. Notice how Kotlin generates toString, equals, hashcode and all getters/setters automatically (for data classes).
 * @property id person ID
 * @property name person name
 */
@Entity
data class Person(@field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
                  @field:NotNull @field:Size(min = 1, max = 200) var name: String? = null) : Serializable
