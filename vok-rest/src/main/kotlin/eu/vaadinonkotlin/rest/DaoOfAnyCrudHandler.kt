package eu.vaadinonkotlin.rest

import com.gitlab.mvysny.jdbiorm.*
import com.gitlab.mvysny.jdbiorm.condition.Condition
import eu.vaadinonkotlin.nonPrimitive
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.queryParamAsClass
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * A read-only CRUD Handler that provides instances of beans from given [dao]. Rejects all mutation operations with 401 UNAUTHORIZED.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 *    Prepending a column name with `-` will sort DESC.
 * * To define filters, simply pass in column names with the values, for example `age=81`. You can also specify operators: one of
 *   `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. You can pass single column name
 *    multiple times to AND additional clauses, for example `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`. OR filters are not supported.
 * * `select=count` - if this is passed in, then instead of a list of matching objects a single number will be returned: the number of
 * records matching given filters.
 *
 * @property dao fetches items from the database.
 * @property maxLimit the maximum number of items permitted to be returned by [getAll]. If the client attempts to request more
 * items, then 400 BAD REQUEST is returned. Defaults to [Int.MAX_VALUE], definitely change this in production!
 * @property defaultLimit if `limit` is unspecified in [getAll] request, then return at most this number of items. By default, equals to [maxLimit].
 * @param allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 * @param allowFilterColumns if not null, only these columns are allowed to be filtered upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 */
public open class DaoOfAnyCrudHandler<T : Any>(
    public val dao: DaoOfAny<T>,
    public val maxLimit: Long = Long.MAX_VALUE,
    public val defaultLimit: Long = maxLimit,
    public val additionalSortFilterProperties: List<Property<*>> = listOf(),
    public val allowSortColumns: Set<Property.Name>? = null,
    public val allowFilterColumns: Set<Property.Name>? = null
) : CrudHandler {
    private val itemClass: Class<T> get() = dao.entityClass

    override fun getAll(ctx: Context) {
        // grab fetchRange from the query
        val limit = ctx.queryParamAsClass<Long>("limit").getOrDefault(defaultLimit)
        if (limit !in 0..maxLimit) throw BadRequestResponse("invalid limit $limit, must be 0..$maxLimit")
        val offset = ctx.queryParamAsClass<Long>("offset").getOrDefault(0)
        if (offset < 0) throw BadRequestResponse("invalid offset $offset, must be 0 or greater")


        // grab sorting from the query
        val sortByParam = ctx.queryParam("sort_by") ?: ""
        val sortedColumns = sortByParam.split(",").filter { it.isNotBlank() }
        if (allowSortColumns != null) {
            for (sortedColumn in sortedColumns) {
                if (!allowSortColumns.contains(Property.Name(sortedColumn))) throw BadRequestResponse("Cannot sort by ${sortedColumn}: allowed sorting columns are $allowSortColumns")
            }
        }
        val sortBy: List<OrderBy> = sortedColumns.map { restSortQueryToOrderBy(it) }


        // construct Condition
        val filterColumns = ctx.queryParamMap().keys
            .filter { it != "limit" && it != "offset" && it != "sort_by" && it != "select" }
        var filter: Condition = Condition.NO_CONDITION
        for (filterColumn in filterColumns) {
            if (allowFilterColumns != null && !allowFilterColumns.contains(Property.Name(filterColumn))) {
                throw BadRequestResponse("Cannot filter by $filterColumn: allowed filtering columns are $allowFilterColumns")
            }
            ctx.queryParams(filterColumn).forEach { value ->
                val c = restFilterToCondition(value, getProperty(filterColumn) as Property<Any>)
                filter = filter.and(c)
            }
        }

        // fetch the data
        if (ctx.queryParam("select") == "count") {
            val count = dao.countBy(filter)
            ctx.result(count.toString())
        } else {
            val result = dao.findAllBy(filter, sortBy, offset, limit)
            ctx.json(result)
        }
    }

    private fun getProperty(name: Property.Name): Property<*> {
        val a = additionalSortFilterProperties.firstOrNull { it.name == name }
        return a ?: TableProperty.of<T, Any>(itemClass, name)
    }
    private fun getProperty(name: String): Property<*> = getProperty(Property.Name(name))

    /**
     * Converts the `sort_by` REST parameter piece in [sortQuery] into a [OrderBy].
     * @param sortQuery e.g. `+name` or `name` or `-name`.
     */
    private fun restSortQueryToOrderBy(sortQuery: String): OrderBy = when {
        sortQuery.startsWith("+") -> getProperty(sortQuery.substring(1)).asc()
        sortQuery.startsWith("-") -> getProperty(sortQuery.substring(1)).desc()
        else -> getProperty(sortQuery).asc()
    }

    /**
     * Converts the REST filter String [value] to a [Condition].
     */
    protected open fun restFilterToCondition(value: String, prop: Property<Any>): Condition {
        return when {
            value.startsWith("eq:") -> prop.eq(convertToDatabase(value.substring(3), prop.valueType))
            value.startsWith("lte:") -> prop.le(convertToDatabase(value.substring(4), prop.valueType))
            value.startsWith("gte:") -> prop.ge(convertToDatabase(value.substring(4), prop.valueType))
            value.startsWith("gt:") -> prop.gt(convertToDatabase(value.substring(3), prop.valueType))
            value.startsWith("lt:") -> prop.lt(convertToDatabase(value.substring(3), prop.valueType))
            value.startsWith("ne:") -> prop.ne(convertToDatabase(value.substring(3), prop.valueType))
            value.startsWith("isnull:") -> prop.isNull
            value.startsWith("isnotnull:") -> prop.isNotNull
            value.startsWith("like:") -> prop.like(value.substring(5))
            value.startsWith("ilike:") -> prop.likeIgnoreCase(value.substring(6))
            value.startsWith("fulltext:") -> prop.fullTextMatches(value.substring(9))
            else -> prop.eq(convertToDatabase(value, prop.valueType))
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
            Number::class.java.isAssignableFrom(expectedClass.nonPrimitive) -> BigDecimal(nvalue)
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
