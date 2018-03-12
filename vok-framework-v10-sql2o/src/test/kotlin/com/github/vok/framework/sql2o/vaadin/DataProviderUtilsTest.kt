package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.karibudsl.flow.getAll
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import kotlin.test.expect
import kotlin.streams.*

class DataProviderUtilsTest : DynaTest({
    group("withFilter") {
        test("InMemoryFilters") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        }

        test("can't change filter provided by withFilter") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            ds.setFilter(null)
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.getAll().toList().map { it.age } }
        }

        test("new filter with AND with the previous one") {
            val list = (15..90).map { Person(name = "test$it", age = it) }
            val ds = ListDataProvider<Person>(list).withFilter { Person::age between 30..60 }
            ds.setFilter(filter { Person::age between 15..40 })
            expect(11) { ds.size(Query()) }
            expect((30..40).toList()) { ds.getAll().toList().map { it.age } }
        }
    }
})
