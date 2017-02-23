package com.github.vok.example.crud.personeditor

import com.github.vok.framework.db
import com.github.vok.framework.deleteById
import com.github.vok.framework.vaadin.*
import com.github.vok.framework.vaadin.ModifierKey.Alt
import com.github.vok.framework.vaadin.ModifierKey.Ctrl
import com.vaadin.event.ShortcutAction.KeyCode.C
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.data.sort.SortDirection
import com.vaadin.ui.Button
import com.vaadin.ui.Grid
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.renderers.ButtonRenderer

/**
 * Demonstrates a CRUD over [Person]. Note how the autoViewProvider automatically discovers your view and assigns a name to it.
 */
class CrudView: VerticalLayout(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
        refreshGrid()
    }

    companion object {
        fun navigateTo() = navigateToView<CrudView>()
    }

    private lateinit var createButton: Button
    private val personGrid: Grid<Person>
    private val personGridDS = jpaDataSource<Person>()

    init {
        setSizeFull()
        horizontalLayout {
            createButton = button("Create New Person (Ctrl+Alt+C)") {
                onLeftClick { createOrEditPerson(Person()) }
                clickShortcut = Ctrl + Alt + C
            }
            button("Generate testing data", { generateTestingData() })
        }
        // the JPA list demo - shows all instances of a particular JPA entity, allow sorting and filtering
        personGrid = grid(Person::class, dataProvider = personGridDS) {
            expandRatio = 1f; setSizeFull()
            showColumns(Person::id, Person::name, Person::age)
            column(Person::id) {
                isSortable = false
            }
            addColumn({ "Show" }, ButtonRenderer<Person>({ event -> PersonView.navigateTo(event.item) }))
            addColumn({ "Edit" }, ButtonRenderer<Person>({ event -> createOrEditPerson(event.item) }))
            addColumn({ "Delete" }, ButtonRenderer<Person>({ event -> deletePerson(event.item.id!!) }))
            // automatically create filters, based on the types of values present in particular columns.
//            appendHeaderRow().generateFilterComponents(this)
        }
    }

    private fun generateTestingData() {
        db {
            (0..85).forEach { em.persist(Person(name = "generated$it", age = it + 15)) }
        }
        refreshGrid()
    }

    private fun deletePerson(id: Long) {
        db { em.deleteById<Person>(id) }
        refreshGrid()
    }

    private fun createOrEditPerson(person: Person) {
        CreateEditPerson(person).apply {
            addCloseListener { refreshGrid() }
            UI.getCurrent().addWindow(this)
        }
    }

    private fun refreshGrid() {
        personGridDS.refreshAll()
    }
}
