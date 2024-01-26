package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._findAll
import com.vaadin.flow.component.UI
import kotlin.test.expect

class HasDataViewUtilsTest : DynaTest({
    group("API test: populating components with data providers") {
        usingH2Database()
        beforeEach { MockVaadin.setup() }
        afterEach { MockVaadin.tearDown() }

        group("grid") {
            test("entity data provider") {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val cb = UI.getCurrent().grid<Person>(Person.dataProvider) {}
                expect((0..10).map { "foo $it" }) { cb._findAll().map { it.personName } }
            }
        }
    }
})
