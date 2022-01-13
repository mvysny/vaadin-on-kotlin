package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._findAll
import com.github.mvysny.kaributesting.v10.getSuggestions
import com.github.mvysny.kaributesting.v10.setUserInput
import com.github.vokorm.dataloader.SqlDataLoader
import com.github.vokorm.dataloader.dataLoader
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.grid.Grid
import kotlin.test.expect

class DataProvidersTest : DynaTest({
    group("API test: populating components with data providers") {
        usingH2Database()
        beforeEach { MockVaadin.setup() }
        afterEach { MockVaadin.tearDown() }

        group("combobox") {
            // test that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
            // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
            test("entity data provider") {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val dp = Person.dataLoader
                val cb = ComboBox<Person>().apply {
                    setItemLabelGenerator { it.personName }
                    setDataProvider(dp.withStringFilterOn(Person::personName))
                }
                expect((0..10).map { "foo $it" }) { cb.getSuggestions() }
                cb.setUserInput("foo 1")
                expectList("foo 1", "foo 10") { cb.getSuggestions() }
            }

            // tests that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
            // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
            test("sql data provider") {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val dp = SqlDataLoader(Person::class.java, "select * from Test where 1=1 {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}")
                val cb = ComboBox<Person>().apply {
                    setItemLabelGenerator { it.personName }
                    setDataProvider(dp.withStringFilterOn("name"))
                }
                expect((0..10).map { "foo $it" }) { cb.getSuggestions() }
                cb.setUserInput("foo 1")
                expectList("foo 1", "foo 10") { cb.getSuggestions() }
            }
        }

        group("grid") {
            // test that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
            // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
            test("entity data provider") {
                (0..10).forEach { Person(null, "foo $it", it).save() }
                val cb: Grid<Person> = UI.getCurrent().grid<Person> {
                    setDataLoader(Person.dataLoader)
                }
                expect((0..10).map { "foo $it" }) { cb._findAll().map { it.personName } }
            }
        }
    }
})
