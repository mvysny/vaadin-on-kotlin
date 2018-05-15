package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.github.vokorm.Filter
import com.github.vokorm.buildFilter
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.SortClause
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import kotlin.test.expect

class DataProvidersTest : DynaTest({
    test("fetch with empty query") {
        val loader = AssertingDataLoader(null, listOf(), 0..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader, { it })
        adapter.size(Query())
        adapter.fetch(Query())
        loader.checkCalled()
    }

    group("offset/limit") {
        test("fetch with offset=0 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 0..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(0, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=30 limit=30") {
            val loader = AssertingDataLoader(null, listOf(), 30..59)
            val adapter = DataLoaderAdapter(loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(30, 30, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=100 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 100..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(100, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }
    }

    test("fetch with sort orders") {
        val loader = AssertingDataLoader(null, listOf("foo".asc, "bar".desc), 0..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader, { it })
        adapter.size(Query())
        adapter.fetch(Query(0, Int.MAX_VALUE, QuerySortOrder.asc("foo").thenDesc("bar").build(), null, null))
        loader.checkCalled()
    }

    test("fetch with filter") {
        val loader = AssertingDataLoader(buildFilter { Person::age eq "foo" }, listOf(), 0..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader, { it })
        adapter.size(Query(buildFilter { Person::age eq "foo" }))
        adapter.fetch(Query(0, Int.MAX_VALUE, listOf(), null, buildFilter { Person::age eq "foo" }))
        loader.checkCalled()
    }

    group("ID retrieval") {
        test("entitydp") {
            expect(1L) { Person.dataProvider.getId(Person(id = 1L, name = "foo", age = 25)) }
        }
        test("entitydp with filter") {
            expect(1L) { Person.dataProvider.withFilter { Person::age eq 25 }.getId(Person(id = 1L, name = "foo", age = 25)) }
        }
        test("sqldp") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = {it.id!!}).getId(Person(id = 1L, name = "foo", age = 25)) }
        }
        test("sqldp with filter") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = {it.id!!}).withFilter { Person::age eq 25 }.getId(Person(id = 1L, name = "foo", age = 25)) }
        }
    }
})

val String.desc get() = SortClause(this, false)
val String.asc get() = SortClause(this, true)
internal class AssertingDataLoader(val expectedFilter: Filter<Person>?,
                                   val expectedSortBy: List<SortClause>,
                                   val expectedRange: IntRange
) : DataLoader<Person> {
    private var fetchCalled: Boolean = false
    private var getCountCalled: Boolean = false
    override fun fetch(filter: Filter<Person>?, sortBy: List<SortClause>, range: IntRange): List<Person> {
        expect(expectedFilter) { filter }
        expect(expectedSortBy) { sortBy }
        expect(expectedRange) { range }
        fetchCalled = true
        return listOf()
    }
    override fun getCount(filter: Filter<Person>?): Int {
        expect(expectedFilter) { filter }
        getCountCalled = true
        return 0
    }
    fun checkCalled() {
        expect(true) { fetchCalled }
        expect(true) { getCountCalled }
    }
}
