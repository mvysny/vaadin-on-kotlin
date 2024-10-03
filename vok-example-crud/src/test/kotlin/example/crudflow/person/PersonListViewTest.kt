package example.crudflow.person

import com.github.mvysny.kaributesting.v10.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import example.crudflow.AbstractAppTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonListViewTest : AbstractAppTest() {
    @Test fun smokeTest() {
        _get<PersonListView>()
    }

    @Test fun `grid is refreshed when data is generated`() {
        _get<Button> { text = "Generate testing data (Alt+G)" } ._click()

        val grid = _get<Grid<*>>()
        grid.expectRows(86)
        val first = Person.findAll()[0]
        // unfortunately since we're using Renderer to render Age, we won't see the "Age" value here because of
        // https://github.com/vaadin/vaadin-grid-flow/issues/197
        grid.expectRow(0, "Button[icon='vaadin:eye', @theme='small icon tertiary']", "Button[icon='vaadin:edit', @theme='small icon tertiary']", "Button[icon='vaadin:trash', @theme='small icon tertiary']", first.id!!.toString(), "generated0", "15", "true", "1990-01-01", "Single", "2010-12-31T22:00:00Z")
    }

    @Test fun `edit one person`() {
        Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()

        val grid = _get<Grid<Person>>()
        grid.expectRows(1)
        grid._clickRenderer(0, "edit")

        // the CreateEditPerson dialog should pop up
        _get<TextField> { label = "Name:" }.value = "Duke Leto Atreides"
        _get<Button> { text = "Save" }._click()

        // assert the updated person
        expectList("Duke Leto Atreides") { Person.findAll().map { it.name } }
    }

    @Test fun `delete one person`() {
        Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
        val grid = _get<Grid<Person>>()
        grid.expectRows(1)
        grid._clickRenderer(0, "delete")
        expectList() { Person.findAll() }
        grid.expectRows(0)
    }

    @Nested inner class ContextMenuTests {
        @Test fun `edit one person`() {
            Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()

            val grid = _get<Grid<Person>>()
            grid.expectRows(1)
            _get<PersonListView>().gridContextMenu._clickItemWithCaption("edit", grid._get(0))

            // the CreateEditPerson dialog should pop up
            _get<TextField> { label = "Name:" }.value = "Duke Leto Atreides"
            _get<Button> { text = "Save" }._click()

            // assert the updated person
            expectList("Duke Leto Atreides") { Person.findAll().map { it.name } }
        }

        @Test fun `delete one person`() {
            Person(name = "Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false).save()
            val grid = _get<Grid<Person>>()
            grid.expectRows(1)
            _get<PersonListView>().gridContextMenu._clickItemWithCaption("delete", grid._get(0))
            expectList() { Person.findAll() }
            grid.expectRows(0)
        }
    }
}
