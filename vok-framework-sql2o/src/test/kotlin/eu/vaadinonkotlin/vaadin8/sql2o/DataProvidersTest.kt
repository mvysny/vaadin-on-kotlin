package eu.vaadinonkotlin.vaadin8.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.vokdataloader.*
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import com.vaadin.ui.ComboBox
import kotlin.test.expect

class DataProvidersTest : DynaTest({
    test("fetch with empty query") {
        val loader = AssertingDataLoader(null, listOf(), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
        adapter.size(Query())
        adapter.fetch(Query())
        loader.checkCalled()
    }

    group("offset/limit") {
        test("fetch with offset=0 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 0L..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(0, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=30 limit=30") {
            val loader = AssertingDataLoader(null, listOf(), 30L..59)
            val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(30, 30, null, null, null))
            loader.checkCalled()
        }

        test("fetch with offset=100 limit=Int.MAX_VALUE") {
            val loader = AssertingDataLoader(null, listOf(), 100L..Int.MAX_VALUE)
            val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
            adapter.size(Query())
            adapter.fetch(Query(100, Int.MAX_VALUE, null, null, null))
            loader.checkCalled()
        }
    }

    test("fetch with sort orders") {
        val loader = AssertingDataLoader(null, listOf("name".asc, "age".desc), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
        adapter.size(Query())
        adapter.fetch(Query(0, Int.MAX_VALUE, QuerySortOrder.asc("personName").thenDesc("age").build(), null, null))
        loader.checkCalled()
    }

    test("fetch with filter") {
        val loader = AssertingDataLoader(buildFilter { Person::age eq "foo" }, listOf(), 0L..Int.MAX_VALUE)
        val adapter = DataLoaderAdapter(Person::class.java, loader, { it })
        adapter.size(Query(buildFilter { Person::age eq "foo" }))
        adapter.fetch(Query(0, Int.MAX_VALUE, listOf(), null, buildFilter { Person::age eq "foo" }))
        loader.checkCalled()
    }

    group("ID retrieval") {
        test("entitydp") {
            expect(1L) { Person.dataProvider.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("entitydp with filter") {
            expect(1L) { Person.dataProvider.withFilter { Person::age eq 25 }.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("sqldp") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = { it.id!! }).getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("sqldp with filter") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = { it.id!! }).withFilter { Person::age eq 25 }.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
    }

    group("API test: populating combobox with data providers") {
        // test that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
        // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
        // this test does not test a functionality; it rather tests the API itself whether the API is simple to use.
        test("entity data provider") {
            val dp = Person.dataProvider
            ComboBox<Person>().apply {
                setItemCaptionGenerator { it.personName }
                setDataProvider(dp) { searchString: String? ->
                    if (searchString.isNullOrBlank()) null else ILikeFilter(Person::personName.name, searchString)
                }
            }
        }
        // tests that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
        // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
        // this test does not test a functionality; it rather tests the API itself whether the API is simple to use.
        test("sql data provider") {
            val dp = sqlDataProvider(Person::class.java, "select * from Person where 1=1 {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}", idMapper = { it.id!! })
            ComboBox<Person>().apply {
                setItemCaptionGenerator { it.personName }
                setDataProvider(dp) { searchString: String? ->
                    if (searchString.isNullOrBlank()) null else ILikeFilter(Person::personName.name, searchString)
                }
            }
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
