package eu.vaadinonkotlin.rest

import com.github.vokorm.KEntity
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.Property
import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.CrudHandler
import io.javalin.config.JavalinConfig
import io.javalin.json.JavalinGson
import io.javalin.json.JsonMapper
import io.javalin.security.RouteRole

/**
 * Configures [gson] as Javalin's [JsonMapper].
 */
public fun JavalinConfig.gsonMapper(gson: Gson) {
    jsonMapper(JavalinGson(gson))
}

/**
 * Configures a CRUD handler to a particular endpoint, e.g. `javalin.crud("/rest/users", User.getCrudHandler(false))`. The following endpoints are published:
 * * `GET /rest/users` returns all users
 * * `GET /rest/users/22` returns one user
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
public fun Javalin.crud2(path: String, crudHandler: CrudHandler, permittedRoles: Set<RouteRole> = setOf()): Javalin = routes {
    val p = path.trim('/')
    if (p.contains('/')) {
        ApiBuilder.path(p.substringBeforeLast('/')) {
            ApiBuilder.crud(p.substringAfterLast('/') + "/{id}", crudHandler, *permittedRoles.toTypedArray())
        }
    } else {
        ApiBuilder.crud(path, crudHandler, *permittedRoles.toTypedArray())
    }
}

/**
 * Creates a CRUD Handler for this entity, providing instances of this entity over REST. You should simply pass the CRUD Handler into
 * the [crud2] function. A shorthand for creating the [VokOrmCrudHandler] for this entity.
 *
 * The [VokOrmCrudHandler.getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than [maxLimit]
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in [allowSortColumns] are allowed.
 * Prepending a column name with `-` will sort DESC.
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
public inline fun <reified ID : Any, reified E : KEntity<ID>> Dao<E, ID>.getCrudHandler(allowModification: Boolean = false,
                                                                                        maxLimit: Long = Long.MAX_VALUE,
                                                                                        defaultLimit: Long = maxLimit,
                                                                                        additionalSortFilterProperties: List<Property<*>> = listOf(),
                                                                                        allowSortColumns: Set<Property.Name>? = null,
                                                                                        allowFilterColumns: Set<Property.Name>? = null): CrudHandler {
    return VokOrmCrudHandler(ID::class.java, this, allowModification, maxLimit, defaultLimit, additionalSortFilterProperties, allowSortColumns, allowFilterColumns)
}
