@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.vok.rest

import com.github.vokorm.*
import com.google.gson.Gson
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
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
import java.lang.Long

/**
 * Configures Gson [receiver] to Javalin. You need to call this on [Javalin] instance if you wish to produce JSON
 * via [Context.json].
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
 */
fun Javalin.crud(path: String, crudHandler: CrudHandler, permittedRoles: Set<Role> = setOf()): Javalin = routes {
    val p = path.trim('/')
    if (p.contains('/')) {
        ApiBuilder.path(p.substringBeforeLast('/')) {
            ApiBuilder.crud(p.substringAfterLast('/') + "/:id", crudHandler, permittedRoles)
        }
    } else {
        ApiBuilder.crud(path, crudHandler, permittedRoles)
    }
}

inline fun <reified ID: Any, reified E : Entity<ID>> Dao<E>.getCrudHandler(allowModification: Boolean = false): CrudHandler {
    return VokOrmCrudHandler(ID::class.java, E::class.java, allowModification)
}

/**
 * A very simple handler that exposes instances of given [entityClass].
 * @param allowsModification if false then POST/PATCH/DELETE [create]/[delete]/[update] will fail with 401 UNAUTHORIZED
 */
class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(idClass: Class<ID>, private val entityClass: Class<E>, val allowsModification: Boolean) : CrudHandler {
    @Suppress("UNCHECKED_CAST")
    private val idConverter = when (idClass) {
        String::class.java -> StringConverter() as Converter<ID>
        Long::class.java -> LongConverter(false) as Converter<ID>
        Integer::class.java -> IntegerConverter(false) as Converter<ID>
        else -> throw IllegalStateException("Can't provide converter for $idClass")
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
        ctx.json(db { con.findAll(entityClass) })
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
}
