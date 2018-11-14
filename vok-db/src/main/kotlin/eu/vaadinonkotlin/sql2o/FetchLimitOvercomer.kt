package eu.vaadinonkotlin.sql2o

import com.github.vokorm.Filter
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.SortClause

val IntRange.length: Int get() = if (isEmpty()) 0 else endInclusive - start + 1

/**
 * Overcomes a natural limit of the number of items which [delegate] can fetch (via [DataLoader.fetch]). If more items are requested,
 * this data loader polls delegate multiple times and reconstructs the result. Ideal for REST endpoints which are often
 * limited in how many items they can fetch.
 * @property delegate the data loader which imposes limit on the number of data fetched
 * @property delegateFetchLimit the known limit of items which the delegate can fetch, 1 or more.
 */
class FetchLimitOvercomer<T: Any>(val delegate: DataLoader<T>, val delegateFetchLimit: Int) : DataLoader<T> {
    init {
        require(delegateFetchLimit >= 1) { "delegateFetchLimit should be 1 or more but was $delegateFetchLimit" }
    }
    override fun toString() = "FetchLimitOvercomer(delegateFetchLimit=$delegateFetchLimit, delegate=$delegate)"
    override fun fetch(filter: Filter<T>?, sortBy: List<SortClause>, range: IntRange): List<T> {
        val result = mutableListOf<T>()
        while(result.size < range.length) {
            val itemsToFetch = (range.length - result.size).coerceAtMost(delegateFetchLimit)
            val offset = range.start + result.size
            val fetchRange = offset until (offset + itemsToFetch)
            val fetched = delegate.fetch(filter, sortBy, fetchRange)
            result.addAll(fetched)
            if (fetched.size < itemsToFetch) {
                // we probably hit an end of this data loader, bail out
                break
            }
        }
        return result
    }
    override fun getCount(filter: Filter<T>?): Int = delegate.getCount(filter)
}

/**
 * Overcomes a natural limit of the number of items which [this] can fetch (via [DataLoader.fetch]). If more items are requested,
 * the returned data loader polls [this] multiple times and reconstructs the result. Ideal for REST endpoints which are often
 * limited in how many items they can fetch.
 * @param delegateFetchLimit the known limit of items which the delegate can fetch, 1 or more.
 */
fun <T: Any> DataLoader<T>.overcomeFetchLimit(delegateFetchLimit: Int): DataLoader<T> = FetchLimitOvercomer(this, delegateFetchLimit)
