package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import kotlin.streams.toList
import kotlin.test.expect

class FiltersTest : DynaTest({
    test("InMemoryFilters") {
        val list = (15..90).map { Person(name = "test$it", age = it) }
        val ds = ListDataProvider<Person>(list).and { Person::age between 30..60 }
        expect(31) { ds.size(Query()) }
        expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
    }
})
