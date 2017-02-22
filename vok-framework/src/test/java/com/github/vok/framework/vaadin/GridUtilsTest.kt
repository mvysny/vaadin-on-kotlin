package com.github.vok.framework.vaadin

import com.github.vok.framework.TestPerson
import com.github.vok.framework.db
import com.github.vok.framework.deleteAll
import com.github.vok.framework.expectList
import com.vaadin.data.provider.Query
import kotlinx.support.jdk8.streams.toList
import org.junit.Before
import org.junit.Test
import kotlin.test.expect

class JPADataSourceTest {

    @Before
    fun clearDb() {
        db { em.deleteAll<TestPerson>() }
    }

    @Test
    fun noEntitiesTest() {
        val ds = jpaDataSource<TestPerson>()
        expect(0) { ds.size(Query()) }
        expect(false) { ds.isInMemory }
        expectList() { ds.fetch(Query()).toList() }
    }

    // @todo mavi add more tests for JPADataSource - sorting, paging, filtering

}