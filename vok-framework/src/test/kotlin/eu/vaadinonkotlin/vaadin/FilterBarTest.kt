package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributools.getColumnBy
import com.github.vokorm.buildCondition
import com.github.vokorm.exp
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateRangePopup
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberRangePopup
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

fun FilterBar<*>.getFilterComponents(): List<Component> = grid.columns.mapNotNull { headerRow.getCell(it).component }

class FilterBarTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("Test simple auto-generated filters") {
        data class Person(var name: String, var age: Int, val dob: Date, val dateOfMarriage: LocalDate)
        val grid: Grid<Person> = Grid(Person::class.java)
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        grid.getColumnBy(Person::age).setSortProperty(Person::age.exp)
        grid.getColumnBy(Person::dob).setSortProperty(Person::dob.exp)
        grid.getColumnBy(Person::dateOfMarriage).setSortProperty(Person::dateOfMarriage.exp)

        val filterBar: FilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        filterBar.forField(NumberRangePopup(), grid.getColumnBy(Person::age)).inRange()
        filterBar.forField(DateRangePopup(), grid.getColumnBy(Person::dob)).inRange()
        filterBar.forField(DateRangePopup(), grid.getColumnBy(Person::dateOfMarriage)).inRange()
        expect<Class<*>>(TextField::class.java) { filterBar.getFilterComponent(Person::name).javaClass }
        expect<Class<*>>(NumberRangePopup::class.java) { filterBar.getFilterComponent(Person::age).javaClass }
        expect<Class<*>>(DateRangePopup::class.java) { filterBar.getFilterComponent(Person::dob).javaClass }
        expect<Class<*>>(DateRangePopup::class.java) { filterBar.getFilterComponent(Person::dateOfMarriage).javaClass }
        // Kotlin 1.5.0 lambdas are no longer serializable: https://youtrack.jetbrains.com/issue/KT-46373
        // Screw it, it makes no sense to serialize Vaadin components anyway because
        // Vaadin doesn't really support session replication as per
        // https://mvysny.github.io/vaadin-14-session-replication/
//        filterBar.serializeToBytes()
    }

    test("string filter") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        val filterBar: FilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        grid.setItems(Person("foo"))
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
        grid.getColumnBy(Person::dob).setSortProperty(Person::dob.exp)
        val filterBar: FilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(DatePicker(), grid.getColumnBy(Person::dob)).onDay()
        grid.setItems(Person(LocalDateTime.of(2020, 3, 20, 1, 0)))
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
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        val filterBar: FilterBar<Person> = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        grid.setItems(Person("foo"))
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
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()
        filterBar.removeAllBindings()
        expectList() { filterBar.getFilterComponents() }
    }

    // test for https://www.github.com/mvysny/vaadin-on-kotlin/issues/17
    test("grid's data provider is polled lazily on filter change") {
        data class Person(var name: String)
        val grid = Grid<Person>(Person::class.java)
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        grid.appendHeaderRow()
        grid.dataProvider = ListDataProvider<Person>(listOf(Person("foobar"))).apply {
            setFilter { false }
        }
        expectList() { grid.dataProvider!!._findAll() }

        val filterBar = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.forField(TextField(), grid.getColumnBy(Person::name)).istartsWith()

        // now let's create and set another data provider. If the generateFilterComponents grabs the DP eagerly, it will ignore this second DP.
        grid.setItems(Person("foobar"))

        // if the generateFilterComponents function reflects the DP change, it will overwrite the filter, making the DP match the person
        val nameFilter = filterBar.getFilterComponent(Person::name) as TextField
        nameFilter.value = "foobar"
        expectList(Person("foobar")) { grid.dataProvider!!._findAll() }
    }

    test("onFilterChanged invoked") {
        data class Person(var name: String)
        val grid = Grid(Person::class.java)
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid)
        filterBar.onFilterChanged = { fail("should not be called") }
        val filterField = TextField()
        filterBar.forField(filterField, grid.getColumnBy(Person::name)).istartsWith()
        grid.setItems(Person("foo"))

        var called = false
        filterBar.onFilterChanged = { called = true }
        filterField._value = "bar"
        expect(true) { called }

        called = false
        filterBar.setCustomFilter("global", buildCondition { Person::name eq "baz" })
        expect(true) { called }

        // setting the same filter again doesn't fire the listener
        called = false
        filterBar.setCustomFilter("global", buildCondition { Person::name eq "baz" })
        expect(false) { called }

        called = false
        filterBar.setCustomFilter("global", null)
        expect(true) { called }
    }

    test("custom filters are applied") {
        data class Person(var name: String)
        val grid: Grid<Person> = Grid(Person::class.java)
        grid.getColumnBy(Person::name).setSortProperty(Person::name.exp)
        val filterBar = grid.appendHeaderRow().asFilterBar(grid)
        grid.setItems(Person("foo"))
        grid.expectRows(1)

        filterBar.setCustomFilter("global", buildCondition { Person::name eq "baz" })
        grid.expectRows(0)

        filterBar.setCustomFilter("global", buildCondition { Person::name eq "foo" })
        grid.expectRows(1)

        filterBar.setCustomFilter("global", buildCondition { Person::name eq "baz" })
        grid.expectRows(0)

        filterBar.setCustomFilter("global", null)
        grid.expectRows(1)
    }
})
