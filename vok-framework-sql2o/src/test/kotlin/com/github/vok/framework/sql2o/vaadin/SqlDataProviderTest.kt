package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.withAllDatabases
import com.github.vokorm.databaseTableName
import com.github.vokorm.db
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import com.vaadin.shared.data.sort.SortDirection
import kotlin.streams.toList
import kotlin.test.expect

class SqlDataProviderTest : DynaTest({
    data class SelectResult(val id: Long, val name: String)

    withAllDatabases {
        val nameAsc: List<QuerySortOrder> = listOf(QuerySortOrder("name", SortDirection.ASCENDING))
        val nameDesc: List<QuerySortOrder> = listOf(QuerySortOrder("name", SortDirection.DESCENDING))

        test("EmptyDataProvider") {
            val dp = SqlDataProvider(SelectResult::class.java,
                """select p.id as id, p.name as name from ${Person::class.java.databaseTableName} p where 1=1 {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}""",
                idMapper = { it.id })

            expect(0) { dp.size(Query()) }
            expectList() { dp.fetch(Query()).toList() }
            val f = filter<SelectResult> { SelectResult::id gt 2 }
            expect(0) { dp.size(Query(f)) }
            expectList() { dp.fetch(Query(f)).toList() }
            expect(0) { dp.size(Query(0, 20, nameAsc, null, null)) }
            expectList() { dp.fetch(Query(0, 20, nameAsc, null, null)).toList() }
            expect(0) { dp.size(Query(0, 20, nameAsc, null, f)) }
            expectList() { dp.fetch(Query(0, 20, nameAsc, null, f)).toList() }
            expect(0) { dp.size(Query(0, Int.MAX_VALUE, nameAsc, null, f)) }
            expectList() { dp.fetch(Query(0, Int.MAX_VALUE, nameAsc, null, f)).toList() }
            expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, f)) }
            expectList() { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, f)).toList() }
            expect(0) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, null)) }
            expectList() { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, null)).toList() }
        }

        test("overwriting parameters is forbidden") {
            expectThrows(IllegalArgumentException::class) {
                val dp = SqlDataProvider(SelectResult::class.java,
                    """select p.id as id, p.name as name from ${Person::class.java.databaseTableName} p where age > :age {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}""",
                    mapOf("age" to 25),
                    { it.id })
                val f = filter<SelectResult> { "age<:age"("age" to 48) }
                // this must fail because the filter also introduces parameter "age" which is already introduced in the SqlDataProvider
                dp.size(Query(f))
            }
        }

        test("parametrized DP") {
            db { (0..50).forEach { Person(name = "name $it", age = it).save() } }
            val dp = SqlDataProvider(SelectResult::class.java,
                """select p.id as id, p.name as name from ${Person::class.java.databaseTableName} p where age > :age {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}""",
                mapOf("age" to 25),
                { it.id })
            val f = filter<SelectResult> { "age<:age_f"("age_f" to 48) }
            expect(25) { dp.size(Query()) }
            expect((26..50).map { "name $it" }) { dp.fetch(Query()).toList().map { it.name } }
            expect(25) { dp.size(Query(0, 20, nameAsc, null, null)) }
            expect((26..45).map { "name $it" }) { dp.fetch(Query(0, 20, nameAsc, null, null)).toList().map { it.name } }

            // limit is ignored with size queries; also test that filter f ANDs with SqlDataProvider's filter
            expect(22) { dp.size(Query(0, 20, nameAsc, null, f)) }
            expect((47 downTo 28).map { "name $it" }) { dp.fetch(Query(0, 20, nameDesc, null, f)).toList().map { it.name } }
            expect(22) { dp.size(Query(0, Int.MAX_VALUE, nameAsc, null, f)) }

            // limit of Int.MAX_VALUE works as if no limit was specified
            expect((26..47).map { "name $it" }) { dp.fetch(Query(0, Int.MAX_VALUE, nameAsc, null, f)).toList().map { it.name } }
            expect(22) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, f)) }
            expect((26..47).map { "name $it" }) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, f)).toList().map { it.name } }

            expect(25) { dp.size(Query(0, Int.MAX_VALUE, listOf(), null, null)) }
            expect((26..50).map { "name $it" }) { dp.fetch(Query(0, Int.MAX_VALUE, listOf(), null, null)).toList().map { it.name } }
        }
    }
})
