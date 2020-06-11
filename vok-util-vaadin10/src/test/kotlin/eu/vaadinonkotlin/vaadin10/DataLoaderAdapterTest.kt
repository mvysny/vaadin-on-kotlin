package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import com.github.mvysny.vokdataloader.*
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

class DataLoaderAdapterTest : DynaTest({
    test("serializable") {
        DataLoaderAdapter(EmptyDataLoader<String>()) { it }.cloneBySerialization()
    }

    test("fetch with empty query") {
        val loader = AssertingDataLoader(null, listOf(), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader) { it }
        adapter.size(Query())
        adapter.fetch(Query())
        loader.checkCalled()
    }

    group("offset/limit") {
        test("fetch with offset=0 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 0L..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(loader) { it }
            adapter.size(Query())
            adapter.fetch(Query(0, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=30 limit=30") {
            val loader = AssertingDataLoader(null, listOf(), 30L..59)
            val adapter = DataLoaderAdapter(loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(30, 30, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=100 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 100L..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(100, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }
    }

    test("fetch with sort orders") {
        val loader = AssertingDataLoader(null, listOf("name".asc, "age".desc), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader, { it })
        adapter.size(Query())
        adapter.fetch(Query(0, Int.MAX_VALUE, QuerySortOrder.asc("name").thenDesc("age").build(), null, null))
        loader.checkCalled()
    }

    test("fetch with filter") {
        val loader = AssertingDataLoader(buildFilter { Person::age eq "foo" }, listOf(), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(loader, { it })
        adapter.size(Query(buildFilter { Person::age eq "foo" }))
        adapter.fetch(Query(0, Int.MAX_VALUE, listOf(), null, buildFilter { Person::age eq "foo" }))
        loader.checkCalled()
    }

    group("DataLoader.asDataProvider().toString() should return meaningful value") {
        test("empty dataloader") {
            expect("DataLoaderAdapter(EmptyDataLoader)") { listOf<String>().dataLoader().asDataProvider { it } .toString() }
        }
    }
})

internal class AssertingDataLoader(val expectedFilter: Filter<Person>?,
                                   val expectedSortBy: List<SortClause>,
                                   val expectedRange: LongRange
) : DataLoader<Person> {
    private var fetchCalled: Boolean = false
    private var getCountCalled: Boolean = false
    override fun fetch(filter: Filter<Person>?, sortBy: List<SortClause>, range: LongRange): List<Person> {
        expect(expectedFilter) { filter }
        expect(expectedSortBy) { sortBy }
        expect(expectedRange) { range }
        fetchCalled = true
        return listOf()
    }
    override fun getCount(filter: Filter<Person>?): Long {
        expect(expectedFilter) { filter }
        getCountCalled = true
        return 0
    }
    fun checkCalled() {
        expect(true) { fetchCalled }
        expect(true) { getCountCalled }
    }
}

data class Person(var id: Long? = null,
                  var personName: String,
                  var age: Int,
                  @Transient var ignored2: Any? = null,
                  var dateOfBirth: LocalDate? = null,
                  var created: Date? = null,
                  var alive: Boolean? = null)
