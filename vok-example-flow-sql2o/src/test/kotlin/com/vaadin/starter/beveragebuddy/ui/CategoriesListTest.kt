package com.vaadin.starter.beveragebuddy.ui

import com.github.karibu.testing.MockVaadin
import com.github.karibu.testing._get
import com.github.karibu.testing._size
import com.github.karibu.testing.autoDiscoverViews
import com.github.mvysny.dynatest.DynaTest
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.grid.Grid
import com.vaadin.starter.beveragebuddy.Bootstrap
import com.vaadin.starter.beveragebuddy.backend.Category
import org.junit.*
import kotlin.test.expect

class CategoriesListTest : DynaTest({
    beforeGroup { Bootstrap().contextInitialized(null) }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup(autoDiscoverViews("com.vaadin.starter")) }
    fun cleanupDb() {
        Category.deleteAll()
    }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }

    test("GridListsAllPersons") {
        Category(name = "Beers").save()
        UI.getCurrent().navigateTo("categories")

        val grid = _get<Grid<*>>()
        expect(1) { grid.dataProvider._size() }
    }
})
