package com.github.vok.framework.sql2o.vaadin

import com.github.karibu.testing.MockVaadin
import com.github.karibu.testing._get
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.karibudsl.addColumn
import com.github.vokorm.Entity
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.data.provider.DataProvider
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField
import kotlin.streams.toList
import kotlin.test.expect

@Deprecated("replace by karibu-testing")
fun <T : Any> DataProvider<T, *>._findAll(): List<T> {
    @Suppress("UNCHECKED_CAST")
    val fetched = (this as DataProvider<T, Any?>).fetch(Query<T, Any?>(0, Int.MAX_VALUE, null, null, null))
    return fetched.toList()
}

class VaadinFiltersTest : DynaTest({
    beforeEach { MockVaadin.setup() }

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow()
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).and { Person::name eq "foo" }
        expectList() { grid.dataProvider!!._findAll() }
        filterRow.generateFilterComponents(grid, Person::class)

        // now let's create another data provider
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).and { Person::name eq "foo" }

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        (filterRow.getCell("name").component as TextField).value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!._findAll() }
    }
})
