package com.github.vok.example.crudflow.person

import com.github.karibu.testing.v10.*
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.vok.example.crudflow.Bootstrap
import com.github.vokorm.deleteAll
import com.github.vokorm.findAll
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import java.time.LocalDate
import kotlin.test.expect

/**
 * When called from a dyna test, it configures the test so that the app is properly bootstrapped and Vaadin is properly mocked.
 *
 * A demo of reusable test lifecycle; see https://github.com/mvysny/dynatest#patterns for details.
 */
fun DynaNodeGroup.usingApp() {
    beforeGroup { Bootstrap().contextInitialized(null) }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup(Routes().autoDiscoverViews("com.github")) }
    fun cleanupDb() { Person.deleteAll() }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }
}

class PersonListViewTest : DynaTest({

    usingApp()

    test("Smoke test") {
        _get<PersonListView>()
    }

    test("grid is refreshed when data is generated") {
        _get<Button> { caption = "Generate testing data" } ._click()

        val grid = _get<Grid<*>>()
        grid.expectRows(86)
        // unfortunately since we're using Renderer to render Age, we won't see the "Age" value here because of
        // https://github.com/vaadin/vaadin-grid-flow/issues/197
        grid.expectRow(0, "1", "generated0", "null", "true", "1990-01-01", "Single", "2011-01-01T00:00:00Z", "null", "null", "null")
    }

    test("edit one person") {
        Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()

        _get<Grid<*>>()
        // currently not possible: https://github.com/mvysny/karibu-testing/issues/2
/*
        grid._clickRenderer(0, "edit")

        // the CreateEditPerson dialog should pop up
        _get<TextField> { caption = "Name:" }.value = "Duke Leto Atreides"
        _get<Button> { caption = "Save" }._click()

        // assert the updated person
        expect(listOf("Duke Leto Atreides")) { Person.findAll().map { it.name } }
*/
    }
})
