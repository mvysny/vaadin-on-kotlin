package com.github.vok.framework.sql2o.vaadin

import com.github.vokorm.*
import com.vaadin.flow.data.provider.*
import java.util.stream.Stream

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
 * Allows you to simply create a data provider off your entity: `grid.dataProvider = Person.dataProvider`. This data provider
 * doesn't support any joins or more complex queries; to use those please use [SqlDataProvider].
 */
inline val <reified T: Entity<*>> Dao<T>.dataProvider: ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>
    get() = EntityDataProvider(T::class.java, { it.id!! }).withConfigurableFilter2()
