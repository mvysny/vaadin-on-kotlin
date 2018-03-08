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
import kotlin.reflect.KProperty1

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
        personGrid = grid {
            flexGrow = 1.0
            dataProvider = Person.dataProvider.withConfigurableFilter()
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

/**
 * Adds a column for given [property]. The column key is set to the property name, so that you can look up the column
 * using [getColumnBy]. The column is also by default set to sortable
 * unless the [sortable] parameter is set otherwise.
 * @param block runs given block on the column.
 * @return the newly created column
 */
fun <T, V : Comparable<V>> Grid<T>.addColumnFor(property: KProperty1<T, V?>,
                                                sortable: Boolean = true,
                                                converter: (V?)->Any? = { it },
                                                block: Grid.Column<T>.() -> Unit = {}): Grid.Column<T> =
    addColumn { it: T -> converter(property.get(it)) } .apply {
        key = property.name
        if (sortable) {
            sortProperty = property
        }
        setHeader(property.name)
        block()
    }

