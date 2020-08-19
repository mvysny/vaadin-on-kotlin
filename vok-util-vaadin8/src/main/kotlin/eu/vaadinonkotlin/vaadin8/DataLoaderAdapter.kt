package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.vokdataloader.DataLoader
import com.github.mvysny.vokdataloader.Filter
import com.github.mvysny.vokdataloader.SortClause
import com.vaadin.data.provider.AbstractBackEndDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.shared.data.sort.SortDirection
import java.util.stream.Stream

/**
 * A Vaadin [com.vaadin.data.provider.DataProvider] implementation which delegates the data-fetching calls to a VoK-ORM [DataLoader];
 * an adapter which adapts calls to Vaadin DataProvider to VoK-ORM DataLoader.
 * @param T the type of items returned by this data provider.
 * @param loader performs the actual data fetching. All data-fetching calls are delegated here.
 * @param idResolver provides unique ID for every item. The ID is then used to differentiate items.
 * See [com.vaadin.data.provider.DataProvider.getId] for more details. Typically every item
 * has a primary key of type [Long], but any Java/Kotlin object with properly written [Any.equals] and [Any.hashCode] can act as the ID,
 * including the item itself.
 */
public class DataLoaderAdapter<T : Any>(public val loader: DataLoader<T>, private val idResolver: (T)->Any) : AbstractBackEndDataProvider<T, Filter<T>?>() {
    override fun getId(item: T): Any = idResolver(item)
    override fun toString(): String = "DataLoaderAdapter($loader)"
    override fun sizeInBackEnd(query: Query<T, Filter<T>?>?): Int {
        val count: Long = loader.getCount(query?.filter?.orElse(null))
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    override fun fetchFromBackEnd(query: Query<T, Filter<T>?>?): Stream<T> {
        val sortBy: List<SortClause> = query?.sortOrders?.map {
            SortClause(it.sorted, it.direction == SortDirection.ASCENDING)
        } ?: listOf()
        val offset = query?.offset ?: 0
        val limit = query?.limit ?: Int.MAX_VALUE
        var endInclusive = (limit.toLong() + offset - 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (endInclusive == Int.MAX_VALUE - 1) endInclusive = Int.MAX_VALUE
        val list = loader.fetch(query?.filter?.orElse(null), sortBy, offset.toLong()..endInclusive.toLong())
        return list.stream()
    }
}

/**
 * Returns a [VokDataProvider] which loads data from this [DataLoader].
 * @param idResolver provides unique ID for every item. The ID is then used to differentiate items.
 * See [com.vaadin.data.provider.DataProvider.getId] for more details. Typically every item
 * has a primary key of type [Long], but any Java/Kotlin object with properly written [Any.equals] and [Any.hashCode] can act as the ID,
 * including the item itself.
 */
public fun <T: Any> DataLoader<T>.asDataProvider(idResolver: (T) -> Any): VokDataProvider<T> =
        DataLoaderAdapter(this, idResolver).withConfigurableFilter2()
