package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.db
import com.github.vok.framework.sql2o.withDatabase
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import kotlin.streams.toList
import kotlin.test.expect

class EntityDataProviderTest : DynaTest({
    withDatabase {
        test("empty data provider") {
            val ds = Person.dataProvider
            expect(0) { ds.size(Query()) }
            expect(false) { ds.isInMemory }
            expectList() { ds.getAll() }
        }

        test("sorting") {
            val ds = Person.dataProvider
            db { for (i in 15..90) Person(name = "test$i", age = i).save() }
            expect(76) { ds.size(Query()) }
            expect((90 downTo 15).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.desc("age").build(), null, null)).toList().map { it.age } }
        }

        test("filterTest1") {
            db { for (i in 15..90) Person(name = "test$i", age = i).save() }
            val ds = Person.dataProvider.and { Person::age between 30..60 }
            expect(31) { ds.size(Query()) }
            expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        }

        test("filterTest2") {
            db { for (i in 15..90) Person(name = "test$i", age = i).save() }
            val ds = Person.dataProvider
            val filter = filter<Person> { Person::age between 30..60 }
            expect(31) { ds.size(Query(filter)) }
            expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, filter)).toList().map { it.age } }
        }

        test("paging") {
            db { for (i in 15..90) Person(name = "test$i", age = i).save() }
            val ds = Person.dataProvider.and { Person::age between 30..60 }
            expect((30..39).toList()) { ds.fetch(Query(0, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
            expect((40..49).toList()) { ds.fetch(Query(10, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        }

        test("native query") {
            db { for (i in 15..90) Person(name = "test$i", age = i).save() }
            val ds = Person.dataProvider.and { Person::age lt 60 and "age > :age"("age" to 29)}
            expect((30..59).toList()) { ds.getAll().map { it.age } }
        }
    }
})
