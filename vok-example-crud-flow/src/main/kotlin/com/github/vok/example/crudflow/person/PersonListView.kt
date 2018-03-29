package com.github.vok.example.crudflow.person

import com.github.vok.framework.sql2o.vaadin.dataProvider
import com.github.vok.framework.sql2o.vaadin.generateFilterComponents
import com.github.vok.framework.toDate
import com.github.vok.karibudsl.flow.*
import com.github.vokorm.db
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.BodySize
import com.vaadin.flow.component.page.Viewport
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import java.time.LocalDate

/**
 * The main view contains a button and a template element.
 */
@BodySize(width = "100vw", height = "100vh")
@Route("")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(Lumo::class)
class PersonListView : VerticalLayout() {
    private val personGrid: Grid<Person>
    init {
        setSizeFull()
        h2("It works!")
        button("Generate testing data") {
            onLeftClick {
                generateTestingData()
            }
        }
        personGrid = grid(dataProvider = Person.dataProvider) {
            flexGrow = 1.0
            addColumnFor(Person::id)
            addColumnFor(Person::name)
            addColumnFor(Person::age)
            addColumnFor(Person::alive)
            addColumnFor(Person::dateOfBirth, converter = { it?.toString() })
            addColumnFor(Person::maritalStatus)
            addColumnFor(Person::created, converter = { it?.toString() })

            generateFilterComponents(Person::class)
        }
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
