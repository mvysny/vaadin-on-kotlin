package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.databaseTableName
import com.github.vok.framework.sql2o.usingDatabase
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import com.vaadin.shared.data.sort.SortDirection
import kotlin.streams.toList
import kotlin.test.expect

class SqlDataProviderTest : DynaTest({
    data class SelectResult(val id: Long, val name: String)

    val dp = SqlDataProvider(SelectResult::class.java,
        """select p.id as id, p.name as name from ${Person::class.java.databaseTableName} p where age > :age {{WHERE}} order by null{{ORDER}} {{PAGING}}""",
        mapOf("age" to 25),
        { it.id })

    usingDatabase()

    test("EmptyDataProvider") {
        expect(0) { dp.size(Query()) }
        expect(listOf()) { dp.fetch(Query()).toList() }
        val f = filter<SelectResult> { SelectResult::id gt 2 }
        expect(0) { dp.size(Query(f)) }
        expect(listOf()) { dp.fetch(Query(f)).toList() }
        expect(0) { dp.size(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, null)) }
        expect(listOf()) { dp.fetch(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, null)).toList() }
        expect(0) { dp.size(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)) }
        expect(listOf()) { dp.fetch(Query(0, 20, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)) }
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(QuerySortOrder("name", SortDirection.ASCENDING)), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, f)) }
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, f)).toList() }
        expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, null)) }
        expect(listOf()) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, null)).toList() }
    }
})
