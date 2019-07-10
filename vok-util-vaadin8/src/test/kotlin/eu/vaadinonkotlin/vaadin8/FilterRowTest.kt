package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.github.mvysny.kaributesting.v8._size
import com.github.mvysny.kaributesting.v8._value
import com.github.mvysny.karibudsl.v8.getAll
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.server.SerializablePredicate
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

class FilterRowTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("Test simple auto-generated filters") {
        data class Person(var name: String, var age: Int, val dob: Date, val dateOfMarriage: LocalDate)
        val grid = Grid<Person>(Person::class.java)
        val filterRow: FilterRow<Person, SerializablePredicate<Person>> =
                grid.appendHeaderRow().generateFilterComponents(grid, Person::class, PredicateFilterFactory<Person>())
        filterRow.cloneBySerialization()
        expect<Class<*>>(TextField::class.java) { filterRow.getFilterComponent(Person::name).javaClass }
        expect<Class<*>>(NumberFilterPopup::class.java) { filterRow.getFilterComponent(Person::age).javaClass }
        expect<Class<*>>(DateFilterPopup::class.java) { filterRow.getFilterComponent(Person::dob).javaClass }
        expect<Class<*>>(DateFilterPopup::class.java) { filterRow.getFilterComponent(Person::dateOfMarriage).javaClass }
    }

    test("string filter") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow().generateFilterComponents(grid, Person::class, PredicateFilterFactory<Person>())
        grid.setItems(Person("foo"))
        expect(1) { grid.dataProvider._size() }

        val nameFilter = filterRow.getFilterComponent(Person::name)
        nameFilter._value = "bar"
        expect(0) { grid.dataProvider._size() }
        nameFilter._value = "foo"
        expect(1) { grid.dataProvider._size() }
    }

    test("filter components from cleared filter bar won't affect the grid anymore") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow().generateFilterComponents(grid, Person::class, PredicateFilterFactory<Person>())
        grid.setItems(Person("foo"))
        val nameFilter = filterRow.getFilterComponent(Person::name)

        expect(1) { grid.dataProvider._size() }
        nameFilter._value = "bar"
        expect(0) { grid.dataProvider._size() }
        filterRow.clear()
        expect(1) { grid.dataProvider._size() }
        nameFilter._value = "baz"
        expect(1) { grid.dataProvider._size() }
    }

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterRow = grid.appendHeaderRow()
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).apply {
            setFilter { false }
        }
        expectList() { grid.dataProvider!!.getAll() }

        filterRow.generateFilterComponents(grid, Person::class, PredicateFilterFactory<Person>())

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar")))

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterRow.getCell("name").component as TextField
        nameFilter.value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!.getAll() }
    }
})
