package com.github.vok.example.crud.personeditor

import com.github.vok.framework.db
import com.github.vok.framework.deleteById
import com.github.vok.framework.get
import com.github.vok.framework.jpaContainer
import com.github.vok.framework.vaadin.*
import com.github.vok.framework.vaadin.ModifierKey.Alt
import com.github.vok.framework.vaadin.ModifierKey.Ctrl
import com.github.vok.framework.vaadin7.cols
import com.github.vok.framework.vaadin7.generateFilterComponents
import com.github.vok.framework.vaadin7.grid7
import com.vaadin.event.ShortcutAction.KeyCode.C
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.Button
import com.vaadin.v7.ui.Grid
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout

/**
 * Demonstrates a CRUD over [Person]. Note how the autoViewProvider automatically discovers your view and assigns a name to it.
 */
@AutoView
class CrudView: VerticalLayout(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
        refreshGrid()
    }

    companion object {
        fun navigateTo() = navigateToView<CrudView>()
    }

    private val createButton: Button
    private val personGrid: Grid
    private val personGridDS = jpaContainer<Person>()

    init {
        setSizeFull()
        createButton = button("Create New Person (Ctrl+Alt+C)") {
            onLeftClick { createOrEditPerson(Person()) }
            clickShortcut = Ctrl + Alt + C
        }
        // the JPA list demo - shows all instances of a particular JPA entity, allow sorting and filtering
        personGrid = grid7(dataSource = personGridDS) {
            expandRatio = 1f; setSizeFull()
            cols {
                column(Person::id) {
                    isSortable = false
                }
                column(Person::name)
                column(Person::age)
                button("show", "Show", { PersonView.navigateTo(db { em.get<Person>(it.itemId) } ) })
                button("edit", "Edit", { createOrEditPerson(db { em.get<Person>(it.itemId) } ) })
                button("delete", "Delete", { deletePerson(it.itemId as Long) })
            }
            // automatically create filters, based on the types of values present in particular columns.
            appendHeaderRow().generateFilterComponents(this)
        }
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
        personGridDS.refresh()
    }
}
