package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.kaributesting.v10._findAll
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

class HasDataViewUtilsTest {
    @Nested inner class `API test - populating components with data providers`() : AbstractVaadinDbTest() {
        @Nested inner class GridTests {
            @Test fun `entity data provider`() {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val cb = UI.getCurrent().grid<Person>(Person.dataProvider) {}
                expect((0..10).map { "foo $it" }) { cb._findAll().map { it.personName } }
            }
        }
    }
}
