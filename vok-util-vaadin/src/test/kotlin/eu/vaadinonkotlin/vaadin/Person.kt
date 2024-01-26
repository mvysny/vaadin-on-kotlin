package eu.vaadinonkotlin.vaadin

import java.time.LocalDate
import java.util.*

data class Person(var id: Long? = null,
                  var personName: String,
                  var age: Int,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,
                  var created: Date? = null,
                  var alive: Boolean? = null)
