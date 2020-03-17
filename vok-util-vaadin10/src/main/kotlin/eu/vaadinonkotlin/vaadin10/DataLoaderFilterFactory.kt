@file:Suppress("UNCHECKED_CAST")

package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.vokdataloader.*
import eu.vaadinonkotlin.FilterFactory

/**
 * Produces filters defined by the `VoK-DataLoader` library. This will allow Vaadin Grid to piggyback on the DataLoader API
 * which provides access to REST (via the `vok-rest-client` module), the ability to use `VoK-ORM` to access SQL databases
 * (via the `vok-db` module). See the `vok-framework-v10-vokdb`'s `sqlDataProvider()` function and the `Dao.dataProvider` for more details.
 */
class DataLoaderFilterFactory<F : Any> : FilterFactory<Filter<F>> {
    override fun and(filters: Set<Filter<F>>) = filters.and()
    override fun or(filters: Set<Filter<F>>) = filters.or()
    override fun eq(propertyName: String, value: Any?) = EqFilter<F>(propertyName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<F>(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<F>(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<F>(propertyName, value)
}
