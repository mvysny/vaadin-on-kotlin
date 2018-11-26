package eu.vaadinonkotlin.rest

import com.github.vokorm.*
import com.github.vokorm.dataloader.EntityDataLoader
import io.javalin.Context
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.CrudHandler
import org.sql2o.converters.Converter
import org.sql2o.converters.IntegerConverter
import org.sql2o.converters.LongConverter
import org.sql2o.converters.StringConverter

/**
 * A very simple handler that exposes instances of given [entityClass] using the `vok-orm` database access library.
 *
 * The [getAll] honors the following query parameters:
 * * `limit` and `offset` for result paging. Both must be 0 or greater; `limit` must be less than `maxLimit`
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. Only those which appear in `allowSortColumns` are allowed.
 * Prepending a column name with `-` will sort DESC.
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
 * @param allowsModification if false then POST/PATCH/DELETE [create]/[delete]/[update] will fail with 401 UNAUTHORIZED
 * @param maxLimit the maximum number of items permitted to be returned by [getAll]. If the client attempts to request more
 * items then 400 BAD REQUEST is returned. Defaults to [Int.MAX_VALUE], definitely change this in production!
 * @param defaultLimit if `limit` is unspecified in [getAll] request, then return at most this number of items. By default equals to [maxLimit].
 * @param allowSortColumns if not null, only these columns are allowed to be sorted upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 * @param allowFilterColumns if not null, only these columns are allowed to be filtered upon. Defaults to null. References the [kotlin.reflect.KProperty1.name] of the entity.
 */
open class VokOrmCrudHandler<ID: Any, E: Entity<ID>>(idClass: Class<ID>, private val entityClass: Class<E>,
                                                                       val allowsModification: Boolean,
                                                                       maxLimit: Long = Long.MAX_VALUE,
                                                                       defaultLimit: Long = maxLimit,
                                                                       allowSortColumns: Set<String>? = null,
                                                                       allowFilterColumns: Set<String>? = null) : CrudHandler {

    /**
     * The [getAll] call delegates here.
     */
    protected val getAllHandler: CrudHandler = DataLoaderCrudHandler(entityClass, EntityDataLoader(entityClass), maxLimit, defaultLimit, allowSortColumns, allowFilterColumns)

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

    override fun getAll(ctx: Context) = getAllHandler.getAll(ctx)

    override fun getOne(ctx: Context, resourceId: String) {
        val id = convertID(resourceId)
        val obj = db { con.findById(entityClass, id) }
                ?: throw NotFoundResponse("No such entity with ID $resourceId")
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
}
