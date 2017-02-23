package com.github.vok.framework.vaadin

import com.github.vok.framework.TestPerson
import com.github.vok.framework.db
import com.github.vok.framework.deleteAll
import com.github.vok.framework.expectList
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import kotlinx.support.jdk8.streams.toList
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.expect

class JPADataProviderTest {

    @Before
    @After
    fun clearDb() {
        db { em.deleteAll<TestPerson>() }
    }

    @Test
    fun noEntitiesTest() {
        val ds = jpaDataProvider<TestPerson>()
        expect(0) { ds.size(Query()) }
        expect(false) { ds.isInMemory }
        expectList() { ds.fetch(Query()).toList() }
    }

    @Test
    fun sortingTest() {
        val ds = jpaDataProvider<TestPerson>()
        db { for (i in 15..90) em.persist(TestPerson(name = "test$i", age = i)) }
        expect(76) { ds.size(Query()) }
        expect((90 downTo 15).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.desc("age").build(), null, null)).toList().map { it.age!! } }
    }

    @Test
    fun filterTest() {
        db { for (i in 15..90) em.persist(TestPerson(name = "test$i", age = i)) }
        val ds = jpaDataProvider<TestPerson>().and { TestPerson::age between 30..60 }
        expect(31) { ds.size(Query()) }
        expect((30..60).toList()) { ds.fetch(Query(0, 100, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age!! } }
    }

    @Test
    fun pagingTest() {
        db { for (i in 15..90) em.persist(TestPerson(name = "test$i", age = i)) }
        val ds = jpaDataProvider<TestPerson>().and { TestPerson::age between 30..60 }
        expect((30..39).toList()) { ds.fetch(Query(0, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age!! } }
        expect((40..49).toList()) { ds.fetch(Query(10, 10, QuerySortOrder.asc("age").build(), null, null)).toList().map { it.age!! } }
    }
}
