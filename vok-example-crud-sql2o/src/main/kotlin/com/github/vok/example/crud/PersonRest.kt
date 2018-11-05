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
import org.sql2o.converters.Converter
import org.sql2o.converters.IntegerConverter
import org.sql2o.converters.LongConverter
import org.sql2o.converters.StringConverter
import java.lang.Long
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
            .configureRest()
            .createServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud("/rest/person", Person.getCrudHandler(false))
    return this
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

class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(idClass: Class<ID>, private val entityClass: Class<E>, val allowsModification: Boolean) : CrudHandler {
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
        throw NotFoundResponse("Unparsable ID: $resourceId")
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
        entity.save()
    }
}
