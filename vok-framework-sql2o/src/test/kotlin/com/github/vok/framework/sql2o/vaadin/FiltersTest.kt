package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.Person
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import org.junit.Test
import kotlin.streams.toList
import kotlin.test.expect

class FiltersTest {
    @Test
    fun testToSQL92() {
        expect("age = :25") { sql { Person::age eq 25 } }
        expect("(age >= :25 and age <= :50)") { sql { Person::age between 25..50 } }
    }

    @Test
    fun testInMemoryFilters() {
        val list = (15..90).map { Person(name = "test$it", age = it) }
        val ds = ListDataProvider<Person>(list).and { Person::age between 30..60 }
        expect(31) { ds.size(Query()) }
        expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
    }

    @Test
    fun testLikeFilterInMemory() {
        expect(true) { LikeFilter<Person>("name", "A").test(Person(name = "kari", age = 35)) }
        expect(true) { LikeFilter<Person>("name", " a ").test(Person(name = "kari", age = 35)) }
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
