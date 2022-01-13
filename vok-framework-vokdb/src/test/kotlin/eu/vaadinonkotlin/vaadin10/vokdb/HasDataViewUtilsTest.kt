package eu.vaadinonkotlin.vaadin10.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._findAll
import com.github.vokorm.dataloader.dataLoader
import com.vaadin.flow.component.UI
import eu.vaadinonkotlin.vaadin10.vokdb.setDataLoader
import kotlin.test.expect

class HasDataViewUtilsTest : DynaTest({
    group("API test: populating components with data providers") {
        usingH2Database()
        beforeEach { MockVaadin.setup() }
        afterEach { MockVaadin.tearDown() }

        group("grid") {
            // test that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
            // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
            test("entity data provider") {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val cb = UI.getCurrent().grid<Person> {
                    setDataLoader(Person.dataLoader)
                }
                expect((0..10).map { "foo $it" }) { cb._findAll().map { it.personName } }
            }
        }
    }
})
