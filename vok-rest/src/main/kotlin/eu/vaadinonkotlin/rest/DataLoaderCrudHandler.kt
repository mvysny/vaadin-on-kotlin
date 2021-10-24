package eu.vaadinonkotlin.rest

import com.github.mvysny.vokdataloader.*
import com.github.vokorm.db
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * A read-only CRUD Handler that only provides instances of beans. Rejects all mutation operations with 401 UNAUTHORIZED.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 * Prepending a column name with `-` will sort DESC.
 * * To define filters, simply pass in column names with the values, for example `age=81`. You can also specify operators: one of
 * `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. You can pass single column name
 * multiple times to AND additional clauses, for example `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`. OR filters are not supported.
 * * `select=count` - if this is passed in, then instead of a list of matching objects a single number will be returned: the number of
 * records matching given filters.
 *
 * All column names are expected to be Kotlin [kotlin.reflect.KProperty1.name] of the entity in question. Currently there is
 * no way to override this.
 *
 * @property itemClass the type of items fetched from the [dataLoader]
 * @property dataLoader the data provider used to fetch items.
 * @property maxLimit the maximum number of items permitted to be returned by [getAll]. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Int.MAX_VALUE], definitely change this in production!
 * @property defaultLimit if `limit` is unspecified in [getAll] request, then return at most this number of items. By default equals to [maxLimit].
 * @property allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 * @property allowFilterColumns if not null, only these columns are allowed to be filtered upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 */
public open class DataLoaderCrudHandler<T : Any>(
        public val itemClass: Class<T>,
        public val dataLoader: DataLoader<T>,
        public val maxLimit: Long = Long.MAX_VALUE,
        public val defaultLimit: Long = maxLimit,
        public val allowSortColumns: Set<DataLoaderPropertyName>? = null,
        public val allowFilterColumns: Set<DataLoaderPropertyName>? = null
) : CrudHandler {
    override fun getAll(ctx: Context) {
        // grab fetchRange from the query
        val limit: Long = ctx.queryParamAsClass<Long>("limit").getOrDefault(defaultLimit)
        if (limit !in 0..maxLimit) throw BadRequestResponse("invalid limit $limit, must be 0..$maxLimit")
        val offset = ctx.queryParamAsClass<Long>("offset").getOrDefault(0)
        if (offset < 0) throw BadRequestResponse("invalid offset $offset, must be 0 or greater")
        val fetchRange = offset until (offset + limit)

        // maps Kotlin Bean property name to PropertyDescriptor which will help us getting the actual column database name.
        val fields: Map<String, PropertyDescriptor> = Introspector.getBeanInfo(itemClass).propertyDescriptors
                .filter { it.name != "limit" && it.name != "offset" && it.name != "sort_by" && it.name != "select" }
                .associateBy { it.name }


        // grab sorting from the query
        val sortByParam = ctx.queryParam("sort_by") ?: ""
        val sortBy: List<SortClause> = sortByParam.split(",").filter { it.isNotBlank() }.map { restSortQueryToSortClause(it) }
        if (allowSortColumns != null) {
            for (sortClause in sortBy) {
                if (!allowSortColumns.contains(sortClause.propertyName)) throw BadRequestResponse("Cannot sort by ${sortClause.propertyName}, only these are allowed: $allowSortColumns")
            }
        }


        // construct filters
        val filters = mutableSetOf<Filter<T>>()
        for ((name, prop) in fields.entries) {
            ctx.queryParams(name).forEach { value ->
                if (allowFilterColumns != null) {
                    require(allowFilterColumns.contains(name)) { "Cannot filter by $name: only the following columns are allowed to be sorted upon: $allowFilterColumns" }
                }
                val filter: Filter<T> = restFilterToDataLoaderFilter(value, prop)
                filters.add(filter)
            }
        }
        val filter: Filter<T>? = filters.and()

        // fetch the data
        if (ctx.queryParam("select") == "count") {
            val count = dataLoader.getCount(filter)
            ctx.result(count.toString())
        } else {
            val result = dataLoader.fetch(filter, sortBy, fetchRange)
            ctx.json(db { result })
        }
    }

    /**
     * Converts the `sort_by` REST parameter piece in [sortQuery] into a [SortClause].
     * @param sortQuery e.g. `+name` or `name` or `-name`.
     */
    protected open fun restSortQueryToSortClause(sortQuery: String): SortClause = when {
        sortQuery.startsWith("+") -> SortClause(sortQuery.substring(1), true)
        sortQuery.startsWith("-") -> SortClause(sortQuery.substring(1), false)
        else -> SortClause(sortQuery, true)
    }

    /**
     * Converts the REST filter String [value] to a `vok-dataloader` [Filter]. Use [prop] as a hint for which Java Bean
     * Property this filter applies.
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun restFilterToDataLoaderFilter(value: String, prop: PropertyDescriptor): Filter<T> {
        val name: String = prop.name
        return when {
            value.startsWith("eq:") -> OpFilter(name, convertToDatabase(value.substring(3), prop.propertyType) as Comparable<Any>, CompareOperator.eq)
            value.startsWith("lte:") -> OpFilter(name, convertToDatabase(value.substring(4), prop.propertyType) as Comparable<Any>, CompareOperator.le)
            value.startsWith("gte:") -> OpFilter(name, convertToDatabase(value.substring(4), prop.propertyType) as Comparable<Any>, CompareOperator.ge)
            value.startsWith("gt:") -> OpFilter(name, convertToDatabase(value.substring(3), prop.propertyType) as Comparable<Any>, CompareOperator.gt)
            value.startsWith("lt:") -> OpFilter(name, convertToDatabase(value.substring(3), prop.propertyType) as Comparable<Any>, CompareOperator.lt)
            value.startsWith("isnull:") -> IsNullFilter(name)
            value.startsWith("isnotnull:") -> IsNotNullFilter(name)
            value.startsWith("like:") -> StartsWithFilter(name, value.substring(5), false)
            value.startsWith("ilike:") -> StartsWithFilter(name, value.substring(6), true)
            value.startsWith("fulltext:") -> FullTextFilter(name, value.substring(9))
            else -> EqFilter(name, convertToDatabase(value, prop.propertyType))
        }
    }

    /**
     * Converts the REST filter String [value] to a Java object that is fit to pass to the JDBC as a WHERE clause parameter.
     * The [expectedClass] tells what kind of type we expect; the function may honor that or may return something else
     * or may even return the original String if the JDBC driver can handle that.
     */
    protected open fun convertToDatabase(value: String, expectedClass: Class<*>): Any? {
        fun String.nullify(): String? = if (this == "null") null else this

        val nvalue = value.nullify() ?: return null
        val convertedValue: Any? = when {
            Number::class.java.isAssignableFrom(expectedClass) -> BigDecimal(nvalue)
            Date::class.java.isAssignableFrom(expectedClass) -> Instant.ofEpochMilli(nvalue.toLong())
            expectedClass == LocalDate::class.java || expectedClass == LocalDateTime::class.java || expectedClass == Instant::class.java -> Instant.ofEpochMilli(nvalue.toLong())
            else -> nvalue
        }
        return convertedValue
    }

    override fun create(ctx: Context) {
        throw UnauthorizedResponse()
    }
    override fun delete(ctx: Context, resourceId: String) {
        throw UnauthorizedResponse()
    }
    override fun getOne(ctx: Context, resourceId: String) {
        throw UnauthorizedResponse()
    }
    override fun update(ctx: Context, resourceId: String) {
        throw UnauthorizedResponse()
    }
}
