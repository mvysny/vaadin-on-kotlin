package eu.vaadinonkotlin.vaadin8.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributesting.v8.getSuggestions
import com.github.mvysny.kaributesting.v8.setUserInput
import com.vaadin.ui.ComboBox
import eu.vaadinonkotlin.vaadin8.withFilter
import eu.vaadinonkotlin.vaadin8.withStringFilterOn
import kotlin.test.expect

class DataProvidersTest : DynaTest({
    group("ID retrieval") {
        test("entitydp") {
            expect(1L) { Person.dataProvider.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("entitydp with filter") {
            expect(1L) { Person.dataProvider.withFilter { Person::age eq 25 }.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("sqldp") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = { it.id!! }).getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
        test("sqldp with filter") {
            expect(1L) { sqlDataProvider(Person::class.java, "foo", idMapper = { it.id!! }).withFilter { Person::age eq 25 }.getId(Person(id = 1L, personName = "foo", age = 25)) }
        }
    }

    group("API test: populating combobox with data providers") {
        usingH2Database()
        // test that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
        // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
        test("entity data provider") {
            (0..10).forEach { Person( null, "foo $it", it).save() }
            val dp = Person.dataProvider
            val cb = ComboBox<Person>().apply {
                setItemCaptionGenerator { it.personName }
                setDataProvider(dp.withStringFilterOn(Person::personName))
            }
            expect((0..10).map { "foo $it" }) { cb.getSuggestions() }
            cb.setUserInput("foo 1")
            expectList("foo 1", "foo 10") { cb.getSuggestions() }
        }
        // tests that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
        // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
        test("sql data provider") {
            (0..10).forEach { Person( null, "foo $it", it).save() }
            val dp = sqlDataProvider(Person::class.java, "select * from Person where 1=1 {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}", idMapper = { it.id!! })
            val cb = ComboBox<Person>().apply {
                setItemCaptionGenerator { it.personName }
                setDataProvider(dp.withStringFilterOn("name"))
            }
            expect((0..10).map { "foo $it" }) { cb.getSuggestions() }
            cb.setUserInput("foo 1")
            expectList("foo 1", "foo 10") { cb.getSuggestions() }
        }
    }
})
