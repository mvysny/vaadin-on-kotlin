package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v10.component
import com.github.mvysny.vokdataloader.Filter
import com.github.mvysny.vokdataloader.buildFilter
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.expect
import kotlin.test.fail

fun FilterBar<*, *>.getFilterComponents(): List<Component> = grid.columns.mapNotNull { headerRow.getCell(it).component }

class FilterBarTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("Test simple auto-generated filters") {
        data class Person(var name: String, var age: Int, val dob: Date, val dateOfMarriage: LocalDate)
        val grid: Grid<Person> = Grid(Person::class.java)
        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
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
        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        grid.setDataLoaderItems(Person("foo"))
        expect(1) { grid.dataProvider._size() }

        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField
        nameFilter._value = "bar"
        expect(0) { grid.dataProvider._size() }
        nameFilter._value = "foo"
        expect(1) { grid.dataProvider._size() }
    }

    /**
     * https://github.com/mvysny/vaadin-on-kotlin/issues/49
     */
    test("day filter") {
        data class Person(var dob: LocalDateTime)
        val grid = Grid<Person>(Person::class.java)
        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(DatePicker(), grid.getColumnBy(Person::dob)).onDay(LocalDateTime::class)
        grid.setDataLoaderItems(Person(LocalDateTime.of(2020, 3, 20, 1, 0)))
        expect(1) { grid.dataProvider._size() }

        val dayFilter: DatePicker = filterBar.getFilterComponent(Person::dob) as DatePicker
        dayFilter._value = LocalDate.of(2020, 3, 21)
        expect(0) { grid.dataProvider._size() }
        dayFilter._value = LocalDate.of(2020, 3, 20)
        expect(1) { grid.dataProvider._size() }
    }

    test("filter components from cleared filter bar won't affect the grid anymore") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        grid.setDataLoaderItems(Person("foo"))
        val nameFilter: TextField = filterBar.getFilterComponent(Person::name) as TextField

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
        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        filterBar.removeAllBindings()
        expectList() { filterBar.getFilterComponents() }
    }

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        grid.appendHeaderRow()
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).apply {
            setFilter { false }
        }
        expectList() { grid.dataProvider!!.getAll() }

        val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.setDataLoaderItems(Person("foobar"))

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField
        nameFilter.value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!.getAll() }
    }

    test("onFilterChanged invoked") {
        data class Person(var name: String)
        val grid = Grid(Person::class.java)
        val filterBar: FilterBar<Person, Filter<Person>> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.onFilterChanged = { fail("should not be called") }
        val filterField = TextField()
        filterBar.forField(filterField, grid.getColumnBy(Person::name)).istartsWith()
        grid.setDataLoaderItems(Person("foo"))

        var called = false
        filterBar.onFilterChanged = { called = true }
        filterField._value = "bar"
        expect(true) { called }

        called = false
        filterBar.setCustomFilter("global", buildFilter { Person::name eq "baz" })
        expect(true) { called }

        // setting the same filter again doesn't fire the listener
        called = false
        filterBar.setCustomFilter("global", buildFilter { Person::name eq "baz" })
        expect(false) { called }

        called = false
        filterBar.setCustomFilter("global", null)
        expect(true) { called }
    }

    test("custom filters are applied") {
        data class Person(var name: String)
        val grid: Grid<Person> = Grid(Person::class.java)
        val filterBar: FilterBar<Person, Filter<Person>> = grid.appendHeaderRow().asFilterBar(grid)
        grid.setDataLoaderItems(Person("foo"))
        grid.expectRows(1)

        filterBar.setCustomFilter("global", buildFilter { Person::name eq "baz" })
        grid.expectRows(0)

        filterBar.setCustomFilter("global", buildFilter { Person::name eq "foo" })
        grid.expectRows(1)

        filterBar.setCustomFilter("global", buildFilter { Person::name eq "baz" })
        grid.expectRows(0)

        filterBar.setCustomFilter("global", null)
        grid.expectRows(1)
    }
})
