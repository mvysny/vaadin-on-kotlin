package com.vaadin.starter.beveragebuddy.ui

import com.github.karibu.testing.MockVaadin
import com.github.karibu.testing._get
import com.github.karibu.testing._size
import com.github.karibu.testing.autoDiscoverViews
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.grid.Grid
import com.vaadin.starter.beveragebuddy.Bootstrap
import com.vaadin.starter.beveragebuddy.backend.Category
import org.junit.*
import kotlin.test.expect

class CategoriesListTest {
    companion object {
        @JvmStatic @BeforeClass
        fun bootstrapApp() {
            Bootstrap().contextInitialized(null)
        }

        @JvmStatic @AfterClass
        fun teardownApp() {
            Bootstrap().contextDestroyed(null)
        }
    }

    @Before
    fun mockVaadin() {
        MockVaadin.setup(autoDiscoverViews("com.vaadin.starter"))
    }

    @Before @After
    fun cleanupDb() {
        Category.deleteAll()
    }

    @Test
    fun testGridListsAllPersons() {
        Category(name = "Beers").save()
        UI.getCurrent().navigateTo("categories")

        val grid = _get<Grid<*>>()
        expect(1) { grid.dataProvider._size() }
    }
}
