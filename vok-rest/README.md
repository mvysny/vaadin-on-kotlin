[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-rest/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-rest)

# VoK REST Server Support

This module exposes ktorm `Table<E>` objects as a REST CRUD endpoint. It is intentionally lightweight: [Javalin](https://javalin.io/)
for routing, [Gson](https://github.com/google/gson) for JSON.

> Note: this module does not consume external REST services. For that, see [vok-rest-client](../vok-rest-client).

## Adding the REST server to your app

```kotlin
dependencies {
    implementation("eu.vaadinonkotlin:vok-rest:x.y.z")
}
```

Define a servlet that delegates everything under `/rest/*` to Javalin, and register a CRUD handler for each ktorm
`Table` you want to expose:

```kotlin
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    private val javalin = Javalin.createStandalone { it.gsonMapper(VokRest.gson) } .apply {
        get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
        crud2("/rest/person", Persons.getCrudHandler(allowModification = true))
    }.javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}
```

Quick check from the command line:

```bash
curl http://localhost:8080/rest/person
```

Consult the [Javalin docs](https://javalin.io/documentation) for general route definitions.

## CRUD handler

`Persons.getCrudHandler()` returns a [KtormCrudHandler](src/main/kotlin/eu/vaadinonkotlin/rest/KtormCrudHandler.kt)
backed by your ktorm `Table`. Mounted at `/rest/users` it exposes:

* `GET /rest/users` — list (with query parameters below)
* `GET /rest/users/22` — single by id
* `POST /rest/users` / `PATCH /rest/users/22` / `DELETE /rest/users/22` — create / update / delete
  (gated by `allowModification`; the body-deserialization path currently returns 501, pending a Gson↔ktorm Entity
  adapter)

### Query parameters on `GET /list`

* `?col=value` — eq filter on a column. Column names are the **SQL** column names (same keys ktorm-vaadin's
  EntityDataProvider uses for grid sorting). Pass each column at most once.
* `?offset=N&limit=M` — paging; both must be 0 or greater; `limit` is capped by the handler's `maxLimit`.
* `?sort=col1:asc,col2:desc` — multi-column sort; direction defaults to `asc` if `:asc`/`:desc` is omitted.
* `?count=true` — return only the matching row count as a plain-text integer.

Value coercion follows the column's ktorm `SqlType`: `Int`, `Long`, `Boolean`, `LocalDate`, `Instant`, and
`enum<>()` columns are recognized; everything else passes through as a String. Unknown columns, bad coercions, and
bad sort directions return **HTTP 400** with a plain-text reason.

This wire format is intentionally simpler than the previous `like:` / `ilike:` / `lt:` / `gt:` algebra — eq-only on
the wire, with anything richer expressed server-side in your own routes.

## Testing REST endpoints

Spin up Jetty in your test setup and use [vok-rest-client](../vok-rest-client) as the HTTP layer. Add the test
dependencies:

```kotlin
testImplementation("org.eclipse.jetty.ee10:jetty-ee10-webapp:12.x.x")
testImplementation("org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server:12.x.x")
testImplementation(project(":vok-rest-client"))
```

Then:

```kotlin
class PersonRestClient(val baseUrl: String) {
    private val client: HttpClient = VokRestClient.httpClient
    fun helloWorld(): String =
        client.exec("${baseUrl}helloworld".buildUrl().buildRequest()) { it.bodyAsString() }
    fun getAll(): List<PersonDto> =
        client.exec(baseUrl.buildUrl().buildRequest()) { it.jsonArray(PersonDto::class.java) }
}

class PersonRestTest : AbstractJavalinTest() {
    @Test fun helloWorld() {
        val client = PersonRestClient("http://localhost:9876/rest/person/")
        expect("Hello World") { client.helloWorld() }
    }
}
```

`PersonDto` is a plain Kotlin data class mirroring the JSON shape — Gson cannot deserialize the ktorm `Entity<E>`
interface directly, so the client side uses a DTO with `var` properties matching the entity's property names.

For a fully wired example, see [PersonRestTest](src/test/kotlin/eu/vaadinonkotlin/rest/PersonRestTest.kt) in this
module and [PersonRestTest](../vok-example-crud/src/test/kotlin/example/crudflow/PersonRestTest.kt) in the demo.

## Customizing JSON mapping

A ktorm `Entity<E>` is a `Map<String, Any?>`. Gson's default Map serialization picks up registered `TypeAdapter`s
for value types (so `LocalDate`, `Instant`, etc. format correctly thanks to `gson-javatime-serialisers`, which
`VokRest.gson` already registers).

If you need to customize the output, override `VokRest.gson` early in your boot sequence. See the
[Gson User Guide](https://github.com/google/gson/blob/master/UserGuide.md).
