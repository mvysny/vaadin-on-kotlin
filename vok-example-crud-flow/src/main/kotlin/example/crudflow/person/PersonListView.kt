package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v10.ModifierKey.Alt
import com.github.vokorm.db
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
import eu.vaadinonkotlin.vaadin10.*
import eu.vaadinonkotlin.vaadin10.vokdb.dataProvider
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
                onLeftClick {
                    generateTestingData()
                }
                addClickShortcut(Alt + KEY_G)
            }
            personGrid = grid(dataProvider = Person.dataProvider) {
                flexGrow = 1.0
                appendHeaderRow()
                val filterBar: VokFilterBar<Person> = appendHeaderRow().asFilterBar(this)

                addButtonColumn(VaadinIcon.EYE, "view", { person -> navigateToView(PersonView::class, person.id!!) }) {}
                addButtonColumn(VaadinIcon.EDIT, "edit", { person -> createOrEditPerson(person) }) {}
                addButtonColumn(VaadinIcon.TRASH, "delete", { person -> person.delete(); refresh() }) {}

                addColumnFor(Person::id, sortable = false) {
                    width = "90px"; isExpand = false
                }
                addColumnFor(Person::name) {
                    filterBar.forField(TextField(), this).ilike()
                }
                addColumnFor(Person::age) {
                    width = "120px"; isExpand = false; textAlign = ColumnTextAlign.CENTER
                    filterBar.forField(NumberRangePopup(), this).inRange()
                }
                addColumnFor(Person::alive) {
                    width = "130px"; isExpand = false
                    filterBar.forField(BooleanComboBox(), this).eq()
                }
                addColumnFor(Person::dateOfBirth, converter = { it?.toString() }) {
                    filterBar.forField(DateRangePopup(), this).inRange(Person::dateOfBirth)
                }
                addColumnFor(Person::maritalStatus) {
                    width = "160px"; isExpand = false
                    filterBar.forField(enumComboBox<MaritalStatus>(), this).eq()
                }
                addColumnFor(Person::created, converter = { it!!.toInstant().toString() }) {
                    filterBar.forField(DateRangePopup(), this).inRange(Person::created)
                }

                gridContextMenu = gridContextMenu {
                    item("view", { person -> if (person != null) navigateToView(PersonView::class, person.id!!) })
                    item("edit", { person -> if (person != null) createOrEditPerson(person) })
                    item("delete", { person -> if (person != null) { person.delete(); refresh() } })
                }
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
                        created = LocalDate.of(2011, 1, 1).plusDays(it.toLong()).atStartOfDay(browserTimeZone).toInstant().toDate).save()
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
    val column: Grid.Column<T> = addColumn(renderer).setKey(key).setWidth("50px")
    column.isExpand = false
    column.block()
    return column
}