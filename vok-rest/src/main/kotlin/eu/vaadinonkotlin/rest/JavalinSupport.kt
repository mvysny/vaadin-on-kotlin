@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package eu.vaadinonkotlin.rest

import com.github.vokorm.*
import com.github.vokorm.dataloader.EntityDataLoader
import com.github.vokorm.dataloader.SortClause
import com.google.gson.Gson
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.CrudHandler
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import io.javalin.security.Role
import org.sql2o.converters.Converter
import org.sql2o.converters.IntegerConverter
import org.sql2o.converters.LongConverter
import org.sql2o.converters.StringConverter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Configures Gson to Javalin. You need to call this if you wish to produce JSON
 * via Javalin's [Context.json], or parse incoming JSON via [Context.bodyAsClass].
 */
fun Gson.configureToJavalin() {
    JavalinJson.fromJsonMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T = fromJson(json, targetClass)
    }
    JavalinJson.toJsonMapper = object : ToJsonMapper {
        override fun map(obj: Any): String = toJson(obj)
    }
}

/**
 * Configures a CRUD handler to a particular endpoint, e.g. `javalin.crud("/rest/users", User.getCrudHandler(false))`. The following endpoints are published:
 * * `GET /rest/users` returns all users
 * * `GET /rest/users/22` returns one users
 * * `POST /rest/users` will create an user
 * * `PATCH /rest/users/22` will update an user
 * * `DELETE /rest/users/22` will delete an user
 *
 * For details on query parameters for sorting, filtering, paging and count retrieval please see the [VokOrmCrudHandler] class itself.
 *
 * @param path the base path where the endpoints are exposed
 * @param crudHandler retrieves bean instances; you can use [getCrudHandler] to automatically provide instances of vok-orm entities.
 * @param permittedRoles only these roles are allowed to access abovementioned endpoints. See Javalin [io.javalin.security.AccessManager] and the Javalin documentation for more details.
 */
fun Javalin.crud2(path: String, crudHandler: CrudHandler, permittedRoles: Set<Role> = setOf()): Javalin = routes {
    val p = path.trim('/')
    if (p.contains('/')) {
        ApiBuilder.path(p.substringBeforeLast('/')) {
            ApiBuilder.crud(p.substringAfterLast('/') + "/:id", crudHandler, permittedRoles)
        }
    } else {
        ApiBuilder.crud(path, crudHandler, permittedRoles)
    }
}

/**
 * Creates a CRUD Handler for this entity, providing instances of this entity over REST. You should simply pass the CRUD Handler into
 * the [crud2] function. A shorthand for creating the [VokOrmCrudHandler] for this entity.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 * * To define filters, simply pass in column names with the values, for example `age=81`. You can also specify operators: one of
 * `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. You can pass single column name
 * multiple times to AND additional clauses, for example `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`. OR filters are not supported.
 * * `select=count` - if this is passed in, then instead of a list of matching objects a single number will be returned: the number of
 * records matching given filters.
 *
 * All column names are expected to be Kotlin [kotlin.reflect.KProperty1.name] of the entity in question.
 *
 * @param allowModification if false then POST/PATCH/DELETE will fail with 401 UNAUTHORIZED
 * @param maxLimit the maximum number of items permitted to be returned by the `getAll()` fetch. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Long.MAX_VALUE], definitely change this in production!
 * @param defaultLimit if `limit` is unspecified in `getAll()` request, then return at most this number of items. By default equals to [maxLimit].
 * @param allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null.
 * @param allowFilterColumns if not null, only these columns are allowed to be filtered upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 */
inline fun <reified ID: Any, reified E : Entity<ID>> Dao<E>.getCrudHandler(allowModification: Boolean = false,
                                                                           maxLimit: Long = Long.MAX_VALUE,
                                                                           defaultLimit: Long = maxLimit,
                                                                           allowSortColumns: Set<String>? = null,
                                                                           allowFilterColumns: Set<String>? = null): CrudHandler {
    return VokOrmCrudHandler(ID::class.java, E::class.java, allowModification, maxLimit, defaultLimit, allowSortColumns, allowFilterColumns)
}

/**
 * A very simple handler that exposes instances of given [entityClass] using the `vok-orm` database access library.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 * * To define filters, simply pass in column names with the values, for example `age=81`. You can also specify operators: one of
 * `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. You can pass single column name
 * multiple times to AND additional clauses, for example `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`. OR filters are not supported.
 * * `select=count` - if this is passed in, then instead of a list of matching objects a single number will be returned: the number of
 * records matching given filters.
 *
 * All column names are expected to be Kotlin [kotlin.reflect.KProperty1.name] of the entity in question.
 *
 * @param idClass the type of the Entity ID. Only `String`, `Long` and `Integer` IDs are supported as of now; all others will be rejected with an [IllegalArgumentException].
 * @property entityClass the type of the entity for which to provide instances.
 * @property allowsModification if false then POST/PATCH/DELETE [create]/[delete]/[update] will fail with 401 UNAUTHORIZED
 * @property maxLimit the maximum number of items permitted to be returned by [getAll]. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Int.MAX_VALUE], definitely change this in production!
 * @property defaultLimit if `limit` is unspecified in [getAll] request, then return at most this number of items. By default equals to [maxLimit].
 * @property allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 * @property allowFilterColumns if not null, only these columns are allowed to be filtered upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 */
open class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(idClass: Class<ID>, private val entityClass: Class<E>,
                                                val allowsModification: Boolean,
                                                val maxLimit: Long = Long.MAX_VALUE,
                                                val defaultLimit: Long = maxLimit,
                                                val allowSortColumns: Set<String>? = null,
                                                val allowFilterColumns: Set<String>? = null) : CrudHandler {

    private val dataLoader = EntityDataLoader(entityClass)

    @Suppress("UNCHECKED_CAST")
    private val idConverter = when (idClass) {
        String::class.java -> StringConverter() as Converter<ID>
        java.lang.Long::class.java -> LongConverter(false) as Converter<ID>
        Integer::class.java -> IntegerConverter(false) as Converter<ID>
        else -> throw IllegalArgumentException("Can't provide converter for $idClass")
    }

    private fun checkAllowsModification() {
        if (!allowsModification) throw UnauthorizedResponse()
    }

    private fun convertID(resourceId: String): ID = try {
        idConverter.convert(resourceId)
    } catch (e: NumberFormatException) {
        throw NotFoundResponse("Malformed ID: $resourceId")
    }

    override fun create(ctx: Context) {
        checkAllowsModification()
        ctx.bodyAsClass(entityClass).save()
    }

    override fun delete(ctx: Context, resourceId: String) {
        checkAllowsModification()
        val id = convertID(resourceId)
        db { con.deleteById(entityClass, id) }
    }

    override fun getAll(ctx: Context) {
        // grab fetchRange from the query
        val limit: Long = ctx.queryParam("limit")?.toLong() ?: defaultLimit
        if (limit !in 0..maxLimit) throw BadRequestResponse("invalid limit $limit, must be 0..$maxLimit")
        val offset = ctx.queryParam("offset")?.toLong() ?: 0
        if (offset < 0) throw BadRequestResponse("invalid offset $offset, must be 0 or greater")
        val fetchRange = offset.toInt2() until (offset + limit).toInt2()

        fun parseSortClause(sortQuery: String): SortClause = when {
            sortQuery.startsWith("+") -> SortClause(sortQuery.substring(1), true)
            sortQuery.startsWith("-") -> SortClause(sortQuery.substring(1), false)
            else -> SortClause(sortQuery, true)
        }

        // grab sorting from the query
        val sortByParam = ctx.queryParam("sort_by") ?: ""
        val sortBy: List<SortClause> = sortByParam.split(",").filter { it.isNotBlank() }.map { parseSortClause(it) }
        if (allowSortColumns != null) {
            sortBy.forEach {
                if (!allowSortColumns.contains(it.columnName)) throw BadRequestResponse("Cannot sort by ${it.columnName}, only these are allowed: $allowSortColumns")
            }
        }

        // construct filters

        val fields = entityClass.entityMeta.properties
                .filter { it.name != "limit" && it.name != "offset" && it.name != "sort_by" && it.name != "select" }
                .associateBy { it.name }
        val filters = mutableSetOf<Filter<E>>()
        for ((name, prop) in fields.entries) {
            val value = ctx.queryParam(name)
            if (value != null) {
                if (allowFilterColumns != null) {
                    require(allowFilterColumns.contains(name)) { "Cannot filter by $name: only the following columns are allowed to be sorted upon: $allowFilterColumns" }
                }
                val dbname = prop.dbColumnName
                val filter: Filter<E> = when {
                    value.startsWith("eq:") -> OpFilter(dbname, convertToDatabase(value.substring(3), prop.valueType) as Comparable<Any>, CompareOperator.eq)
                    value.startsWith("lte:") -> OpFilter(dbname, convertToDatabase(value.substring(4), prop.valueType) as Comparable<Any>, CompareOperator.le)
                    value.startsWith("gte:") -> OpFilter(dbname, convertToDatabase(value.substring(4), prop.valueType) as Comparable<Any>, CompareOperator.ge)
                    value.startsWith("gt:") -> OpFilter(dbname, convertToDatabase(value.substring(3), prop.valueType) as Comparable<Any>, CompareOperator.gt)
                    value.startsWith("lt:") -> OpFilter(dbname, convertToDatabase(value.substring(3), prop.valueType) as Comparable<Any>, CompareOperator.lt)
                    value.startsWith("isnull:") -> IsNullFilter(dbname)
                    value.startsWith("isnotnull:") -> IsNotNullFilter(dbname)
                    value.startsWith("like:") -> LikeFilter(dbname, value.substring(5))
                    value.startsWith("ilike:") -> ILikeFilter(dbname, value.substring(6))
                    else -> EqFilter(prop.dbColumnName, convertToDatabase(value, prop.valueType))
                }
                filters.add(filter)
            }
        }
        val filter: Filter<E>? = if (filters.isEmpty()) null else AndFilter(filters)

        // fetch the data
        if (ctx.queryParam("select") == "count") {
            val count = dataLoader.getCount(filter)
            ctx.result(count.toString())
        } else {
            val result = dataLoader.fetch(filter, sortBy, fetchRange)
            ctx.json(db { result })
        }
    }

    protected fun convertToDatabase(value: String, expectedClass: Class<*>): Any? {
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

    override fun getOne(ctx: Context, resourceId: String) {
        val id = convertID(resourceId)
        val obj = db { con.findById(entityClass, id) } ?: throw NotFoundResponse("No such entity with ID $resourceId")
        ctx.json(obj)
    }

    override fun update(ctx: Context, resourceId: String) {
        checkAllowsModification()
        val entity = ctx.bodyAsClass(entityClass)
        entity.id = idConverter.convert(resourceId)
        db {
            val exists = con.existsById(entityClass, entity.id!!)
            if (!exists) throw NotFoundResponse("No such entity with ID $resourceId")
            entity.save()
        }
    }

    private fun Long.toInt2() = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
