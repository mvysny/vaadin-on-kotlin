@file:Suppress("UNCHECKED_CAST")

package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.vokdataloader.*
import eu.vaadinonkotlin.FilterFactory

/**
 * Produces filters defined by the `VoK-DataLoader` library. This will allow Vaadin Grid to piggyback on the DataLoader API
 * which provides access to REST (via the `vok-rest-client` module), the ability to use `VoK-ORM` to access SQL databases
 * (via the `vok-db` module). See the `vok-framework-v10-vokdb`'s `sqlDataProvider()` function and the `Dao.dataProvider` for more details.
 * @param T the bean of type filtered by [Filter].
 */
public open class DataLoaderFilterFactory<T : Any> : FilterFactory<Filter<T>> {
    override fun and(filters: Set<Filter<T>>): Filter<T>? = filters.and()
    override fun or(filters: Set<Filter<T>>): Filter<T>? = filters.or()
    override fun eq(propertyName: String, value: Any?): Filter<T> = EqFilter(propertyName, value)
    override fun le(propertyName: String, value: Any): Filter<T> = OpFilter(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any): Filter<T> = OpFilter(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String): Filter<T> = ILikeFilter(propertyName, value)
}
