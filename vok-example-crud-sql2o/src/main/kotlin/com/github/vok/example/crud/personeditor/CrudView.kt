package com.github.vok.example.crud.personeditor

import com.github.vok.framework.sql2o.vaadin.dataProvider
import com.github.vok.framework.sql2o.vaadin.generateFilterComponentsSql
import com.github.vok.karibudsl.*
import com.github.vok.karibudsl.ModifierKey.Alt
import com.github.vok.karibudsl.ModifierKey.Ctrl
import com.github.vokorm.db
import com.vaadin.event.ShortcutAction.KeyCode.C
import com.vaadin.icons.VaadinIcons
import com.vaadin.navigator.View
import com.vaadin.server.FontIcon
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import com.vaadin.ui.renderers.LocalDateRenderer
import com.vaadin.ui.renderers.TextRenderer

/**
 * Demonstrates a CRUD over [Person]. Note how the autoViewProvider automatically discovers your view and assigns a name to it.
 */
@AutoView
class CrudView: VerticalLayout(), View {
    companion object {
        fun navigateTo() = navigateToView<CrudView>()
    }

    private lateinit var createButton: Button
    private val personGrid: Grid<Person>

    init {
        setSizeFull(); isMargin = false
        horizontalLayout {
            createButton = button("Create New Person (Ctrl+Alt+C)") {
                onLeftClick { createOrEditPerson(Person()) }
                clickShortcut = Ctrl + Alt + C
            }
            button("Generate testing data") {
                onLeftClick { generateTestingData() }
            }
        }
        // the SQL2O list demo - shows all instances of a particular database table, allows sorting and filtering.
        // you can restrict the values by writing the following expression:
        // dataProvider = Person.dataProvider.withFilter { Person::age between 20..60 }
        // any user-configured filters will be applied on top of this filter.
        personGrid = grid(dataProvider = Person.dataProvider) {
            expandRatio = 1f; setSizeFull()

            // we will also enable in-place editors; please see http://www.vaadinonkotlin.eu/grids.html for more info
            editor.isEnabled = true; editor.binder = beanValidationBinder()
            editor.addSaveListener { e -> e.bean.save(); refresh() }

            // a sample of how to reconfigure a column
            addColumnFor(Person::id) { isSortable = false }
            addColumnFor(Person::name) {
                setEditorComponent(TextField())
            }
            addColumnFor(Person::age) {
                // this demoes binding of a component which edits String (the TextField) to an Int-typed Person::age property
                editorBinding = this@grid.editor.binder.forField(TextField()).toInt().bind(Person::age)
                setStyleGenerator({ "v-align-right" })
            }
            addColumnFor(Person::dateOfBirth) {
                setRenderer(LocalDateRenderer())
                setEditorComponent(DateField())
            }
            addColumnFor(Person::maritalStatus) {
                setEditorComponent(ComboBox<MaritalStatus>(null, MaritalStatus.values().toList()))
            }
            addColumnFor(Person::alive) {
                setEditorComponent(CheckBox("Alive"))
            }
            addColumnFor(Person::created) {
                // example of a custom renderer which converts value to a displayable string.
                setRenderer({ it.toString() }, TextRenderer())
            }

            // add additional columns with buttons
            addButtonColumn(VaadinIcons.EXTERNAL_LINK, { event -> PersonView.navigateTo(event.item) })
            // the tests need to target and click the EDIT button, and hence we need to give an id to the column.
            addButtonColumn(VaadinIcons.EDIT, { event -> createOrEditPerson(event.item) }, "edit")
            addButtonColumn(VaadinIcons.TRASH, { event -> event.item.delete(); refresh() })

            // automatically create filters, based on the types of values present in particular columns.
            appendHeaderRow().generateFilterComponentsSql(this, Person::class)
        }
    }

    private fun generateTestingData() {
        db {
            (0..85).forEach {
                Person(name = "generated$it", age = it + 15, maritalStatus = MaritalStatus.Single, alive = true).save()
            }
        }
        personGrid.refresh()
    }

    private fun createOrEditPerson(person: Person) {
        CreateEditPerson(person).apply {
            addCloseListener { personGrid.refresh() }
            UI.getCurrent().addWindow(this)
        }
    }
}

/**
 * A helper function which adds a borderless button column with given [icon] to the grid. When clicked, [listener] is notified.
 * The column will optionally have given [id]
 */
fun <T> Grid<T>.addButtonColumn(icon: FontIcon, listener: (ClickableRenderer.RendererClickEvent<T>)->Unit, id: String? = null): Grid.Column<T, String> {
    val renderer = ButtonRenderer<T>(listener).apply { isHtmlContentAllowed = true }
    val html = icon.html
    val column = addColumn({ html }, renderer)
    column.setStyleGenerator { "borderless" }
    if (id != null) {
        column.id = id
    }
    return column
}
