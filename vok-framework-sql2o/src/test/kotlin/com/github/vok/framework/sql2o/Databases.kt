package com.github.vok.framework.sql2o

import com.github.vokorm.Dao
import com.github.vokorm.Entity
import com.github.vokorm.Ignore
import com.github.vokorm.Table
import java.time.LocalDate
import java.util.*

@Table("Test")
data class Person(override var id: Long? = null,
                  var name: String,
                  var age: Int,
                  @Ignore var ignored: String? = null,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,
                  var created: Date? = null,
                  var alive: Boolean? = null,
                  var maritalStatus: MaritalStatus? = null

) : Entity<Long> {
    override fun save(validate: Boolean) {
        if (id == null) {
            created = java.sql.Timestamp(System.currentTimeMillis())
        }
        super.save(validate)
    }

    companion object : Dao<Person>
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}
