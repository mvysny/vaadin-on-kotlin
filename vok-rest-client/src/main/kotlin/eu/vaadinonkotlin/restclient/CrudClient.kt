package eu.vaadinonkotlin.restclient

import com.gitlab.mvysny.jdbiorm.OrderBy
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.condition.And
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.condition.Eq
import com.gitlab.mvysny.jdbiorm.condition.Expression
import com.gitlab.mvysny.jdbiorm.condition.FullTextCondition
import com.gitlab.mvysny.jdbiorm.condition.IsNotNull
import com.gitlab.mvysny.jdbiorm.condition.IsNull
import com.gitlab.mvysny.jdbiorm.condition.Like
import com.gitlab.mvysny.jdbiorm.condition.LikeIgnoreCase
import com.gitlab.mvysny.jdbiorm.condition.Op
import com.gitlab.mvysny.uribuilder.net.URIBuilder
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import eu.vaadinonkotlin.MediaType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.util.stream.Stream

/**
 * Uses the CRUD endpoint and serves instances of given item of type [itemClass] over given [client] using [VokRestClient.gson].
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
 * * The filters are simply converted to query parameters, for example `age=81`. [Op]s are also supported: the value will be prefixed with a special operator prefix:
 * `eq:`, `lt:`, `lte:`, `gt:`, `gte:`, `ilike:`, `like:`, `isnull:`, `isnotnull:`, for example `age=lt:25`. A full example is `name=ilike:martin&age=lte:70&age=gte:20&birthdate=isnull:&grade=5`.
 * OR filters are not supported - passing [Or] will cause [getAll] to throw [IllegalArgumentException].
 *
 * Since this client is also a [DataProvider], you can use this class to feed data into Vaadin Grid or ComboBox etc:
 * ```
 * val crud = CrudClient("http://localhost:8080/rest/person/", Person::class.java)
 * grid.dataProvider = crud
 * ```
 * @property baseUrl the base URL, such as `http://localhost:8080/rest/users/`, must end with a slash.
 * @property converter used to convert filter values to strings passable as query parameters. Defaults to [QueryParameterConverter] with system-default
 * zone; it is pretty much recommended to set a specific time zone.
 * @property client which HTTP client to use, defaults to [VokRestClient.httpClient].
 * @property converter converts filter values to strings when querying for data.
 * Defaults to [QueryParameterConverter].
 */
public open class CrudClient<T: Any>(
    public val baseUrl: String,
    public val itemClass: Class<T>,
    public val client: HttpClient = VokRestClient.httpClient!!,
    public val converter: Converter<in Any, String> = QueryParameterConverter()) : AbstractBackEndDataProvider<T, Condition>() {
    init {
        require(baseUrl.endsWith("/")) { "$baseUrl must end with /" }
    }

    /**
     * Fetches data from the back end. The items must match given [filter]. This function does exactly the same as [fetch].
     * @param filter optional filter which defines filtering to be used for counting the
     * number of items. If null all items are considered.
     * @param sortBy optionally sort the beans according to given fields. By default sorts ASC; if you prepend the field with the "-"
     * character the sorting will be DESC.
     * @param range offset and limit to fetch
     * @return a list of items matching the query, may be empty.
     */
    private fun getAll(filter: Condition = Condition.NO_CONDITION, sortBy: List<OrderBy> = listOf(), offset: Long, limit: Long): List<T> {
        val url: URI = baseUrl.buildUrl {
            addParameter("offset", offset.toString())
            addParameter("limit", limit.toString())
            if (sortBy.isNotEmpty()) {
                addParameter("sort_by", sortBy.joinToString(",") { "${if(it.order == OrderBy.Order.ASC)"+" else "-"}${it.property.name}" })
            }
            addFilterQueryParameters(filter)
        }
        val request: HttpRequest = url.buildRequest()
        return client.exec(request) { response -> response.jsonArray(itemClass) }
    }

    private fun URIBuilder.addFilterQueryParameters(condition: Condition) {
        fun opToRest(op: Op.Operator): String = when (op) {
            Op.Operator.EQ -> "eq"
            Op.Operator.GE -> "gte"
            Op.Operator.GT -> "gt"
            Op.Operator.LE -> "lte"
            Op.Operator.LT -> "lt"
            Op.Operator.NE -> "ne"
        }

        fun Expression<*>.getPropertyName(): String {
            val propName = (this as Property<*>).name.name
            require(propName != "limit" && propName != "offset" && propName != "sort_by" && propName != "select") {
                "cannot filter on reserved query parameter name $propName"
            }
            return propName
        }
        fun Expression<*>.getValue(): String? {
            val value = (this as Expression.Value<*>).value ?: return null
            return converter.convert(value)
        }

        when {
            condition == Condition.NO_CONDITION -> return
            condition is Eq -> addParameter(condition.arg1.getPropertyName(), condition.arg2.getValue())
            condition is IsNotNull -> addParameter(condition.arg.getPropertyName(), "isnotnull:")
            condition is IsNull -> addParameter(condition.arg.getPropertyName(), "isnull:")
            condition is Like -> addParameter(condition.arg1.getPropertyName(), "like:" + condition.arg2.getValue())
            condition is LikeIgnoreCase -> addParameter(condition.arg1.getPropertyName(), "ilike:" + condition.arg2.getValue())
            condition is Op -> addParameter(condition.arg1.getPropertyName(), opToRest(condition.operator) + ":" + condition.arg2.getValue())
            condition is FullTextCondition -> addParameter(condition.arg.getPropertyName(), "fulltext:" + condition.query)
            condition is And -> { addFilterQueryParameters(condition.condition1); addFilterQueryParameters(condition.condition2) }
            else -> throw IllegalArgumentException("Unsupported condition $condition")
        }
    }

    public fun getOne(id: String): T {
        val request: HttpRequest = "$baseUrl$id".buildUrl().buildRequest()
        return client.exec(request) { response -> response.json(itemClass) }
    }

    public fun create(entity: T) {
        val json: String = VokRestClient.gson.toJson(entity)
        val request: HttpRequest = baseUrl.buildUrl().buildRequest { post(json, MediaType.jsonUtf8) }
        client.exec(request) {}
    }

    public fun update(id: String, entity: T) {
        val json: String = VokRestClient.gson.toJson(entity)
        val request: HttpRequest = "$baseUrl$id".buildUrl().buildRequest { patch(json, MediaType.jsonUtf8) }
        client.exec(request) {}
    }

    public fun delete(id: String) {
        val request: HttpRequest = "$baseUrl$id".buildUrl().buildRequest { DELETE() }
        client.exec(request) {}
    }

    private val QuerySortOrder.orderBy: OrderBy get() = OrderBy.of(itemClass, sorted, if (direction == SortDirection.ASCENDING) OrderBy.Order.ASC else OrderBy.Order.DESC)
    private val Query<*, *>.range: LongRange get() = offset.toLong() .. (offset + limit).toLong()

    override fun fetchFromBackEnd(query: Query<T, Condition>): Stream<T> {
        val sortBy: List<OrderBy> = (query.sortOrders ?: listOf()).map { it.orderBy }
        val condition: Condition = query.filter.orElse(Condition.NO_CONDITION)
        val result = getAll(condition, sortBy, query.offset.toLong(), query.limit.toLong())
        return result.stream()
    }

    override fun sizeInBackEnd(query: Query<T, Condition>): Int {
        val condition: Condition = query.filter.orElse(Condition.NO_CONDITION)
        val request: HttpRequest = "$baseUrl?select=count".buildUrl {
            addFilterQueryParameters(condition)
        }.buildRequest()
        return client.exec(request) { response -> response.body().reader().readText().toInt() }
    }
}
