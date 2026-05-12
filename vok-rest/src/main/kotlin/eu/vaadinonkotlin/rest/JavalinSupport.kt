package eu.vaadinonkotlin.rest

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.CrudHandler
import io.javalin.config.JavalinConfig
import io.javalin.json.JavalinGson
import io.javalin.json.JsonMapper
import io.javalin.security.RouteRole
import org.ktorm.entity.Entity
import org.ktorm.schema.Table

/**
 * Configures [gson] as Javalin's [JsonMapper].
 */
public fun JavalinConfig.gsonMapper(gson: Gson) {
    jsonMapper(JavalinGson(gson))
}

/**
 * Registers a CRUD handler at [path], e.g. `javalin.crud2("/rest/users", Users.getCrudHandler())`. Endpoints:
 *
 *  * `GET /rest/users`           — list (see [KtormCrudHandler] for query params)
 *  * `GET /rest/users/22`        — single by id
 *  * `POST /rest/users`          — create (if [allowModification])
 *  * `PATCH /rest/users/22`      — update (if [allowModification])
 *  * `DELETE /rest/users/22`     — delete (if [allowModification])
 *
 * @param path the base path
 * @param crudHandler typically built via [Table.getCrudHandler]
 * @param permittedRoles roles allowed to access these endpoints
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
 * Convenience factory: builds a [KtormCrudHandler] for this [Table]. The table must have exactly one primary key.
 */
public fun <E : Entity<E>> Table<E>.getCrudHandler(
    allowModification: Boolean = false,
    maxLimit: Long = Long.MAX_VALUE,
    defaultLimit: Long = maxLimit
): CrudHandler = KtormCrudHandler(this, allowModification, maxLimit, defaultLimit)
