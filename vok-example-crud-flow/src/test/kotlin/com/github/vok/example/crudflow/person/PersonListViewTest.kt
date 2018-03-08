package com.github.vok.example.crudflow.person

import com.github.karibu.testing.*
import com.github.mvysny.dynatest.DynaTest
import com.github.vok.example.crudflow.Bootstrap
import com.github.vokorm.deleteAll
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import kotlin.test.expect

class PersonListViewTest : DynaTest({
    beforeGroup {
        Bootstrap().contextInitialized(null)
    }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup(            autoDiscoverViews("com.github")) }
    fun cleanupDb() {
        Person.deleteAll()
    }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }

    test("Smoke test") {
        UI.getCurrent().navigate("")
        _get<PersonListView>()
    }

    test("grid is refreshed when data is generated") {
        UI.getCurrent().navigate("")
        _get<Button> { caption = "Generate testing data" } ._click()
        expect(86) { _get<Grid<*>>()._size() }
    }
})
