package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v10.component
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

fun FilterBar<*, *>.getFilterComponents(): List<Component> = grid.columns.mapNotNull { headerRow.getCell(it).component }

class FilterBarTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("Test simple auto-generated filters") {
        data class Person(var name: String, var age: Int, val dob: Date, val dateOfMarriage: LocalDate)
        val grid: Grid<Person> = Grid(Person::class.java)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid, PredicateFilterFactory<Person>())
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).ilike()
        filterBar.forField(NumberRangePopup(), grid.getColumnBy(Person::age)).inRange()
        filterBar.forField(DateRangePopup(), grid.getColumnBy(Person::dob)).inRange(Person::dob)
        filterBar.forField(DateRangePopup(), grid.getColumnBy(Person::dateOfMarriage)).inRange(Person::dateOfMarriage)
        expect<Class<*>>(TextField::class.java) { filterBar.getFilterComponent(Person::name).javaClass }
        expect<Class<*>>(NumberRangePopup::class.java) { filterBar.getFilterComponent(Person::age).javaClass }
        expect<Class<*>>(DateRangePopup::class.java) { filterBar.getFilterComponent(Person::dob).javaClass }
        expect<Class<*>>(DateRangePopup::class.java) { filterBar.getFilterComponent(Person::dateOfMarriage).javaClass }
        filterBar.serializeToBytes()
    }

    test("string filter") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid, PredicateFilterFactory<Person>())
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).ilike()
        grid.setItems(Person("foo"))
        expect(1) { grid.dataProvider._size() }

        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField
        nameFilter._value = "bar"
        expect(0) { grid.dataProvider._size() }
        nameFilter._value = "foo"
        expect(1) { grid.dataProvider._size() }
    }

    test("filter components from cleared filter bar won't affect the grid anymore") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid, PredicateFilterFactory<Person>())
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).ilike()
        grid.setItems(Person("foo"))
        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField

        expect(1) { grid.dataProvider._size() }
        nameFilter._value = "bar"
        expect(0) { grid.dataProvider._size() }
        filterBar.clear()
        expect("") { nameFilter._value }
        expect(1) { grid.dataProvider._size() }
        nameFilter._value = "baz"
        expect(0) { grid.dataProvider._size() }
        nameFilter._value = "foo"
        expect(1) { grid.dataProvider._size() }
    }

    test("filter components from cleared filter bar gone") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid, PredicateFilterFactory<Person>())
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).ilike()
        filterBar.removeAllBindings()
        expectList() { filterBar.getFilterComponents() }
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

        val filterBar = grid.appendHeaderRow().asFilterBar(grid, PredicateFilterFactory<Person>())
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).ilike()

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar")))

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField
        nameFilter.value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!.getAll() }
    }
})
