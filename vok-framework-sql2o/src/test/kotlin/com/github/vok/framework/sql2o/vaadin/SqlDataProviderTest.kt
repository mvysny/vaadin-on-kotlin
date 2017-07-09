package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.AbstractDbTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.db
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import org.junit.Test
import kotlin.streams.toList
import kotlin.test.expect

class JPADataProviderTest : AbstractDbTest() {

    @Test
    fun noEntitiesTest() {
        val ds = Person.dataProvider
        expect(0) { ds.size(Query()) }
        expect(false) { ds.isInMemory }
        expectList() { ds.fetch(Query()).toList() }
    }

    @Test
    fun sortingTest() {
        val ds = Person.dataProvider
        db { for (i in 15..90) Person(name = "test$i", age = i).save() }
        expect(76) { ds.size(Query()) }
        expect((90 downTo 15).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.desc("age").build(), null, null)).toList().map { it.age } }
    }

    @Test
    fun filterTest1() {
        db { for (i in 15..90) Person(name = "test$i", age = i).save() }
        val ds = Person.dataProvider.and { Person::age between 30..60 }
        expect(31) { ds.size(Query()) }
        expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
    }

    @Test
    fun filterTest2() {
        db { for (i in 15..90) Person(name = "test$i", age = i).save() }
        val ds = Person.dataProvider
        val filter = filter<Person> { Person::age between 30..60 }
        expect(31) { ds.size(Query(filter)) }
        expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, filter)).toList().map { it.age } }
    }

    @Test
    fun pagingTest() {
        db { for (i in 15..90) Person(name = "test$i", age = i).save() }
        val ds = Person.dataProvider.and { Person::age between 30..60 }
        expect((30..39).toList()) { ds.fetch(Query(0, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
        expect((40..49).toList()) { ds.fetch(Query(10, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age } }
    }

    @Test
    fun nativeQueryTest() {
        db { for (i in 15..90) Person(name = "test$i", age = i).save() }
        val ds = Person.dataProvider.and { Person::age lt 60 and "age > :age"("age" to 29)}
        expect((30..59).toList()) { ds.fetch(Query()).toList().map { it.age } }
    }
}

/**
 * Expects that [actual] list of objects matches [expected] list of objects. Fails otherwise.
 * @param expected expected list of objects
 * @param actual actual list of objects
 */
fun <T> expectList(vararg expected: T, actual: ()->List<T>) = expect(expected.toList(), actual)
