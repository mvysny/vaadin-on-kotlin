package eu.vaadinonkotlin.restclient

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
 * Uses the VoK CRUD endpoint and serves instances of [itemClass] over the given [client] using [VokRestClient.gson].
 *
 * Endpoints (matching [eu.vaadinonkotlin.rest.KtormCrudHandler]):
 *
 *  * `GET /rest/users`              — list users
 *  * `GET /rest/users?count=true`   — count, used by [sizeInBackEnd]
 *  * `GET /rest/users/22`           — single user
 *  * `POST /rest/users`             — create
 *  * `PATCH /rest/users/22`         — update
 *  * `DELETE /rest/users/22`        — delete
 *
 * Wire format:
 *
 *  * `?col=value` for eq filters — caller pre-formats values as Strings
 *  * `?offset=N&limit=M` paging
 *  * `?sort=col:asc,col2:desc` multi-column sort
 *
 * Filter columns matching one of the reserved control names (`offset`, `limit`, `sort`, `count`) will throw
 * [IllegalArgumentException] from [getAll].
 *
 * The data provider's filter type is [Map] of column name → value. Pass `dataProvider.setFilter(mapOf("name" to "Leto"))`
 * to apply filters from grid code.
 *
 * @property baseUrl base URL ending with `/`, e.g. `http://localhost:8080/rest/users/`
 * @property itemClass the entity class to deserialize JSON into
 * @property client which HTTP client to use; defaults to [VokRestClient.httpClient]
 */
public open class CrudClient<T : Any>(
    public val baseUrl: String,
    public val itemClass: Class<T>,
    public val client: HttpClient = VokRestClient.httpClient,
) : AbstractBackEndDataProvider<T, Map<String, String>>() {

    init {
        require(baseUrl.endsWith("/")) { "$baseUrl must end with /" }
    }

    /**
     * Fetches data from the back end.
     * @param filter eq filters as column → value pairs; values are passed verbatim as query parameters
     * @param sortBy zero or more sort orders applied in priority order
     * @param offset 0 or greater
     * @param limit 0 or greater, capped by server's maxLimit
     */
    public fun getAll(
        filter: Map<String, String> = emptyMap(),
        sortBy: List<QuerySortOrder> = listOf(),
        offset: Long = 0,
        limit: Long = Long.MAX_VALUE
    ): List<T> {
        val url: URI = baseUrl.buildUrl {
            addParameter("offset", offset.toString())
            addParameter("limit", limit.toString())
            if (sortBy.isNotEmpty()) {
                addParameter("sort", sortBy.joinToString(",") { it.toWire() })
            }
            addFilters(filter)
        }
        val request: HttpRequest = url.buildRequest()
        return client.exec(request) { response -> response.jsonArray(itemClass) }
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

    override fun fetchFromBackEnd(query: Query<T, Map<String, String>>): Stream<T> {
        val sortBy: List<QuerySortOrder> = query.sortOrders ?: listOf()
        val filter: Map<String, String> = query.filter.orElse(emptyMap())
        return getAll(filter, sortBy, query.offset.toLong(), query.limit.toLong()).stream()
    }

    override fun sizeInBackEnd(query: Query<T, Map<String, String>>): Int {
        val filter: Map<String, String> = query.filter.orElse(emptyMap())
        val url: URI = baseUrl.buildUrl {
            addParameter("count", "true")
            addFilters(filter)
        }
        val request: HttpRequest = url.buildRequest()
        return client.exec(request) { response -> response.body().bufferedReader().readText().trim().toInt() }
    }

    private fun URIBuilder.addFilters(filter: Map<String, String>) {
        for ((col, value) in filter) {
            require(col !in RESERVED) { "filter column '$col' clashes with reserved control parameter name" }
            addParameter(col, value)
        }
    }

    private fun QuerySortOrder.toWire(): String =
        "${sorted}:${if (direction == SortDirection.ASCENDING) "asc" else "desc"}"

    private companion object {
        val RESERVED: Set<String> = setOf("offset", "limit", "sort", "count")
    }
}
