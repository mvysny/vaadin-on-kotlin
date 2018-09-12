package com.github.vok.framework.sql2o.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.vok.framework.sql2o.Person
import com.github.vok.framework.sql2o.usingH2Database
import com.github.vok.karibudsl.flow.asc
import com.github.vok.karibudsl.flow.desc
import com.github.vok.karibudsl.flow.getAll
import com.github.vokorm.Filter
import com.github.vokorm.buildFilter
import com.github.vokorm.db
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import java.util.stream.Stream
import kotlin.test.expect
import kotlin.streams.*

class DataProviderUtilsTest : DynaTest({
    usingH2Database()

    group("withFilter") {
        test("InMemoryFilters") {
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
            expect((30..60).toList()) { ds.getAll().toList().map { it.age } }
        }

        test("setting a filter to a DP returned by withFilter() will AND with the previous one") {
            db { (15..90).forEach { Person(personName = "test$it", age = it).save() } }
            val ds = Person.dataProvider.withFilter { Person::age between 30..60 }
            ds.setFilter(buildFilter { Person::age between 15..40 })
            expect(11) { ds.size(Query()) }
            expect((30..40).toList()) { ds.getAll().toList().map { it.age } }

            // this must overwrite the previously set filter
            ds.setFilter(buildFilter { Person::age between 15..35 })
            expect(6) { ds.size(Query()) }
            expect((30..35).toList()) { ds.getAll().toList().map { it.age } }
        }

        group("sortedBy") {
            class FetchAssert(val expectedSortOrders: List<QuerySortOrder>) : AbstractBackEndDataProvider<Person, Filter<Person>?>() {
                override fun sizeInBackEnd(query: Query<Person, Filter<Person>?>?): Int = 0
                override fun fetchFromBackEnd(query: Query<Person, Filter<Person>?>): Stream<Person> {
                    expect(expectedSortOrders.joinToString { "${it.sorted}${it.direction}" }) { query.sortOrders.joinToString { "${it.sorted}${it.direction}" } }
                    return listOf<Person>().stream()
                }
            }

            test("null Query.sortOrders") {
                FetchAssert(listOf(Person::age.desc))
                    .withConfigurableFilter2()
                    .sortedBy(Person::age.desc)
                    .fetch(Query(0, 0, null, null, null))
            }

            test("empty Query.sortOrders") {
                FetchAssert(listOf(Person::age.desc))
                    .withConfigurableFilter2()
                    .sortedBy(Person::age.desc)
                    .fetch(Query(0, 0, listOf(), null, null))
            }

            test("Query.sortOrders take priority") {
                FetchAssert(listOf(Person::created.asc, Person::age.desc))
                    .withConfigurableFilter2()
                    .sortedBy(Person::age.desc)
                    .fetch(Query(0, 0, listOf(Person::created.asc), null, null))
            }
        }
    }
})
