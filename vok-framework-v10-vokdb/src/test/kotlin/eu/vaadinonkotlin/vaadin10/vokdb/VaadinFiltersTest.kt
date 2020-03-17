package eu.vaadinonkotlin.vaadin10.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.karibudsl.v10.addColumnFor
import com.github.mvysny.karibudsl.v10.getAll
import com.github.mvysny.karibudsl.v10.getColumnBy
import com.github.mvysny.kaributesting.v10._value
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import eu.vaadinonkotlin.vaadin10.*

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
        val filterBar = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::personName)).ilike()

        // now let's create another data provider
        grid.dataProvider = Person.dataProvider

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        (filterBar.getFilterComponent(Person::personName) as TextField)._value = "foobar"
        expectList("foobar") { grid.dataProvider!!.getAll().map { it.personName } }
    }
})
