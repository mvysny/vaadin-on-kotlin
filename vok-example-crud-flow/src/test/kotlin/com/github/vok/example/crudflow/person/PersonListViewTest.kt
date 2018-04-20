package com.github.vok.example.crudflow.person

import com.github.karibu.testing.*
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.vok.example.crudflow.Bootstrap
import com.github.vokorm.deleteAll
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
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
        UI.getCurrent().navigate("")
        _get<PersonListView>()
    }

    test("grid is refreshed when data is generated") {
        UI.getCurrent().navigate("")
        _get<Button> { caption = "Generate testing data" } ._click()
        expect(86) { _get<Grid<*>>()._size() }
    }
})
