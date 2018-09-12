package com.github.vok.framework.sql2o.vaadin

import com.github.karibu.testing.v10.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.usingH2Database
import com.github.vok.karibudsl.flow.addColumnFor
import com.github.vok.karibudsl.flow.getAll
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider

class VaadinFiltersTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    usingH2Database()

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        val grid = Grid<Person>()
        grid.addColumnFor(Person::name)

        Person(name = "foobar", age = 5).save()

        grid.dataProvider = Person.dataProvider.withFilter { Person::name eq "foo" }
        expectList() { grid.dataProvider!!.getAll() }
        val filterComponents: Map<String, Component> =
            grid.appendHeaderRow().generateFilterComponents(grid, Person::class).getFilterComponents()

        // now let's create another data provider
        grid.dataProvider = Person.dataProvider

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        (filterComponents["name"] as TextField).value = "foobar"
        expectList("foobar") { grid.dataProvider!!.getAll().map { it.name } }
    }
})
