package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.karibudsl.getAll
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import kotlin.streams.toList
import kotlin.test.expect

class DataProviderUtilsTest : DynaTest({
    group("withFilter") {
        test("basic test") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        }

        test("can't remove filter set by the withFilter() call") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            ds.setFilter(null)
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.getAll().toList().map { it.age } }
        }

        test("setting a filter to a DP returned by withFilter() will AND with the previous one") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            ds.setFilter(filter { Person::age between 15..40 })
            expect(11) { ds.size(Query()) }
            expect((30..40).toList()) { ds.getAll().toList().map { it.age } }

            // this must overwrite the previously set filter
            ds.setFilter(filter { Person::age between 15..35 })
            expect(6) { ds.size(Query()) }
            expect((30..35).toList()) { ds.getAll().toList().map { it.age } }
        }
    }
})
