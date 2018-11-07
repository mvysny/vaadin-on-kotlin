[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# VoK REST Client Support

This module makes it easy to consume REST endpoint exported by another party. We're using the [okhttp](http://square.github.io/okhttp/)
library, and you can optionally use the [Retrofit](https://square.github.io/retrofit/) support though this is not recommended.

> Note: to expose your objects from your app via REST please see the [vok-rest](../vok-rest) module.

## Adding REST Client To Your App

Include dependency on this module to your app; just add the following Gradle dependency to your `build.gradle`:

```groovy
dependencies {
    compile "com.github.vok:vok-rest-client:x.y.z"
}
```

> Note: to obtain the newest version see above for the most recent tag

Now you can write the REST client. The recommended method is to use `okhttp` directly; you can also use `Retrofit` to generate REST clients out of
your annotated interfaces but that's not really recommended.

## Using `okhttp`

```kotlin
// Demoes direct access via okhttp
class PersonRestClient2(val baseUrl: String) {
    private val client: OkHttpClient = RetrofitClientVokPlugin.okHttpClient!!
    fun helloWorld(): String {
        val request = Request.Builder().url("${baseUrl}helloworld").build()
        return client.exec(request) { response -> response.string() }
    }
    fun getAll(): List<Person> {
        val request = Request.Builder().url(baseUrl).build()
        return client.exec(request) { response -> response.jsonArray(Person::class.java) }
    }
}
val client = PersonRestClient2("http://localhost:8080/rest/person/")
println(client.getAll())
```

## Retrofit

Retrofit uses okhttp under the belt but allows you to create client out of annotated interfaces. Might save you a few keystrokes, but makes it
impossible to debug. This way is therefore not recommended.

For precise instructions on how to construct REST client interfaces for Retrofit please visit the [Retrofit page](https://square.github.io/retrofit/).

Example:
```kotlin
interface PersonRestClient {
    @GET("helloworld")
    @Throws(IOException::class)
    fun helloWorld(): String

    @GET(".")
    @Throws(IOException::class)
    fun getAll(): List<Person>
}

val client = createRetrofit("http://localhost:8080/rest/person/").create(PersonRestClient::class.java)
println(client.getAll())
```

Retrofit is automatically configured by `vok-rest-client` to properly fail on result code other than 200..299; it is configured to properly
handle any type of values. Retrofit doesn't support functions returning `Unit` or `void` - just make the function return `Unit?`.

## Customizing JSON mapping

Gson by default only export non-transient fields. It only exports actual Java fields, or only Kotlin properties that are backed by actual fields;
it ignores computed Kotlin properties such as `val reviews: List<Review> get() = Review.findAll()`.
To reconfigure Gson, just set a new instance to `RetrofitClientVokPlugin.gson`. To reconfigure `OkHttpClient` just set `RetrofitClientVokPlugin.okHttpClient`
before calling `VaadinOnKotlin.init()`.

Please see [Gson User Guide](https://github.com/google/gson/blob/master/UserGuide.md) for more details.
