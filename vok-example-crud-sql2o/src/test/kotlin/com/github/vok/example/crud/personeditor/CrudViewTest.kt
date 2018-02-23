package com.github.vok.example.crud.personeditor

import com.github.karibu.testing.*
import com.github.mvysny.dynatest.DynaTest
import com.github.vok.example.crud.Bootstrap
import com.github.vok.example.crud.MyUI
import com.github.vokorm.deleteAll
import com.github.vokorm.findAll
import com.vaadin.ui.Button
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField
import java.time.LocalDate
import kotlin.test.expect

class CrudViewTest : DynaTest({
    beforeGroup {
        autoDiscoverViews("com.github")
        Bootstrap().contextInitialized(null)
    }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup({ MyUI() }) }
    fun cleanupDb() {
        Person.deleteAll()
    }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }

    test("GridListsAllPersons") {
        Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        grid.expectRows(1)
        grid.expectRow(0, "1", "Duke Leto Atreides", "45", "1980-05-01", "Single", "false", "null", "Show", "Edit", "Delete")
    }

    test("Edit") {
        Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        grid._clickRenderer(0, "edit")

        // the CreateEditPerson dialog should pop up
        _get<TextField> { caption = "Name:" }.value = "Duke Leto Atreides"
        _get<Button> { caption = "Save" }._click()

        // assert the updated person
        expect(listOf("Duke Leto Atreides")) { Person.findAll().map { it.name } }
    }
})
