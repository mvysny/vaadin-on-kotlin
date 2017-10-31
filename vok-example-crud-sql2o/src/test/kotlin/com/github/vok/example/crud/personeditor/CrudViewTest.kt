package com.github.vok.example.crud.personeditor

import com.github.karibu.testing.MockVaadin
import com.github.karibu.testing._click
import com.github.karibu.testing._get
import com.github.karibu.testing.autoDiscoverViews
import com.github.vok.example.crud.Bootstrap
import com.github.vok.example.crud.MyUI
import com.github.vok.framework.sql2o.deleteAll
import com.github.vok.framework.sql2o.findAll
import com.vaadin.ui.Button
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField
import org.junit.*
import java.time.LocalDate
import kotlin.test.expect

class CrudViewTest {
    companion object {
        @JvmStatic @BeforeClass
        fun bootstrapApp() {
            autoDiscoverViews("com.github")
            Bootstrap().contextInitialized(null)
        }

        @JvmStatic @AfterClass
        fun teardownApp() {
            Bootstrap().contextDestroyed(null)
        }
    }

    @Before
    fun mockVaadin() {
        MockVaadin.setup({ -> MyUI() })
    }

    @Before @After
    fun cleanupDb() {
        Person.deleteAll()
    }

    @Test
    fun testGridListsAllPersons() {
        Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        expect(1) { grid.dataProvider._size() }
    }

    @Test
    fun testEdit() {
        Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        grid._clickRenderer(0, "edit")

        // the CreateEditPerson dialog should pop up
        _get<TextField>(caption = "Name:").value = "Duke Leto Atreides"
        _get<Button>(caption = "Save")._click()

        // assert the updated person
        expect(listOf("Duke Leto Atreides")) { Person.findAll().map { it.name } }
    }
}
