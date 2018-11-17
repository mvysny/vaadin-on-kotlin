package eu.vaadinonkotlin.vaadin10.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.karibudsl.v10.addColumnFor
import com.github.mvysny.karibudsl.v10.getAll
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import eu.vaadinonkotlin.vaadin10.generateFilterComponents
import eu.vaadinonkotlin.vaadin10.withFilter

class VaadinFiltersTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }
    usingH2Database()

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        val grid = Grid<Person>()
        grid.addColumnFor(Person::personName)

        Person(personName = "foobar", age = 5).save()

        grid.dataProvider = Person.dataProvider.withFilter { Person::personName eq "foo" }
        expectList() { grid.dataProvider!!.getAll() }
        val filterComponents: Map<String, Component> =
            grid.appendHeaderRow().generateFilterComponents(grid, Person::class).getFilterComponents()

        // now let's create another data provider
        grid.dataProvider = Person.dataProvider

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        (filterComponents["personName"] as TextField).value = "foobar"
        expectList("foobar") { grid.dataProvider!!.getAll().map { it.personName } }
    }
})
