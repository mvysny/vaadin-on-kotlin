package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.*
import com.github.mvysny.ktormvaadin.and
import com.github.mvysny.ktormvaadin.dataProvider
import com.github.mvysny.ktormvaadin.db
import com.github.mvysny.ktormvaadin.e
import com.github.mvysny.ktormvaadin.filter.BooleanFilterField
import com.github.mvysny.ktormvaadin.filter.DateInterval
import com.github.mvysny.ktormvaadin.filter.DateRangePopup
import com.github.mvysny.ktormvaadin.filter.FilterTextField
import com.github.mvysny.ktormvaadin.filter.NumberRangePopup
import com.github.mvysny.ktormvaadin.filter.between
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
import eu.vaadinonkotlin.vaadin.vokdb.enumFilterField
import example.crudflow.MainLayout
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.greaterEq
import org.ktorm.dsl.inList
import org.ktorm.dsl.less
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The main view. Uses ktorm-vaadin's [EntityDataProvider] backed by [Persons]; filter components are wired into
 * the grid header row, with their values combined into a ktorm [ColumnDeclaring] in [updateFilter].
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
    private val dataProvider = Persons.dataProvider
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

                columnFor(Person::id, sortable = false, key = Persons.id.e.key) {
                    width = "90px"; isExpand = false
                }
                val nameColumn = columnFor(Person::name, key = Persons.name.e.key) {
                    nameFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = nameFilter
                }
                columnFor(Person::age, key = Persons.age.e.key) {
                    width = "120px"; isExpand = false; textAlign = ColumnTextAlign.CENTER
                    ageFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = ageFilter
                }
                columnFor(Person::alive, key = Persons.alive.e.key) {
                    width = "130px"; isExpand = false
                    aliveFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = aliveFilter
                }
                columnFor(Person::dateOfBirth, converter = { it?.toString() }, key = Persons.dateOfBirth.e.key) {
                    dateOfBirthFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = dateOfBirthFilter
                }
                columnFor(Person::maritalStatus, key = Persons.maritalStatus.e.key) {
                    width = "160px"; isExpand = false
                    maritalStatusFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = maritalStatusFilter
                }
                columnFor(Person::created, converter = { it?.toString() }, key = Persons.created.e.key) {
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
        val conditions = mutableListOf<ColumnDeclaring<Boolean>?>()
        if (nameFilter.value.isNotBlank()) {
            conditions += Persons.name.ilike("${nameFilter.value.trim()}%")
        }
        conditions += Persons.age.between(ageFilter.value.asIntegerInterval())
        aliveFilter.value?.let { conditions += Persons.alive eq it }
        conditions += Persons.dateOfBirth.between(dateOfBirthFilter.value)
        val selectedStatuses = maritalStatusFilter.value
        if (selectedStatuses.isNotEmpty() && selectedStatuses.size < MaritalStatus.entries.size) {
            conditions += Persons.maritalStatus.inList(selectedStatuses.toList())
        }
        conditions += createdFilter.value.containsInstant(Persons.created)
        dataProvider.setFilter(conditions.and())
    }

    /** ktorm-vaadin's DateInterval works on [LocalDate]; the [Persons.created] column is [Instant], so widen the
     * day range to an Instant range in the browser's timezone. */
    private fun DateInterval.containsInstant(col: Column<Instant>, zone: ZoneId = BrowserTimeZone.get): ColumnDeclaring<Boolean>? {
        val startInstant: Instant? = start?.atStartOfDay(zone)?.toInstant()
        val endExclusive: Instant? = endInclusive?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
        return when {
            startInstant != null && endExclusive != null -> (col greaterEq startInstant) and (col less endExclusive)
            startInstant != null -> col greaterEq startInstant
            endExclusive != null -> col less endExclusive
            else -> null
        }
    }

    private fun createOrEditPerson(person: Person) {
        CreateEditPerson(person).apply {
            onSaveOrCreateListener = { personGrid.refresh() }
        }.open()
    }

    private fun generateTestingData() {
        db {
            (0..85).forEach { i ->
                Person {
                    name = "generated$i"
                    age = i + 15
                    maritalStatus = MaritalStatus.Single
                    alive = true
                    dateOfBirth = LocalDate.of(1990, 1, 1).plusDays(i.toLong())
                    created = LocalDate.of(2011, 1, 1).plusDays(i.toLong()).atStartOfDay(BrowserTimeZone.get).toInstant()
                }.create()
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
