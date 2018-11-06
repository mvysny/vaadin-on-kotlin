[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# VoK REST support

This module makes it easy to export your objects via REST. The aim here is to use as lightweight libraries as possible,
that's why we're using [Javalin](https://javalin.io/) for REST endpoint definition, and [Gson](https://github.com/google/gson) for object-to-JSON mapping instead of
Jackson.

> Note: this module does not have any support for your app to *consume* and *display* data from an external REST services.
Please follow the [Accessing NoSQL or REST data sources](http://www.vaadinonkotlin.eu/nosql_rest_datasources.html) guide for more information.

## Adding REST to your app

Include dependency on this module to your app; just add the following Gradle dependency to your `build.gradle`:

```groovy
dependencies {
    compile "com.github.vok:vok-rest:x.y.z"
}
```

> Note: to obtain the newest version see above for the most recent tag

Now you can write the REST endpoint interface. We are going to introduce a new servlet that will handle all REST-related calls;
we're going to reroute all calls to Javalin which will then parse REST requests; we're then going to configure Javalin with our REST
endpoints:

```kotlin
/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin = EmbeddedJavalin()
            .configureRest()
            .createServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    val gson = GsonBuilder().create()
    gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    get("/rest/person/helloworld2") { ctx -> ctx.json(Person.findAll()) }  // uses Gson
    crud("/rest/person", Person.getCrudHandler(true))
    return this
}
```

To test it out, just run the following in your command line:

```bash
curl http://localhost:8080/rest/person
```

This should hit the route defined via the `crud("/rest/person")` and should print all personnel in your database.

Please consult [Javalin Documentation](https://javalin.io/documentation) for more details on how to configure REST endpoints.

### Testing your REST endpoints

You can easily start Javalin with Jetty which would serve your endpoints on an actual http server. You need to add the following dependencies:

```gradle
dependencies {
    testCompile("khttp:khttp:0.1.0") {
        exclude(mapOf("group" to "org.json"))
    }
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")
}
```

This will add Jetty for booting up a testing Javalin server; we're going to access the REST endpoints via the [khttp](https://khttp.readthedocs.io/en/latest/) library.

The testing file will look like this:

```kotlin
fun Response.checkOk(): Response {
    if (statusCode !in 200..299) throw IOException("$statusCode: $text ($url)")
    return this
}

class PersonRestClient(val baseUrl: String) {
    init {
        require(!baseUrl.endsWith("/")) { "$baseUrl must not end with a slash" }
    }
    val gson: Gson = GsonBuilder().create()
    fun helloWorld(): String = khttp.get("$baseUrl/person/helloworld").checkOk().text
    fun getAll(): List<Person> {
        val text = khttp.get("$baseUrl/person").checkOk().text
        val type = TypeToken.getParameterized(List::class.java, Person::class.java).type
        return gson.fromJson<List<Person>>(text, type)
    }
}

class PersonRestTest : DynaTest({
    lateinit var javalin: Javalin
    beforeGroup {
        javalin = Javalin.create().disableStartupBanner()
        javalin.configureRest().start(9876)
    }
    afterGroup { javalin.stop() }

    usingApp()  // to bootstrap the app to have access to the database.

    lateinit var client: PersonRestClient
    beforeEach { client = PersonRestClient("http://localhost:9876/rest") }

    test("hello world") {
        expect("Hello World") { client.helloWorld() }
    }

    test("get all users") {
        expectList() { client.getAll() }
        val p = Person(personName = "Duke Leto Atreides", age = 45, dateOfBirth = LocalDate.of(1980, 5, 1), maritalStatus = MaritalStatus.Single, alive = false)
        p.save()
        expectList(p) { client.getAll() }
    }
})
```

Please consult the [vok-example-crud-sql2o](../vok-example-crud-sql2o) example project for more info.

## Customizing JSON mapping

Gson by default only export non-transient fields. It only exports actual Java fields, or only Kotlin properties that are backed by actual fields;
it ignores computed Kotlin properties such as `val reviews: List<Review> get() = Review.findAll()`.

Please see [Gson User Guide](https://github.com/google/gson/blob/master/UserGuide.md) for more details.

### Deprecated: Old Way With RESTEasy and Annotations

This way of exposing REST services is now deprecated and superseded by the Javalin functional way. This will go away; if you still need this you need to add a dependency
to [RESTEasy](http://resteasy.jboss.org/) in addition to vok-rest:

```groovy
dependencies {
    compile("org.jboss.resteasy:resteasy-servlet-initializer:3.6.1.Final")
}
```

To activate the REST service you will need to create the following class:

```kotlin
@ApplicationPath("/rest")
class ApplicationConfig : Application()
```

Then, to export a particular data just create the following class:

```kotlin
/**
 * Provides access to the database. To test, just run `curl http://localhost:8080/rest/categories`
 */
@Path("/")
class RestService {

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllCategories(): List<Category> = Category.findAll()

    @GET
    @Path("/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllReviews(): List<Review> = Review.findAll()
}
```

Please read more at [RESTEasy User Guide](http://docs.jboss.org/resteasy/docs/3.5.0.Final/userguide/html/Using_Path.html).
