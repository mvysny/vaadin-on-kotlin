package com.github.vok.framework.sql2o

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.vok.framework.VaadinOnKotlin
import com.github.vokorm.*
import java.time.LocalDate
import java.util.*

fun DynaNodeGroup.usingDatabase() {

    beforeGroup {
        VaadinOnKotlin.dataSourceConfig.apply {
            minimumIdle = 0
            maximumPoolSize = 30
            this.jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            this.username = "sa"
            this.password = ""
        }
        VokOrmPlugin().init()
        db {
            con.createQuery("""create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                alive boolean,
                maritalStatus varchar
                 )""").executeUpdate()
        }
    }

    afterGroup {
        VokOrmPlugin().destroy()
    }

    fun clearDatabase() = Person.deleteAll()
    beforeEach { clearDatabase() }
    afterEach { clearDatabase() }
}

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
    override fun save() {
        if (id == null) {
            created = java.sql.Timestamp(System.currentTimeMillis())
        }
        super.save()
    }

    companion object : Dao<Person>
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}
