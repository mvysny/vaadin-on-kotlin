package com.github.vok.framework.sql2o.vaadin

import com.github.vokorm.*
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.EntityDataLoader
import com.github.vokorm.dataloader.SortClause
import com.vaadin.data.provider.AbstractBackEndDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.shared.data.sort.SortDirection
import java.util.stream.Stream

/**
 * Provides instances of entities of given [clazz] from a database. Does not support joins on any of the like; supports filtering
 * and sorting. Only supports simple views over one database table (one entity) - for anything more complex please use [SqlDataProvider].
 */
class EntityDataProvider<T : Entity<*>>(val clazz: Class<T>) : DataLoaderAdapter<T>(EntityDataLoader(clazz)) {
    override fun getId(item: T): Any = item.id!!
    override fun toString() = "EntityDataProvider($clazz)"
}

abstract class DataLoaderAdapter<T : Any>(val loader: DataLoader<T>) : AbstractBackEndDataProvider<T, Filter<T>?>() {
    override fun toString() = "DataLoaderAdapter($loader)"
    override fun sizeInBackEnd(query: Query<T, Filter<T>?>?): Int = loader.getCount(query?.filter?.orElse(null))
    override fun fetchFromBackEnd(query: Query<T, Filter<T>?>?): Stream<T> {
        val sortBy: List<SortClause> = query?.sortOrders?.map { SortClause(it.sorted, it.direction == SortDirection.ASCENDING) } ?: listOf()
        val offset = query?.offset ?: 0
        val limit = query?.limit ?: Int.MAX_VALUE
        var endInclusive = (limit.toLong() + offset - 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (endInclusive == Int.MAX_VALUE - 1) endInclusive = Int.MAX_VALUE
        val list = loader.fetch(query?.filter?.orElse(null), sortBy, offset..endInclusive)
        return list.stream()
    }
}

/**
 * Allows you to simply create a data provider off your entity: `grid.dataProvider = Person.dataProvider`. This data provider
 * doesn't support any joins or more complex queries; to use those please use [SqlDataProvider].
 */
inline val <reified T: Entity<*>> Dao<T>.dataProvider: VokDataProvider<T>
    get() = EntityDataProvider(T::class.java).withConfigurableFilter2()
