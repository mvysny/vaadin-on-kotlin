package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.*
import com.vaadin.flow.data.provider.*
import com.vaadin.flow.function.SerializablePredicate
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Provides instances of entities of given [clazz] from a database. Does not support joins on any of the like; supports filtering
 * and sorting. Only supports simple views over one database table (one entity) - for anything more complex please use [SqlDataProvider].
 * @param idMapper returns the primary key of the entity.
 */
class EntityDataProvider<T : Any>(val clazz: Class<T>, val idMapper: (T)->Any) : AbstractBackEndDataProvider<T, Filter<T>?>() {
    override fun getId(item: T): Any = idMapper(item)
    override fun toString() = "EntityDataProvider($clazz)"

    override fun sizeInBackEnd(query: Query<T, Filter<T>?>?): Int = db {
        val count = con.createQuery("select count(*) ${query.toSQLFromClause(false)}")
                .fillInParamsFromFilters(query)
                .executeScalar()
        (count as Number).toInt()
    }

    override fun fetchFromBackEnd(query: Query<T, Filter<T>?>?): Stream<T> = db {
        val list = con.createQuery("select * ${query.toSQLFromClause(true)}")
                .fillInParamsFromFilters(query)
                .executeAndFetch(clazz)
        list.stream()
    }

    private fun org.sql2o.Query.fillInParamsFromFilters(query: Query<T, Filter<T>?>?): org.sql2o.Query {
        val filters = query?.filter?.orElse(null) ?: return this
        val params = filters.getSQL92Parameters()
        params.entries.forEach { (name, value) -> addParameter(name, value) }
        return this
    }

    private fun Query<T, Filter<T>?>?.toSQLFromClause(includeOrderBy: Boolean): String {
        val where: String? = this?.filter?.orElse(null)?.toSQL92()
        val orderBy = this?.sortOrders?.map { "${it.sorted} ${if (it.direction == SortDirection.ASCENDING) "ASC" else "DESC"}" }?.joinToString()
        val offset = this?.offset
        val limit = this?.limit.takeUnless { it == Int.MAX_VALUE }
        val sql = buildString {
            append("from ${clazz.databaseTableName}")
            if (where != null && where.isNotBlank()) append(" where $where")
            if (orderBy != null && orderBy.isNotBlank() && includeOrderBy) append(" order by $orderBy")
            if (offset != null && limit != null) append(" offset $offset limit $limit")
        }
        return sql
    }
}

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return data provider which can be configured to always apply given filter.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.configurableFilter() : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
        withConfigurableFilter({ f1: Filter<T>?, f2: Filter<T>? -> listOf(f1, f2).filterNotNull().toSet().and() })

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param other applies this filter
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.and(other: Filter<T>) : DataProvider<T, Filter<T>?> = configurableFilter().apply {
    setFilter(other)
}
// since SerializablePredicate is Vaadin-built-in interface which our Filters do not use, since Vaadin 8 has different SerializablePredicate class
// than Vaadin 10 and we would have to duplicate filters just because a dumb interface.
private fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.adaptToVOKFilters() : DataProvider<T, Filter<T>?> = withConvertedFilter<Filter<T>?> {
    f -> if (f == null) null else SerializablePredicate { f.test(it) }
}
@JvmName("andVaadin")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.and(other: Filter<T>) : DataProvider<T, Filter<T>?> = adaptToVOKFilters().and(other)

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter. Allows you to write
 * expressions like this: `sqlDataProvider<Person>().and { Person::age lt 25 }`
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.and(block: SqlWhereBuilder<T>.()-> Filter<T>) : DataProvider<T, Filter<T>?> = configurableFilter().apply {
    setFilter(block)
}
@JvmName("andVaadin2")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.and(block: SqlWhereBuilder<T>.()-> Filter<T>) : DataProvider<T, Filter<T>?> = adaptToVOKFilters().and(block)

/**
 * Creates a filter programatically: `filter { Person::age lt 25 }`
 */
fun <T: Any> filter(block: SqlWhereBuilder<T>.()-> Filter<T>): Filter<T> = block(SqlWhereBuilder())

/**
 * Removes the original filter and sets the new filter. Allows you to write
 * expressions like this: `sqlDataProvider<Person>().and { Person::age lt 25 }`.
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will overwrite the previous filter.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>.setFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) {
    setFilter(block(SqlWhereBuilder()))
}

/**
 * Allows you to simply create a data provider off your entity: `grid.dataProvider = Person.dataProvider`. This data provider
 * doesn't support any joins or more complex queries; to use those please use [SqlDataProvider].
 */
inline val <reified T: Entity<*>> Dao<T>.dataProvider: DataProvider<T, Filter<T>?> get() = EntityDataProvider(T::class.java, { it.id!! })

/**
 * Returns all items provided by this data provider as an eager list. Careful with larger data!
 */
fun <T: Any, F> DataProvider<T, F>.getAll(): List<T> = fetch(Query()).toList()

