package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.*
import com.github.vokorm.db
import com.github.vokorm.exp
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
import com.vaadin.flow.component.textfield.TextField
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
            personGrid = grid<Person>(Person.dataProvider) {
                flexGrow = 1.0
                appendHeaderRow() // because of https://github.com/vaadin/vaadin-grid/issues/1870
                val filterBar = appendHeaderRow().asFilterBar(this)

                addButtonColumn(VaadinIcon.EYE, "view", { person: Person -> navigateTo(PersonView::class, person.id!!) }) {}
                addButtonColumn(VaadinIcon.EDIT, "edit", { person: Person -> createOrEditPerson(person) }) {}
                addButtonColumn(VaadinIcon.TRASH, "delete", { person: Person -> person.delete(); refresh() }) {}

                columnFor(Person::id, sortable = false) {
                    width = "90px"; isExpand = false
                }
                val nameColumn = columnFor(Person::name) {
                    setSortProperty(Person::name.exp)
                    filterBar.forField(FilterTextField(), this).istartsWith()
                }
                columnFor(Person::age) {
                    width = "120px"; isExpand = false; textAlign = ColumnTextAlign.CENTER
                    setSortProperty(Person::age.exp)
                    filterBar.forField(NumberRangePopup(), this).inRange()
                }
                columnFor(Person::alive) {
                    width = "130px"; isExpand = false
                    setSortProperty(Person::alive.exp)
                    filterBar.forField(BooleanFilterField(), this).eq()
                }
                columnFor(Person::dateOfBirth, converter = { it?.toString() }) {
                    setSortProperty(Person::dateOfBirth.exp)
                    filterBar.forField(DateRangePopup(), this).inRange()
                }
                columnFor(Person::maritalStatus) {
                    width = "160px"; isExpand = false
                    setSortProperty(Person::maritalStatus.exp)
                    filterBar.forField(enumFilterField<MaritalStatus>(), this).`in`(true)
                }
                columnFor(Person::created, converter = { it!!.toInstant().toString() }) {
                    setSortProperty(Person::created.exp)
                    filterBar.forField(DateRangePopup(), this).inRange()
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