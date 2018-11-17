package eu.vaadinonkotlin.vaadin10.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.vokdataloader.*
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
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
        // this test does not test a functionality; it rather tests the API itself whether the API is simple to use.
        test("entity data provider") {
            val dp = Person.dataProvider
            ComboBox<Person>().apply {
                setItemLabelGenerator { it.personName }
                // currently there is no way to filter server-side based on combobox contents:
                // https://github.com/vaadin/vaadin-combo-box-flow/issues/17
                // https://github.com/vaadin/vaadin-combo-box-flow/issues/72
                setDataProvider(dp)
            }
        }
        // tests that the EntityDataProvider and SqlDataProviders are compatible with Vaadin ComboBox
        // since ComboBox emits String as a filter (it emits whatever the user typed into the ComboBox).
        // this test does not test a functionality; it rather tests the API itself whether the API is simple to use.
        test("sql data provider") {
            val dp = sqlDataProvider(Person::class.java, "select * from Test where 1=1 {{WHERE}} order by 1=1{{ORDER}} {{PAGING}}", idMapper = { it.id!! })
            ComboBox<Person>().apply {
                setItemLabelGenerator { it.personName }
                // currently there is no way to filter server-side based on combobox contents:
                // https://github.com/vaadin/vaadin-combo-box-flow/issues/17
                // https://github.com/vaadin/vaadin-combo-box-flow/issues/72
                setDataProvider(dp)
            }
        }
    }
})
