package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.sql2o.Filter
import com.github.vok.framework.sql2o.db
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.SortDirection
import java.sql.ResultSet
import java.util.stream.Stream

/**
 * Allows the coder to write any SQL he wishes. This provider must be simple enough to not to get in the way by smart (complex) Kotlin language features.
 * It should support any SQL select, but should allow for adding custom filters and orderings (since this is plugged into Grid after all).
 *
 * The provider is bound to a *holder class* which holds the values (any POJO). Sql2o is used to map the result set to the class. For example:
 *
 * ```
 * data class CustomerAddress(val customerName: String, val address: String)
 *
 * val provider = SqlDataProvider("""select c.name as customerName, a.street || ' ' || a.city as address
 *     from Customer c inner join Address a on c.address_id=a.id where 1=1 {{WHERE}} order by null{{ORDER}} {{PAGING}}""")
 * ```
 *
 * (Note how select column names must correspond to field names in the `CustomerAddress` class)
 *
 * Now SqlDataProvider can hot-patch the `where` clause computed from Grid's filters into `{{WHERE}}` (as a simple string replacement),
 * and the `order by` and `offset`/`limit` into the `{{ORDER}}` and `{{PAGING}}`, as follows:
 *
 * * `{{WHERE}}` will be replaced by something like this: `"and name=:pqw5as and age>:p123a"` - note the auto-generated parameter
 *   names starting with `p`. If there are no filters, will be replaced by an empty string.
 * * `{{ORDER}}` will be replaced by `", customerName ASC, street ASC"` or by an empty string if there is no ordering requirement.
 * * `{{PAGING}}` will be replaced by `"offset 0 limit 100"` or by an empty string if there are no limitations.
 *
 * Note that the Grid will display fields present in the `CustomerAddress` holder class and will also auto-generate filters
 * for them, based on the type of the field.
 *
 * No bloody annotations! Work in progress. It is expected that a holder class is written for every select, tailored to show the outcome
 * of that particular select.
 *
 * @param clazz the type of the holder class which will hold the result
 * @param sql the select which can map into the holder class (that is, it selects columns whose names match the holder class fields). It should contain
 * {{WHERE}}, {{ORDER}} and {{PAGING}} strings which will be replaced by a simple substring replacement.
 * @param params the [sql] may be parametrized; this map holds all the parameters present in the sql itself.
 * @param idMapper returns the primary key. If the holder class is a data class and/or has proper equals/hashcode, the class itself may act as the key; in such case
 * just pass in identity here: `{ it }`
 * @param T the type of the holder class.
 * @author mavi
 */
class SqlDataProvider<T: Any>(val clazz: Class<T>, val sql: String, val params: Map<String, Any?> = mapOf(), val idMapper: (T)->Any) : AbstractBackEndDataProvider<T, Filter<T>?>() {
    override fun getId(item: T): Any = idMapper(item)
    override fun toString() = "SqlDataProvider($clazz:$sql)"

    override fun sizeInBackEnd(query: Query<T, Filter<T>?>?): Int = db {
        val q = con.createQuery(query.computeSQL(true))
        params.entries.forEach { (name, value) -> q.addParameter(name, value) }
        q.fillInParamsFromFilters(query)
        val counts: List<Int> = q.executeAndFetch({ rs: ResultSet -> if (rs.last()) rs.row else 0 })
        if (counts.isEmpty()) 0 else counts[0]
    }

    override fun fetchFromBackEnd(query: Query<T, Filter<T>?>?): Stream<T> = db {
        val q = con.createQuery(query.computeSQL(false))
        params.entries.forEach { (name, value) -> q.addParameter(name, value) }
        q.fillInParamsFromFilters(query)
        val list = q.executeAndFetch(clazz)
        list.stream()
    }

    private fun org.sql2o.Query.fillInParamsFromFilters(query: Query<T, Filter<T>?>?): org.sql2o.Query {
        val filters = query?.filter?.orElse(null) ?: return this
        val params = filters.getSQL92Parameters()
        params.entries.forEach { (name, value) -> addParameter(name, value) }
        return this
    }

    private fun Query<T, Filter<T>?>?.computeSQL(isCountQuery: Boolean): String {
        var where: String = this?.filter?.orElse(null)?.toSQL92() ?: ""
        if (where.isNotBlank()) where = "and $where"
        var orderBy = if (isCountQuery) "" else
            this?.sortOrders?.map { "${it.sorted} ${if (it.direction == SortDirection.ASCENDING) "ASC" else "DESC"}" }?.joinToString() ?: ""
        if (orderBy.isNotBlank()) orderBy = ", $orderBy"
        val offset = this?.offset
        val limit = this?.limit.takeUnless { it == Int.MAX_VALUE }
        val paging = if (!isCountQuery && offset != null && limit != null) " offset $offset limit $limit" else ""
        val s = sql.replace("{{WHERE}}", where).replace("{{ORDER}}", orderBy).replace("{{PAGING}}", paging)
        return s
    }
}
