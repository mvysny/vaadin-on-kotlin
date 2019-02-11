package example.crud_jpa.personeditor

import com.github.mvysny.kaributesting.v8.*
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v8.autoDiscoverViews
import com.vaadin.icons.VaadinIcons
import com.vaadin.ui.Button
import com.vaadin.ui.Grid
import com.vaadin.ui.TextField
import eu.vaadinonkotlin.vaadin8.jpa.db
import eu.vaadinonkotlin.vaadin8.jpa.deleteAll
import eu.vaadinonkotlin.vaadin8.jpa.findAll
import example.crud_jpa.Bootstrap
import example.crud_jpa.MyUI
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

/**
 * When called from a dyna test, it configures the test so that the app is properly bootstrapped and Vaadin is properly mocked.
 *
 * A demo of reusable test lifecycle; see https://github.com/mvysny/dynatest#patterns for details.
 */
fun DynaNodeGroup.usingApp() {
    beforeGroup {
        autoDiscoverViews("example")
        Bootstrap().contextInitialized(null)
    }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup({ MyUI() }) }
    afterEach { MockVaadin.tearDown() }

    fun cleanupDb() { db { em.deleteAll<Person>() } }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }
}

/**
 * Tests the [CrudView] class. Uses the browserless testing approach provided by the
 * [Karibu Testing](https://github.com/mvysny/karibu-testing) library - check that link for more details.
 */
class CrudViewTest : DynaTest({

    usingApp()

    test("the grid lists all personnel properly") {
        val person = Person(name = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        db { em.persist(person) }
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        grid.expectRows(1)
        grid.expectRow(0, person.id!!.toString(), "Duke Leto Atreides", "45", "1980-05-01", "Single", "false", "null", "Show", "Edit", "Delete")
    }

    test("edit one person") {
        db { em.persist(Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)) }
        CrudView.navigateTo()

        val grid = _get<Grid<*>>()
        grid._clickRenderer(0, "edit")

        // the CreateEditPerson dialog should pop up
        _get<TextField> { caption = "Name:" }.value = "Duke Leto Atreides"
        _get<Button> { caption = "Save" }._click()

        // assert the updated person
        expect(listOf("Duke Leto Atreides")) { db { em.findAll<Person>().map { it.name } } }
    }
})
