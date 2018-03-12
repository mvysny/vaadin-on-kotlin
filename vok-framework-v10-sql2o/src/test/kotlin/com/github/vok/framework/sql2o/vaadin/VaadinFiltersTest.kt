package com.github.vok.framework.sql2o.vaadin

import com.github.karibu.testing.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.karibudsl.flow.addColumnFor
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.provider.Query
import kotlin.streams.toList

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
        val grid = Grid<Person>()
        grid.addColumnFor(Person::name)
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).and { Person::name eq "foo" }
        expectList() { grid.dataProvider!!._findAll() }
        val filterComponents = grid.generateFilterComponents(Person::class)

        // now let's create another data provider
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).and { Person::name eq "foo" }

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        (filterComponents["name"] as TextField).value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!._findAll() }
    }
})
