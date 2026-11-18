package eu.vaadinonkotlin.rest

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.CrudHandler
import io.javalin.config.JavalinConfig
import io.javalin.config.RoutesConfig
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
 * Registers a CRUD handler at [path], inside [Javalin.create]'s config block:
 *
 * ```kotlin
 * Javalin.create { it.routes.crud2("/rest/users", Users.getCrudHandler()) }
 * ```
 *
 * Endpoints:
 *
 *  * `GET /rest/users`           — list (see [KtormCrudHandler] for query params)
 *  * `GET /rest/users/22`        — single by id
 *  * `POST /rest/users`          — create (if [KtormCrudHandler.allowModification])
 *  * `PATCH /rest/users/22`      — update (if [KtormCrudHandler.allowModification])
 *  * `DELETE /rest/users/22`     — delete (if [KtormCrudHandler.allowModification])
 *
 * @param path the collection's path, without the `/{id}` suffix
 * @param crudHandler typically built via [Table.getCrudHandler]
 * @param permittedRoles roles allowed to access these endpoints
 */
public fun RoutesConfig.crud2(path: String, crudHandler: CrudHandler, permittedRoles: Set<RouteRole> = setOf()): RoutesConfig =
    apiBuilder { ApiBuilder.crud("/${path.trim('/')}/{id}", crudHandler, *permittedRoles.toTypedArray()) }

/**
 * Convenience factory: builds a [KtormCrudHandler] for this [Table]. The table must have exactly one primary key.
 */
public fun <E : Entity<E>> Table<E>.getCrudHandler(
    allowModification: Boolean = false,
    maxLimit: Long = Long.MAX_VALUE,
    defaultLimit: Long = maxLimit
): CrudHandler = KtormCrudHandler(this, allowModification, maxLimit, defaultLimit)
