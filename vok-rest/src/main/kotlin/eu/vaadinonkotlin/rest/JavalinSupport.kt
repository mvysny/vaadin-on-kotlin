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
 * the [crud2] function.
 * @param allowModification if false then POST/PATCH/DELETE will fail with 401 UNAUTHORIZED
 * @param maxLimit the maximum number of items permitted to be returned by the `getAll()` fetch. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Long.MAX_VALUE], definitely change this in production!
 * @param defaultLimit if `limit` is unspecified in `getAll()` request, then return at most this number of items. By default equals to [maxLimit].
 * @param allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null.
 */
inline fun <reified ID: Any, reified E : Entity<ID>> Dao<E>.getCrudHandler(allowModification: Boolean = false,
                                                                           maxLimit: Long = Long.MAX_VALUE,
                                                                           defaultLimit: Long = maxLimit,
                                                                           allowSortColumns: Set<String>? = null): CrudHandler {
    return VokOrmCrudHandler(ID::class.java, E::class.java, allowModification, maxLimit, defaultLimit, allowSortColumns)
}

/**
 * A very simple handler that exposes instances of given [entityClass] using the `vok-orm` database access library.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 * * TBD filters
 *
 * @param idClass the type of the Entity ID. Only `String`, `Long` and `Integer` IDs are supported as of now; all others will be rejected with an [IllegalArgumentException].
 * @property entityClass the type of the entity for which to provide instances.
 * @property allowsModification if false then POST/PATCH/DELETE [create]/[delete]/[update] will fail with 401 UNAUTHORIZED
 * @property maxLimit the maximum number of items permitted to be returned by [getAll]. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Int.MAX_VALUE], definitely change this in production!
 * @property defaultLimit if `limit` is unspecified in [getAll] request, then return at most this number of items. By default equals to [maxLimit].
 * @property allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null.
 */
class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(idClass: Class<ID>, private val entityClass: Class<E>,
                                                val allowsModification: Boolean,
                                                val maxLimit: Long = Long.MAX_VALUE,
                                                val defaultLimit: Long = maxLimit,
                                                val allowSortColumns: Set<String>? = null) : CrudHandler {

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

        // fetch the data
        if (ctx.queryParam("select") == "count") {
            val count = dataLoader.getCount()
            ctx.result(count.toString())
        } else {
            val result = dataLoader.fetch(sortBy = sortBy, range = fetchRange)
            ctx.json(db { result })
        }
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
            val exists = con.findById(entityClass, entity.id!!) != null
            if (!exists) throw NotFoundResponse("No such entity with ID $resourceId")
            entity.save()
        }
    }

    private fun Long.toInt2() = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
