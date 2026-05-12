package eu.vaadinonkotlin.rest

import com.github.mvysny.ktormvaadin.db
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.delete
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.count
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.EnumSqlType
import org.ktorm.schema.InstantSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LocalDateSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.NestedBinding
import org.ktorm.schema.Table
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

/**
 * A Javalin [CrudHandler] backed by a ktorm [Table]. POST/PATCH/DELETE return 401 unless [allowModification] is true.
 *
 * Wire format on [getAll]:
 *
 *  * `?col=value` — eq filter for `col` (one per call; specify each column at most once)
 *  * `?offset=N` / `?limit=M` — paging; both must be 0 or greater; `limit` <= [maxLimit]
 *  * `?sort=col:asc,col2:desc` — sort clauses; direction defaults to asc if omitted
 *  * `?count=true` — return only the matching row count as plain text
 *
 * Column names match the **SQL** column names (matching [Column.name]) — same keys used by ktorm-vaadin's
 * [EntityDataProvider] for grid sort. Values are coerced through each column's ktorm [org.ktorm.schema.SqlType]
 * (Int/Long/Boolean/LocalDate/Instant/enum recognized; everything else is passed as String).
 *
 * JSON bodies (on create/update) and JSON responses are keyed by the entity **property name** (not the SQL column
 * name) — that matches what a plain Gson serialization of a data-class DTO with the same property names produces, so
 * generic clients like `vok-rest-client`'s `CrudClient<DTO>` can round-trip without a custom adapter. [update] is
 * bound to PATCH and applies partial semantics: only columns whose property name appears in the JSON body are
 * touched. Property-value types pass through the configured [VokRest.gson], so the registered java.time TypeAdapters
 * cover [LocalDate]/[Instant], and enums round-trip by name.
 *
 * Errors return HTTP 400 with a plain-text reason: unknown columns, bad value coercion, bad sort direction, repeated
 * filter columns, bad offset/limit values, or malformed/unknown JSON body fields.
 *
 * @property table the ktorm Table backing this CRUD endpoint
 * @property allowModification if false, POST/PATCH/DELETE return 401
 * @property maxLimit maximum number of items [getAll] may return
 * @property defaultLimit limit when client omits the `limit` parameter; defaults to [maxLimit]
 */
public open class KtormCrudHandler<E : Entity<E>>(
    public val table: Table<E>,
    public val allowModification: Boolean = false,
    public val maxLimit: Long = Long.MAX_VALUE,
    public val defaultLimit: Long = maxLimit,
) : CrudHandler {

    private val idColumn: Column<*> = table.primaryKeys.singleOrNull()
        ?: error("Table '${table.tableName}' must have exactly one primary key column to use KtormCrudHandler")

    override fun getAll(ctx: Context) {
        val offset = ctx.queryParam(OFFSET)?.let {
            it.toLongOrNull()?.takeIf { o -> o >= 0 }
                ?: throw BadRequestResponse("invalid $OFFSET '$it'; must be 0 or greater")
        } ?: 0L
        val limit = ctx.queryParam(LIMIT)?.let {
            it.toLongOrNull()?.takeIf { l -> l in 0..maxLimit }
                ?: throw BadRequestResponse("invalid $LIMIT '$it'; must be 0..$maxLimit")
        } ?: defaultLimit

        val countOnly = ctx.queryParam(COUNT)?.let {
            it.toBooleanStrictOrNull()
                ?: throw BadRequestResponse("invalid $COUNT '$it'; must be true or false")
        } ?: false

        val sortClauses: List<OrderByExpression> = parseSort(ctx.queryParam(SORT))
        val filters: List<ColumnDeclaring<Boolean>> = parseFilters(
            ctx.queryParamMap().filterKeys { it !in RESERVED }
        )

        if (countOnly) {
            val count = db {
                val combined = if (filters.isNotEmpty()) filters.reduce(ColumnDeclaring<Boolean>::and) else null
                val seq = database.sequenceOf(table)
                if (combined != null) seq.filter { combined }.count() else seq.count()
            }
            ctx.result(count.toString())
            return
        }

        val rows: List<E> = db {
            val q = database.from(table).select()
            val q2 = if (filters.isNotEmpty()) q.where(filters.reduce(ColumnDeclaring<Boolean>::and)) else q
            val q3 = if (sortClauses.isNotEmpty()) q2.orderBy(*sortClauses.toTypedArray()) else q2
            q3.limit(offset.toInt(), limit.toInt()).map { table.createEntity(it) }
        }
        ctx.json(rows.map { it.toMap() })
    }

    private fun parseFilters(params: Map<String, List<String>>): List<ColumnDeclaring<Boolean>> =
        params.map { (colName, values) ->
            val column = lookupColumn(colName)
            val value = values.singleOrNull()
                ?: throw BadRequestResponse("filter column '$colName' specified ${values.size} times; supply at most one value")
            @Suppress("UNCHECKED_CAST")
            (column as Column<Any>) eq coerceFilterValue(column, value)
        }

    private fun parseSort(sortParam: String?): List<OrderByExpression> {
        if (sortParam.isNullOrBlank()) return emptyList()
        return sortParam.split(",").map { clause ->
            val parts = clause.split(":")
            val (colName, dir) = when (parts.size) {
                1 -> parts[0] to "asc"
                2 -> parts[0] to parts[1]
                else -> throw BadRequestResponse("invalid $SORT clause '$clause'; expected col[:asc|desc]")
            }
            val column = lookupColumn(colName)
            when (dir.lowercase()) {
                "asc" -> column.asc()
                "desc" -> column.desc()
                else -> throw BadRequestResponse("invalid $SORT direction '$dir' in clause '$clause'; expected asc or desc")
            }
        }
    }

    private fun lookupColumn(name: String): Column<*> =
        table.columns.firstOrNull { it.name == name }
            ?: throw BadRequestResponse("unknown column '$name'; allowed: ${table.columns.joinToString { it.name }}")

    private fun coerceFilterValue(column: Column<*>, raw: String): Any {
        val sqlType = column.sqlType
        return try {
            when {
                sqlType === IntSqlType -> raw.toInt()
                sqlType === LongSqlType -> raw.toLong()
                sqlType === BooleanSqlType -> raw.toBooleanStrict()
                sqlType === LocalDateSqlType -> LocalDate.parse(raw)
                sqlType === InstantSqlType -> Instant.parse(raw)
                sqlType is EnumSqlType<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    java.lang.Enum.valueOf(sqlType.enumClass as Class<out Enum<*>>, raw)
                }
                else -> raw
            }
        } catch (e: Exception) {
            throw BadRequestResponse("invalid value '$raw' for column ${column.name}: ${e.message ?: e::class.simpleName}")
        }
    }

    override fun getOne(ctx: Context, resourceId: String) {
        val id = parseId(resourceId)
        val entity = db {
            @Suppress("UNCHECKED_CAST")
            database.sequenceOf(table).find { (idColumn as Column<Any>) eq id }
        } ?: throw NotFoundResponse("No such entity with id $resourceId")
        ctx.json(entity.toMap())
    }

    /**
     * Snapshots a ktorm [Entity] into a plain [LinkedHashMap] keyed by the bound entity property names so Gson can
     * serialize it. ktorm's [Entity] is a JDK [java.lang.reflect.Proxy] which Gson can't reflect into, so we read
     * each property explicitly via the column's [NestedBinding].
     */
    @Suppress("UNCHECKED_CAST")
    private fun E.toMap(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for (col in table.columns) {
            val binding = col.binding as? NestedBinding ?: continue
            val prop = binding.properties.firstOrNull() as? KProperty1<E, Any?> ?: continue
            result[prop.name] = prop.get(this)
        }
        return result
    }

    /**
     * Builds an [Entity] from the JSON body, walking the same [NestedBinding]s as [toMap] in reverse: each bound
     * property whose name appears in the body is deserialized via [VokRest.gson] using the property's Kotlin return
     * type, then assigned through [Entity.set]. Properties absent from the body are left un-touched on the entity, so
     * downstream callers can distinguish "explicitly set to null" from "not in the request" via [Entity.properties].
     */
    private fun fromJson(ctx: Context): E {
        val body = ctx.body()
        if (body.isBlank()) throw BadRequestResponse("empty JSON body")
        val obj: JsonObject = try {
            VokRest.gson.fromJson(body, JsonObject::class.java)
        } catch (e: JsonSyntaxException) {
            throw BadRequestResponse("invalid JSON body: ${e.message ?: e::class.simpleName}")
        } ?: throw BadRequestResponse("JSON body must be an object")

        val entityClass = requireNotNull(table.entityClass) {
            "Table '${table.tableName}' has no entityClass; pass the KClass to the Table constructor"
        }
        @Suppress("UNCHECKED_CAST")
        val entity: E = Entity.create(entityClass) as E
        val boundPropertyNames: Set<String> = table.columns
            .mapNotNull { (it.binding as? NestedBinding)?.properties?.firstOrNull()?.name }
            .toSet()

        for ((key, _) in obj.entrySet().toList()) {
            if (key !in boundPropertyNames) {
                throw BadRequestResponse("unknown property '$key'; allowed: ${boundPropertyNames.joinToString()}")
            }
        }

        for (col in table.columns) {
            val binding = col.binding as? NestedBinding ?: continue
            val prop = binding.properties.firstOrNull() as? KProperty1<E, Any?> ?: continue
            if (!obj.has(prop.name)) continue
            val elem = obj.get(prop.name)
            val value: Any? = if (elem == null || elem.isJsonNull) {
                null
            } else try {
                VokRest.gson.fromJson<Any?>(elem, prop.returnType.javaType)
            } catch (e: Exception) {
                throw BadRequestResponse("invalid value for property '${prop.name}': ${e.message ?: e::class.simpleName}")
            }
            entity[prop.name] = value
        }
        return entity
    }

    override fun create(ctx: Context) {
        if (!allowModification) throw UnauthorizedResponse()
        val entity: E = fromJson(ctx)
        db { database.sequenceOf(table).add(entity) }
        ctx.status(201).json(entity.toMap())
    }

    override fun update(ctx: Context, resourceId: String) {
        if (!allowModification) throw UnauthorizedResponse()
        val id: Any = parseId(resourceId)
        val entity: E = fromJson(ctx)
        val touched: Set<String> = entity.properties.keys
        val idPropName: String? = (idColumn.binding as? NestedBinding)?.properties?.firstOrNull()?.name
        val touchedNonId: Set<String> = touched - listOfNotNull(idPropName).toSet()

        @Suppress("UNCHECKED_CAST")
        val existsClause: ColumnDeclaring<Boolean> = (idColumn as Column<Any>) eq id

        if (touchedNonId.isEmpty()) {
            // No-op PATCH: still 404 if the row doesn't exist, otherwise return its current state.
            val existing: E = db { database.sequenceOf(table).find { existsClause } }
                ?: throw NotFoundResponse("No such entity with id $resourceId")
            ctx.json(existing.toMap())
            return
        }

        val affected: Int = db {
            database.update(table) {
                for (col in table.columns) {
                    if (col === idColumn) continue
                    val binding = col.binding as? NestedBinding ?: continue
                    val prop = binding.properties.firstOrNull() as? KProperty1<E, Any?> ?: continue
                    if (prop.name !in touchedNonId) continue
                    @Suppress("UNCHECKED_CAST")
                    set(col as Column<Any>, prop.get(entity))
                }
                where { existsClause }
            }
        }
        if (affected == 0) throw NotFoundResponse("No such entity with id $resourceId")

        val refetched: E = db { database.sequenceOf(table).find { existsClause } }
            ?: throw NotFoundResponse("No such entity with id $resourceId")
        ctx.json(refetched.toMap())
    }

    override fun delete(ctx: Context, resourceId: String) {
        if (!allowModification) throw UnauthorizedResponse()
        val id: Any = parseId(resourceId)
        val affected: Int = db {
            @Suppress("UNCHECKED_CAST")
            database.delete(table) { (idColumn as Column<Any>) eq id }
        }
        if (affected == 0) throw NotFoundResponse("No such entity with id $resourceId")
    }

    private fun parseId(raw: String): Any = try {
        when (val st = idColumn.sqlType) {
            LongSqlType -> raw.toLong()
            IntSqlType -> raw.toInt()
            else -> if (st === LongSqlType) raw.toLong() else raw
        }
    } catch (e: NumberFormatException) {
        throw NotFoundResponse("Malformed ID: $raw")
    }

    private companion object {
        const val OFFSET = "offset"
        const val LIMIT = "limit"
        const val SORT = "sort"
        const val COUNT = "count"
        val RESERVED: Set<String> = setOf(OFFSET, LIMIT, SORT, COUNT)
    }
}
