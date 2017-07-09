package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.Person
import org.junit.Test
import kotlin.test.expect

class FiltersTest {
    @Test
    fun testToSQL92() {
        expect("age = :25") { sql { Person::age eq 25 } }
        expect("(age >= :25 and age <= :50)") { sql { Person::age between 25..50 } }
    }

    private fun sql(block: SqlWhereBuilder<Person>.()->Filter<Person>): String {
        val filter: Filter<Person> = block(SqlWhereBuilder())
        return unmangleParameterNames(filter.toSQL92(), filter.getSQL92Parameters())
    }

    private fun unmangleParameterNames(sql: String, params: Map<String, Any?>): String {
        var sql = sql
        params.entries.forEach { (key, value) -> sql = sql.replace(":$key", ":$value") }
        return sql
    }
}
