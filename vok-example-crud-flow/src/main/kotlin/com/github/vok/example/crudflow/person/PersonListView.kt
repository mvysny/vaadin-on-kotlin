package com.github.vok.example.crudflow.person

import com.github.vok.example.crudflow.MainLayout
import com.github.vok.framework.sql2o.vaadin.dataProvider
import com.github.vok.framework.sql2o.vaadin.generateFilterComponents
import com.github.vok.framework.toDate
import com.github.vok.karibudsl.flow.*
import com.github.vokorm.db
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.icon.VaadinIcons
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.BodySize
import com.vaadin.flow.component.page.Viewport
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.NativeButtonRenderer
import com.vaadin.flow.data.renderer.Renderer
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import java.time.LocalDate

/**
 * The main view contains a button and a template element.
 */
@Route("", layout = MainLayout::class)
class PersonListView : VerticalLayout() {
    private val personGrid: Grid<Person>
    init {
        setSizeFull()
        h4("Person list")
        button("Generate testing data") {
            onLeftClick {
                generateTestingData()
            }
        }
        personGrid = grid(dataProvider = Person.dataProvider) {
            flexGrow = 1.0
            addColumnFor(Person::id, sortable = false) {
                width = "90px"; flexGrow = 0
            }
            addColumnFor(Person::name)
            addColumnFor(Person::age, center({it.age?.toString()})) {
                width = "120px"; flexGrow = 0
            }
            addColumnFor(Person::alive) {
                width = "130px"; flexGrow = 0
            }
            addColumnFor(Person::dateOfBirth, converter = { it?.toString() })
            addColumnFor(Person::maritalStatus) {
                width = "160px"; flexGrow = 0
            }
            addColumnFor(Person::created, converter = { it!!.toInstant().toString() })

            addColumn(NativeButtonRenderer<Person>("View", { person -> navigateToView<Long, PersonView>(person.id!!) })).apply {
                width = "90px"; flexGrow = 0
            }
            addColumn(NativeButtonRenderer<Person>("Edit", { person -> createOrEditPerson(person) })).apply {
                width = "90px"; flexGrow = 0
            }
            addColumn(NativeButtonRenderer<Person>("Delete", { person -> person.delete(); refresh() })).apply {
                width = "90px"; flexGrow = 0
            }

            appendHeaderRow().generateFilterComponents(this, Person::class)
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
