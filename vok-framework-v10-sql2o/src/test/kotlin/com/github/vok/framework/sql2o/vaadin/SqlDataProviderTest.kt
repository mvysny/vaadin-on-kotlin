package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.AbstractDbTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.databaseTableName
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import com.vaadin.data.provider.SortDirection
import org.junit.Test
import kotlin.streams.toList
import kotlin.test.expect

class SqlDataProviderTest : AbstractDbTest() {
    data class SelectResult(val id: Long, val name: String)

    private val dp = SqlDataProvider(SelectResult::class.java,
            """select p.id as id, p.name as name from ${Person::class.java.databaseTableName} p where age > :age {{WHERE}} order by null{{ORDER}} {{PAGING}}""",
            mapOf("age" to 25),
            { it.id })
    @Test
    fun testEmptyDataProvider() {
        expect(0) { dp.size(Query()) }
        expect(listOf()) { dp.fetch(Query()).toList() }
        val f = filter<SelectResult> { SelectResult::id gt 2 }
        expect(0) { dp.size(Query(f))}
        expect(listOf()) { dp.fetch(Query(f)).toList() }
        expect(0) { dp.size(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, null))}
        expect(listOf()) { dp.fetch(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, null)).toList() }
        expect(0) { dp.size(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f))}
        expect(listOf()) { dp.fetch(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f))}
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, f))}
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, null))}
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, null)).toList() }
    }
}
