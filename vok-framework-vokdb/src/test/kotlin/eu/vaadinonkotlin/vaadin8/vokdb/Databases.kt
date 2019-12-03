package eu.vaadinonkotlin.vaadin8.vokdb

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.vokorm.*
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.Ignore
import com.gitlab.mvysny.jdbiorm.JdbiOrm
import com.gitlab.mvysny.jdbiorm.Table
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.Driver
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate
import java.util.*

@Table("Test")
data class Person(override var id: Long? = null,
                  @field:ColumnName("name") var personName: String = "",
                  var age: Int = -1,
                  @field:Ignore var ignored: String? = null,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,
                  var created: Date? = null,
                  var alive: Boolean? = null,
                  var maritalStatus: MaritalStatus? = null

) : KEntity<Long> {
    override fun save(validate: Boolean) {
        if (id == null) {
            created = java.sql.Timestamp(System.currentTimeMillis())
        }
        super.save(validate)
    }

    companion object : Dao<Person, Long>(Person::class.java)
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}

fun DynaNodeGroup.usingH2Database() {
    beforeGroup {
        val config = HikariConfig().apply {
            driverClassName = Driver::class.java.name  // the org.h2.Driver class
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
        JdbiOrm.setDataSource(HikariDataSource(config))
        db {
            handle.createUpdate(
                """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                alive boolean,
                maritalStatus varchar
                 )"""
            ).execute()
        }
    }

    afterGroup { JdbiOrm.destroy() }

    fun clearDb() = Person.deleteAll()
    beforeEach { clearDb() }
    afterEach { clearDb() }
}
