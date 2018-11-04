package com.github.vok.example.crud

import com.github.vok.example.crud.personeditor.Person
import com.github.vokorm.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.CrudHandler
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val gson = GsonBuilder().create()

    init {
        gson.configureToJavalin()
    }

    val javalin = EmbeddedJavalin()
            .get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
            .crud("/rest/person", Person.getCrudHandler(false))
            .createServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Gson.configureToJavalin() {
    JavalinJson.fromJsonMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T = fromJson(json, targetClass)
    }
    JavalinJson.toJsonMapper = object : ToJsonMapper {
        override fun map(obj: Any): String = toJson(obj)
    }
}

fun Javalin.crud(path: String, crudHandler: CrudHandler): Javalin = routes {
    val p = path.trim('/')
    if (p.contains('/')) {
        ApiBuilder.path(p.substringBeforeLast('/')) {
            ApiBuilder.crud(p.substringAfterLast('/') + "/:user-id", crudHandler)
        }
    } else {
        ApiBuilder.crud(path, crudHandler)
    }
}

inline fun <reified ID: Any, reified E : Entity<ID>> Dao<E>.getCrudHandler(allowModification: Boolean = false): CrudHandler {
    return VokOrmCrudHandler(ID::class.java, E::class.java, allowModification)
}

class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(val idClass: Class<ID>, val entityClass: Class<E>, val allowModification: Boolean) : CrudHandler {
    private fun checkAllowModification() {
        if (!allowModification) throw UnauthorizedResponse()
    }
    override fun create(ctx: Context) {
        checkAllowModification()
        ctx.bodyAsClass(entityClass).save()
    }

    override fun delete(ctx: Context, resourceId: String) {
        checkAllowModification()
        db { con.deleteById(entityClass, resourceId) }
    }

    override fun getAll(ctx: Context) {
        ctx.json(db { con.findAll(entityClass) })
    }

    override fun getOne(ctx: Context, resourceId: String) {
        val obj = db { con.findById(entityClass, resourceId) } ?: throw NotFoundResponse()
        ctx.json(obj)
    }

    override fun update(ctx: Context, resourceId: String) {
        checkAllowModification()
        val entity = ctx.bodyAsClass(entityClass)
        TODO("unimplemented")
//        entity.id = resourceId
//        entity.save()
    }
}
