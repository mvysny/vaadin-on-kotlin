# VoK REST support

This module makes it easy to export your objects via REST. The aim here is to use as lightweight libraries as possible,
that's why we're using [RESTEasy](http://resteasy.jboss.org/) instead of Jersey, and [Gson](https://github.com/google/gson) for object-to-JSON mapping instead of
Jackson.

## Adding REST to your app

Include dependency on this module to your app; just add the following Gradle dependency to your `build.gradle`:

```groovy
dependencies {
    compile "com.github.vok:vok-rest:0.4.1"
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

## Customizing JSON mapping

Gson by default only export non-transient fields. It only exports actual Java fields, or only Kotlin properties that are backed by actual fields;
it ignores computed Kotlin properties such as `val reviews: List<Review> get() = Review.findAll()`.

Please see [Gson User Guide](https://github.com/google/gson/blob/master/UserGuide.md) for more details.
