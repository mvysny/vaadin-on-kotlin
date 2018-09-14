package com.github.vok.framework.sql2o.vaadin

import com.github.karibu.testing.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.usingH2Database
import com.github.vok.karibudsl.getAll
import com.github.vok.karibudsl.getCell
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField

class VaadinFiltersTest : DynaTest({
    beforeEach { MockVaadin.setup() }

    usingH2Database()

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow()
        Person(personName = "foobar", age = 5).save()

        grid.dataProvider = Person.dataProvider.withFilter { Person::personName eq "foo" }
        expectList() { grid.dataProvider!!.getAll() }
        filterRow.generateFilterComponents(grid, Person::class)

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.dataProvider = Person.dataProvider

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterRow.getCell(Person::personName).component as TextField
        nameFilter.value = "foobar"
        expectList("foobar") { grid.dataProvider!!.getAll().map { it.personName } }
    }
})
