package com.github.vok.framework.flow

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.karibudsl.flow.getAll
import com.github.vok.karibudsl.flow.getCell
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider

class VaadinFiltersTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    data class Person(var name: String, var age: Int)

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow()

        grid.dataProvider = ListDataProvider(listOf(Person(name = "foobar", age = 5))).withFilter { Person::name eq "foo" }
        expectList() { grid.dataProvider!!.getAll() }
        filterRow.generateFilterComponents(grid, Person::class, PredicateFilterFactory<Person>())

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.dataProvider = ListDataProvider(listOf(Person(name = "foobar", age = 5)))

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterRow.getCell(Person::name).component as TextField
        nameFilter.value = "foobar"
        expectList("foobar") { grid.dataProvider!!.getAll().map { it.name } }
    }
})
