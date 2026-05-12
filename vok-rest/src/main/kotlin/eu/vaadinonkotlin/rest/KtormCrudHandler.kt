package eu.vaadinonkotlin.rest

import com.github.mvysny.ktormvaadin.db
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.totalRecords
import org.ktorm.dsl.where
import org.ktorm.entity.Entity
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
import org.ktorm.schema.Table
import java.time.Instant
import java.time.LocalDate

/**
 * A Javalin [CrudHandler] backed by a ktorm [Table]. Implements only the read endpoints — POST/PATCH/DELETE return
 * 401 unless [allowModification] is true (and even then they currently return 501 pending a ktorm-Entity Gson adapter).
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
 * Errors return HTTP 400 with a plain-text reason: unknown columns, bad value coercion, bad sort direction, repeated
 * filter columns, or bad offset/limit values.
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
                val base = database.from(table).select(idColumn)
                val filtered = if (filters.isNotEmpty()) base.where(filters.reduce(ColumnDeclaring<Boolean>::and)) else base
                filtered.totalRecords
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
        ctx.json(rows)
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
        ctx.json(entity)
    }

    override fun create(ctx: Context) {
        if (!allowModification) throw UnauthorizedResponse()
        throw NotImplementedResponse("create not yet supported — needs Gson↔ktorm Entity adapter")
    }

    override fun update(ctx: Context, resourceId: String) {
        if (!allowModification) throw UnauthorizedResponse()
        throw NotImplementedResponse("update not yet supported — needs Gson↔ktorm Entity adapter")
    }

    override fun delete(ctx: Context, resourceId: String) {
        if (!allowModification) throw UnauthorizedResponse()
        throw NotImplementedResponse("delete not yet supported")
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

/** Javalin lacks a dedicated 501 response, so we use a [BadRequestResponse]-shaped exception with status 501. */
internal class NotImplementedResponse(message: String) : io.javalin.http.HttpResponseException(501, message)
