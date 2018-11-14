package eu.vaadinonkotlin.restclient

import com.github.vokorm.*
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.SortClause
import okhttp3.*
import retrofit2.Converter
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.util.*

/**
 * Uses the CRUD endpoint and serves instances of given item of type [itemClass] over given [client] using [OkHttpClientVokPlugin.gson].
 * Expect the CRUD endpoint to be exposed in the following manner:
 * * `GET /rest/users` returns all users
 * * `GET /rest/users?select=count` returns a single number - the count of all users. This is only necessary for [getCount]
 * or if you plan to use this client as a backend for Vaadin Grid.
 * * `GET /rest/users/22` returns one users
 * * `POST /rest/users` will create an user
 * * `PATCH /rest/users/22` will update an user
 * * `DELETE /rest/users/22` will delete an user
 *
 * Paging/sorting/filtering is supported: the following query parameters will simply be added to the "get all" URL request:
 *
 * * `limit` and `offset` for result paging. Both must be 0 or greater. The server may impose max value limit on the `limit` parameter.
 * * `sort_by=-last_modified,+email,first_name` - a list of sorting clauses. The server may restrict sorting by only a selected subset of properties.
 * * The filters are simply converted to query parameters, for example `age=81`. [OpFilter]s are also supported: the value will be prefixed with a special operator prefix:
 * `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. A full example is `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`.
 * OR filters are not supported - passing [OrFilter] will cause [getAll] to throw [IllegalArgumentException].
 *
 * All column names are expected to be Kotlin [kotlin.reflect.KProperty1.name] of the entity in question.
 *
 * Since this client is also a [DataLoader], you can use the `DataLoaderAdapter` class from the `vok-framework-sql2o`/`vok-framework-v10-sql2o`
 * module to turn this client into a Vaadin `DataProvider` which you can then feed into Vaadin Grid or ComboBox etc:
 * ```
 * val crud = CrudClient("http://localhost:8080/rest/person/", Person::class.java)
 * val dp = DataLoaderAdapter(Person::class.java, crud, { it.id!! }).withConfigurableFilter2()
 * grid.dataProvider = dp
 * ```
 * @param baseUrl the base URL, such as `http://localhost:8080/rest/users/`, must end with a slash.
 * @param converter used to convert filter values to strings passable as query parameters. Defaults to [QueryParameterConverter] with system-default
 * zone; it is pretty much recommended to set a specific time zone.
 */
class CrudClient<T: Any>(val baseUrl: String, val itemClass: Class<T>,
                         val client: OkHttpClient = OkHttpClientVokPlugin.okHttpClient!!,
                         val converter: Converter<in Any, String> = QueryParameterConverter()) : DataLoader<T> {
    init {
        require(baseUrl.endsWith("/")) { "$baseUrl must end with /" }
    }

    private val dbFieldNameToPropertyName = itemClass.entityMeta.properties.associate { it.dbColumnName to it.name }

    /**
     * Fetches data from the back end. The items must match given [filter]. This function does exactly the same as [fetch].
     * @param filter optional filter which defines filtering to be used for counting the
     * number of items. If null all items are considered.
     * @param sortBy optionally sort the beans according to given fields. By default sorts ASC; if you prepend the field with the "-"
     * character the sorting will be DESC.
     * @param range offset and limit to fetch
     * @return a list of items matching the query, may be empty.
     */
    fun getAll(filter: Filter<in T>? = null, sortBy: List<SortClause> = listOf(), range: LongRange = 0..Long.MAX_VALUE): List<T> {
        val url = buildUrl(baseUrl) {
            if (range != 0..Long.MAX_VALUE) {
                addQueryParameter("offset", range.first.toString())
                addQueryParameter("limit", range.length.toString())
            }
            if (!sortBy.isEmpty()) {
                addQueryParameter("sort_by", sortBy.joinToString(",") { "${if(it.asc)"+" else "-"}${it.columnName}" })
            }
            if (filter != null) {
                addFilterQueryParameters(filter)
            }
        }
        val request = Request.Builder().url(url).build()
        return client.exec(request) { response -> response.jsonArray(itemClass) }
    }

    private fun HttpUrl.Builder.addFilterQueryParameters(filter: Filter<in T>) {

        fun opToRest(op: CompareOperator): String = when (op) {
            CompareOperator.eq -> "eq"
            CompareOperator.ge -> "gte"
            CompareOperator.gt -> "gt"
            CompareOperator.le -> "lte"
            CompareOperator.lt -> "lt"
        }

        if (filter is BeanFilter) {
            val propName = requireNotNull(dbFieldNameToPropertyName[filter.databaseColumnName]) {
                "Unknown dbFieldName ${filter.databaseColumnName} for $itemClass, available properties: ${itemClass.entityMeta.properties}"
            }
            require(propName != "limit" && propName != "offset" && propName != "sort_by" && propName != "select") {
                "cannot filter on reserved query parameter name $propName"
            }
            val value = if (filter.value == null) null else converter.convert(filter.value!!)
            val restValue = when (filter) {
                is EqFilter -> value
                is IsNotNullFilter -> "isnotnull:"
                is IsNullFilter -> "isnull:"
                is LikeFilter -> "like:$value"
                is ILikeFilter ->  "ilike:$value"
                is OpFilter -> "${opToRest(filter.operator)}:$value"
                else -> throw IllegalArgumentException("Unsupported filter $filter")
            }
            addQueryParameter(propName, restValue)
        } else {
            when (filter) {
                is AndFilter -> filter.children.forEach { addFilterQueryParameters(it) }
                else -> throw IllegalArgumentException("Unsupported filter $filter")
            }
        }
    }

    fun getOne(id: String): T {
        val request = Request.Builder().url("$baseUrl$id").build()
        return client.exec(request) { response -> response.json(itemClass) }
    }

    fun create(entity: T) {
        val body = RequestBody.create(mediaTypeJson, OkHttpClientVokPlugin.gson.toJson(entity))
        val request = Request.Builder().post(body).url(baseUrl).build()
        client.exec(request) {}
    }

    fun update(id: String, entity: T) {
        val body = RequestBody.create(mediaTypeJson, OkHttpClientVokPlugin.gson.toJson(entity))
        val request = Request.Builder().patch(body).url("$baseUrl$id").build()
        client.exec(request) {}
    }

    fun delete(id: String) {
        val request = Request.Builder().delete().url("$baseUrl$id").build()
        client.exec(request) {}
    }

    private fun buildUrl(baseUrl: String, block: HttpUrl.Builder.()->Unit): HttpUrl {
        val url = requireNotNull(HttpUrl.parse(baseUrl)) { "Unparsable url: $baseUrl" }
        return url.newBuilder().apply { block() } .build()!!
    }

    companion object {
        val mediaTypeJson = MediaType.parse("application/json; charset=utf-8")
    }

    override fun fetch(filter: Filter<T>?, sortBy: List<SortClause>, range: IntRange): List<T> = getAll(filter, sortBy, range.first.toLong()..range.endInclusive.toLong())

    override fun getCount(filter: Filter<T>?): Int {
        val url = buildUrl("$baseUrl?select=count") {
            if (filter != null) {
                addFilterQueryParameters(filter)
            }
        }
        val request = Request.Builder().url(url).build()
        return client.exec(request) { response -> response.string().toInt() }
    }
}

/**
 * Converts values of different types properly to String, so that they can be consumed by the REST endpoints.
 * The default implementation uses the following algorithm:
 * * Converts all [Number] to their decimal format with comma as the decimal separator, e.g. "54" or "2.25"
 * * Convert all Date-like objects such as [Date], [LocalDate] and [LocalDateTime] to the UTC Epoch long (number of milliseconds since January 1, 1970, 00:00:00 GMT in UTC).
 * * Fails for everything else.
 * @param zoneId used to convert [LocalDate] and [LocalDateTime] to UTC Epoch.
 */
open class QueryParameterConverter(val zoneId: ZoneId = ZoneId.systemDefault()) : Converter<Any, String> {
    protected fun convertNumber(number: Number): String = when(number) {
        is Int, is Short, is Byte, is Long, is BigInteger -> number.toString()
        is BigDecimal -> number.stripTrailingZeros().toPlainString()
        is Float -> convertNumber(number.toDouble())
        is Double -> convertNumber(number.toBigDecimal())
        else -> throw IllegalArgumentException("$number of type ${number.javaClass} is not supported")
    }

    override fun convert(value: Any): String = when(value) {
        is String -> value
        is Number -> convertNumber(value)
        is Date -> value.time.toString()
        is LocalDate -> convert(LocalDateTime.of(value, LocalTime.MIN))
        is LocalDateTime -> convert(value.atZone(zoneId))
        is ZonedDateTime -> convert(value.toInstant())
        is Instant -> value.toEpochMilli().toString()
        else -> throw IllegalArgumentException("$value of type ${value.javaClass} is not supported")
    }
}
