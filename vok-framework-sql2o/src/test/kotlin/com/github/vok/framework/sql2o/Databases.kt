package com.github.vok.framework.sql2o

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.vokorm.*
import java.time.LocalDate
import java.util.*

@Table("Test")
data class Person(override var id: Long? = null,
                  @As("name") var personName: String,
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

fun DynaNodeGroup.usingH2Database() {
    beforeGroup {
        VokOrm.dataSourceConfig.apply {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
        VokOrm.init()
        db {
            con.createQuery(
                """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                alive boolean,
                maritalStatus varchar
                 )"""
            ).executeUpdate()
        }
    }

    afterGroup { VokOrm.destroy() }

    fun clearDb() = Person.deleteAll()
    beforeEach { clearDb() }
    afterEach { clearDb() }
}
