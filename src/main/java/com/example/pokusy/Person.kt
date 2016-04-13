package com.example.pokusy

import javax.persistence.*
import javax.validation.constraints.*

@Entity
data class Person(@field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
                  @field:NotNull @field:Size(min = 1, max = 200) var name: String? = null)
