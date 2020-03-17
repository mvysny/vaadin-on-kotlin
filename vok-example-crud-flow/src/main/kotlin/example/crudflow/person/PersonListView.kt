package example.crudflow.person

import example.crudflow.MainLayout
import eu.vaadinonkotlin.vaadin10.vokdb.dataProvider
import eu.vaadinonkotlin.toDate
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v10.ModifierKey.Alt
import com.github.mvysny.vokdataloader.Filter
import com.github.vokorm.db
import com.vaadin.flow.component.Key.KEY_G
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.NativeButtonRenderer
import com.vaadin.flow.data.renderer.Renderer
import com.vaadin.flow.router.Route
import eu.vaadinonkotlin.vaadin10.*
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
                val filterBar: FilterBar<Person, Filter<Person>> = appendHeaderRow().asFilterBar(this)

                addColumnFor(Person::id, sortable = false) {
                    width = "90px"; isExpand = false
                }
                addColumnFor(Person::name) {
                    filterBar.forField(TextField(), this).ilike()
                }
                addColumnFor(Person::age, center { it.age?.toString() }) {
                    width = "120px"; isExpand = false
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

                addColumn(NativeButtonRenderer<Person>("View", { person -> navigateToView(PersonView::class, person.id!!) })).apply {
                    width = "90px"; isExpand = false
                }
                addColumn(NativeButtonRenderer<Person>("Edit", { person -> createOrEditPerson(person) })).apply {
                    width = "90px"; isExpand = false; key = "edit"
                }
                addColumn(NativeButtonRenderer<Person>("Delete", { person -> person.delete(); refresh() })).apply {
                    width = "90px"; isExpand = false; key = "delete"
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
 * A workaround for centering column contents until https://github.com/vaadin/vaadin-grid-flow/issues/185 is implemented
 */
fun <T> Grid<T>.center(vp: (T)->String?): Renderer<T> =
    ComponentRenderer<Div, T>({ it:T -> Div().apply { text = vp(it); style.set("text-align", "center") }})
