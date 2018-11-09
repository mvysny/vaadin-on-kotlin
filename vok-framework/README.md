[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# VoK Framework Core

The core module of the Vaadin-on-Kotlin framework, always included in your projects, typically
as a transitive dependency of other modules as they are included in your project.

This module provides the means to bootstrap/teardown the VoK runtime, typically from your
`ServletContextListener` as follows:

```kotlin
@WebListener
class Bootstrap: ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        VaadinOnKotlin.init()
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        VaadinOnKotlin.destroy()
    }
}
```

This will initialize all VoK plugins properly. The VoK plugins also tend to add additional fields
to the `VaadinOnKotlin` object. For example the `vok-db` module adds the
`VaadinOnKotlin.dataSourceConfig` property which allows you to specify the JDBC URL, username, password,
JDBC connection pool configuration and other.

## Initializing VoK from your tests

It is very easy to initialize VoK in your JUnit's `@BeforeClass` or in your DynaTest's `beforeGroup {}` which
makes VoK very easy to test. The easiest way is to call `Bootstrap().contextInitialized(null)` before all tests,
and `Bootstrap().contextDestroyed(null)` after all tests.

Since the VoK is fully initialized by the abovementioned calls (including the database access if you have the appropriate
module included), you can even access the database from your tests.
This allows you to test your server logic freely, without any need to mock the data access.
If you're not using H2 (which you are probably not in production), you can for example start a dockerized
PostgreSQL database before all tests, and kill the docker container afterwards. Then you only need to
provide a proper JDBC URL and that's it.

This is the true value of simplicity.

## Vaadin-on-Kotlin and I18n

The internationalization/localization/i18n/l10n of strings used by VoK (e.g. error messages,
filter component captions) can be tuned by the means of resource bundles. See
[I18n.kt](src/main/kotlin/eu/vaadinonkotlin/I18n.kt) for more details.

The following resource bundles are searched:

* The `VokMessages*.properties` bundle, located in the root package. Create one if you need to customize the localization
  strings in your app.
* If the message is not found, the standard message bundle of `com.github.vok.VokMessages*.properties` is consulted.

Consult the [standard message bundle](src/main/resources/eu/vaadinonkotlin/VokMessages.properties) for the list of messages.

Consult the [Translating Your App](http://www.vaadinonkotlin.eu/i18n.html) Guide for more details.
