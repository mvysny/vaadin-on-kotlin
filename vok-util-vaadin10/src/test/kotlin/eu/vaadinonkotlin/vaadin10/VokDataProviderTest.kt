package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.asc
import com.github.mvysny.karibudsl.v10.desc
import com.github.mvysny.vokdataloader.Filter
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import java.util.stream.Stream
import kotlin.test.expect

class VokDataProviderTest : DynaTest({
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
})
