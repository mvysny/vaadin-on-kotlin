[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework)

# VoK Framework Core

The core module of Vaadin-on-Kotlin. Always included in your project, usually transitively via one of the other VoK
modules.

This module is intentionally minimal: it provides the VoK runtime bootstrap, an async executor, an i18n bundle, a
`Session` helper, and a `Cookies` accessor. There is no DB dependency here — that lives in
[vok-framework-vokdb](../vok-framework-vokdb), which wires in ktorm + ktorm-vaadin.

## Bootstrap

Typically called from a `ServletContextListener` (or whatever boot hook your container provides):

```kotlin
@WebListener
class Bootstrap : ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        VaadinOnKotlin.init()
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        VaadinOnKotlin.destroy()
    }
}
```

Other VoK modules tend to add fields to the `VaadinOnKotlin` object. For example `vok-framework-vokdb` adds
`VaadinOnKotlin.dataSource` — assigning a `HikariDataSource` to it also wires `ActiveKtorm.database` so ktorm
queries work immediately.

## Initializing VoK from tests

You can call `Bootstrap().contextInitialized(null)` from `@BeforeAll` and `Bootstrap().contextDestroyed(null)` from
`@AfterAll`. Since this fully initializes the VoK runtime (including the database access if `vok-framework-vokdb`
is on the classpath), tests can hit the real DB without any mocking. For non-H2 backends, spin up a Dockerized
PostgreSQL/MySQL/etc. before the test suite and tear it down after — the test code itself doesn't change.

## I18n

The internationalization of strings used by VoK (e.g. error messages) is driven by `ResourceBundle`s. See
[I18n.kt](src/main/kotlin/eu/vaadinonkotlin/I18n.kt) for details.

The following bundles are searched:

* `VokMessages*.properties` in the root package — create one to override VoK's defaults for your app.
* The default `eu.vaadinonkotlin.VokMessages*.properties` if nothing app-specific matches.

See the [Translating Your App](https://www.vaadinonkotlin.eu/i18n.html) guide for more.

## Filter UI for Grids

This module no longer ships a `FilterBar` DSL. Filter components live in `ktorm-vaadin` and are pulled in through
`vok-framework-vokdb`:

```kotlin
import com.github.mvysny.ktormvaadin.filter.FilterTextField
import com.github.mvysny.ktormvaadin.filter.DateRangePopup
import com.github.mvysny.ktormvaadin.filter.NumberRangePopup
import com.github.mvysny.ktormvaadin.filter.BooleanFilterField
```

You wire them into a Grid header row manually and combine their values into a ktorm
`ColumnDeclaring<Boolean>` filter that you hand to the data provider via `setFilter(...)`. The
[`PersonListView`](../vok-example-crud/src/main/kotlin/example/crudflow/person/PersonListView.kt) in the demo and
the [`EmployeesRoute`](https://github.com/mvysny/ktorm-vaadin/blob/master/testapp/src/main/kotlin/testapp/EmployeesRoute.kt)
in ktorm-vaadin's test app are the canonical end-to-end examples.

## Support for Session

Provides a `Session` object that wraps `VaadinSession`:

* `Session.current` — the current `VaadinSession`.
* `Session["key"] = value` — store/retrieve values keyed by string.
* `Session[MyService::class] = MyService()` — store a session-scoped service.
* `Session.getOrPut { … }` — the idiomatic way to lazily attach a session-scoped service:

```kotlin
class LoggedInUser : Serializable {
    var user: User? = null
        private set
    val isLoggedIn: Boolean get() = user != null

    fun login(username: String, password: String) {
        val u = User.findByUsername(username) ?: throw LoginException("No such user $username")
        if (!u.validatePassword(password)) throw LoginException("$username: invalid password")
        user = u
    }

    fun logout() { user = null; Session.current.close() }
}
val Session.loggedInUser: LoggedInUser get() = getOrPut { LoggedInUser() }
```

`Session.loggedInUser.login(...)` is now usable from anywhere — no DI needed.

> Note: the session is only accessible from code holding the Vaadin UI lock. Background threads must call `ui.access {}`.

## Cookies

There is a `Cookies` singleton for cookie access on the current request:

* `Cookies += Cookie("autologin", "secret")` — add a cookie
* `Cookies.delete("autologin")` — remove a cookie
* `Cookies["autologin"]` — read a cookie
