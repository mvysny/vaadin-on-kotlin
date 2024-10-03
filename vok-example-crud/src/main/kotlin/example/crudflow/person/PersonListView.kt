package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.*
import com.github.vokorm.db
import com.github.vokorm.exp
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.vaadin.filter.BooleanFilterField
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateRangePopup
import com.gitlab.mvysny.jdbiorm.vaadin.filter.FilterTextField
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberRangePopup
import com.vaadin.flow.component.Key.KEY_G
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu
import com.vaadin.flow.component.icon.IconFactory
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.Route
import eu.vaadinonkotlin.toDate
import eu.vaadinonkotlin.vaadin.*
import eu.vaadinonkotlin.vaadin.vokdb.dataProvider
import eu.vaadinonkotlin.vaadin.vokdb.enumFilterField
import example.crudflow.MainLayout
import java.time.LocalDate

/**
 * The main view contains a button and a template element.
 */
@Route("", layout = MainLayout::class)
class PersonListView : KComposite() {
    private lateinit var personGrid: Grid<Person>
    lateinit var gridContextMenu: GridContextMenu<Person>
    private val nameFilter = FilterTextField()
    private val ageFilter = NumberRangePopup()
    private val aliveFilter = BooleanFilterField()
    private val dateOfBirthFilter = DateRangePopup()
    private val maritalStatusFilter = enumFilterField<MaritalStatus>()
    private val dataProvider = Person.dataProvider
    private val createdFilter = DateRangePopup()

    private val root = ui {
        verticalLayout {
            setSizeFull()
            h4("Person list")
            button("Generate testing data (Alt+G)") {
                onClick {
                    generateTestingData()
                }
                addClickShortcut(Alt + KEY_G)
            }
            personGrid = grid<Person>(dataProvider) {
                flexGrow = 1.0
                appendHeaderRow() // because of https://github.com/vaadin/vaadin-grid/issues/1870
                val filterBar = appendHeaderRow()

                addButtonColumn(VaadinIcon.EYE, "view", { person: Person -> navigateTo(PersonView::class, person.id!!) }) {}
                addButtonColumn(VaadinIcon.EDIT, "edit", { person: Person -> createOrEditPerson(person) }) {}
                addButtonColumn(VaadinIcon.TRASH, "delete", { person: Person -> person.delete(); refresh() }) {}

                columnFor(Person::id, sortable = false) {
                    width = "90px"; isExpand = false
                }
                val nameColumn = columnFor(Person::name) {
                    setSortProperty(Person::name.exp)
                    nameFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = nameFilter
                }
                columnFor(Person::age) {
                    width = "120px"; isExpand = false; textAlign = ColumnTextAlign.CENTER
                    setSortProperty(Person::age.exp)
                    ageFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = ageFilter
                }
                columnFor(Person::alive) {
                    width = "130px"; isExpand = false
                    setSortProperty(Person::alive.exp)
                    aliveFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = aliveFilter
                }
                columnFor(Person::dateOfBirth, converter = { it?.toString() }) {
                    setSortProperty(Person::dateOfBirth.exp)
                    dateOfBirthFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = dateOfBirthFilter
                }
                columnFor(Person::maritalStatus) {
                    width = "160px"; isExpand = false
                    setSortProperty(Person::maritalStatus.exp)
                    maritalStatusFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = maritalStatusFilter
                }
                columnFor(Person::created, converter = { it!!.toInstant().toString() }) {
                    setSortProperty(Person::created.exp)
                    createdFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = createdFilter
                }

                gridContextMenu = gridContextMenu {
                    item("view", { person: Person? -> if (person != null) navigateTo(PersonView::class, person.id!!) })
                    item("edit", { person: Person? -> if (person != null) createOrEditPerson(person) })
                    item("delete", { person: Person? -> if (person != null) { person.delete(); refresh() } })
                }

                sort(nameColumn.asc)
            }
        }
    }

    private fun updateFilter() {
        var c: Condition = Condition.NO_CONDITION
        if (!nameFilter.value.isBlank()) {
            c = c.and(Person::name.exp.startsWithIgnoreCase(nameFilter.value))
        }
        c = c.and(ageFilter.value.asIntegerInterval().contains(Person::age.exp))
        if (!aliveFilter.isEmpty) {
            c = c.and(Person::alive.exp.`is`(aliveFilter.value))
        }
        c = c.and(dateOfBirthFilter.value.contains(Person::dateOfBirth.exp, BrowserTimeZone.get))
        if (!maritalStatusFilter.isAllOrNothingSelected) {
            c = c.and(Person::maritalStatus.exp.`in`(maritalStatusFilter.value))
        }
        c = c.and(createdFilter.value.contains(Person::created.exp, BrowserTimeZone.get))
        dataProvider.filter = c
    }

    private fun createOrEditPerson(person: Person) {
        CreateEditPerson(person).apply {
            onSaveOrCreateListener = { personGrid.refresh() }
        }.open()
    }

    private fun generateTestingData() {
        db {
            (0..85).forEach {
                Person(name = "generated$it", age = it + 15, maritalStatus = MaritalStatus.Single, alive = true,
                        dateOfBirth = LocalDate.of(1990, 1, 1).plusDays(it.toLong()),
                        created = LocalDate.of(2011, 1, 1).plusDays(it.toLong()).atStartOfDay(BrowserTimeZone.get).toInstant().toDate).save()
            }
        }
        personGrid.dataProvider.refreshAll()
    }
}

/**
 * Utility method which adds a column housing one small icon button with given [icon] and [clickListener].
 */
fun <T> Grid<T>.addButtonColumn(icon: IconFactory, key: String, clickListener: (T) -> Unit, block: Grid.Column<T>.()->Unit): Grid.Column<T> {
    val renderer = ComponentRenderer<Button, T> { row: T ->
        val button = Button(icon.create())
        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)
        button.addClickListener { clickListener(row) }
        button
    }
    val column: Grid.Column<T> = addColumn(renderer).apply {
        setKey(key)
        setWidth("50px")
        isExpand = false
        block()
    }
    return column
}