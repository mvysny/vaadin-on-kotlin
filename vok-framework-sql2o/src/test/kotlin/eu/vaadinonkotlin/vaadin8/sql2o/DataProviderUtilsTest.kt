package eu.vaadinonkotlin.vaadin8.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v8.asc
import com.github.mvysny.karibudsl.v8.desc
import com.github.mvysny.karibudsl.v8.getAll
import com.github.mvysny.vokdataloader.Filter
import com.github.mvysny.vokdataloader.buildFilter
import com.github.vokorm.db
import com.vaadin.data.provider.AbstractBackEndDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import eu.vaadinonkotlin.vaadin8.withFilter
import java.util.stream.Stream
import kotlin.streams.toList
import kotlin.test.expect

class DataProviderUtilsTest : DynaTest({
    usingH2Database()

    group("withFilter") {
        test("basic test") {
            db { (15..90).forEach { Person(personName = "test$it", age = it).save() } }
            val ds = Person.dataProvider.withFilter { Person::age between 30..60 }
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        }

        test("can't remove filter set by the withFilter() call") {
            db { (15..90).forEach { Person(personName = "test$it", age = it).save() } }
            val ds = Person.dataProvider.withFilter { Person::age between 30..60 }
            ds.setFilter(null)
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.getAll().map { it.age } }
        }

        test("setting a filter to a DP returned by withFilter() will AND with the previous one") {
            db { (15..90).forEach { Person(personName = "test$it", age = it).save() } }
            val ds = Person.dataProvider.withFilter { Person::age between 30..60 }
            ds.setFilter(buildFilter { Person::age between 15..40 })
            expect(11) { ds.size(Query()) }
            expect((30..40).toList()) { ds.getAll().map { it.age } }

            // this must overwrite the previously set filter
            ds.setFilter(buildFilter { Person::age between 15..35 })
            expect(6) { ds.size(Query()) }
            expect((30..35).toList()) { ds.getAll().map { it.age } }
        }
    }
})
