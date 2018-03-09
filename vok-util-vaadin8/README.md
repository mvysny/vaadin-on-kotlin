# VoK additional utilities for Vaadin 8

Provides utilities for creating UIs with Vaadin 8, but does not
introduce support for any particular database type.  Includes the [Karibu-DSL](https://github.com/mvysny/karibu-dsl)
library, depends on the [vok-framework core](../vok-framework) and
provides additional Vaadin 8 Kotlin wrappers.

Just add the following to your Gradle script, to depend on this module:
```groovy
dependencies {
    compile('com.github.vok:vok-util-vaadin8:x.y.z')
}
```

> Note: obtain the newest version from the release name, and the tag name as well: [https://github.com/mvysny/vaadin-on-kotlin/releases](https://github.com/mvysny/vaadin-on-kotlin/releases)

## When to use this module

Use this module when you want to use Vaadin 8 and you need to have additional support for Vaadin Grid
(namely, auto-generated filtering components). Since VoK includes built-in support for SQL
databases you may also want to include additional modules - read below.

When you want to also use the SQL database with the recommended approach ([vok-db](../vok-db)):

* Depend on the [vok-framework-sql2o](../vok-framework-sql2o) module instead - it will include this
  module, the [vok-db](../vok-db) module which sports VoK-ORM,
  and will implement proper filters which work with SQL databases.

When you want to also use the SQL database plus JPA:

* Depend on the [vok-framework-jpa](../vok-framework-jpa) module instead - it will include this
  module, JPA+Hibernate and will implement proper filters which work with the JPA Criterion API.
  
When you want to also use the SQL database plus some other ORM library, or the JDBC directly:

* Depend on this module and implement the proper `FilterFactory` implementation yourself, in order
  to have auto-generated Grid filter components. Read below on how to do that.

When you want to use the NoSQL database:

* Depend on this module and implement the proper `FilterFactory` implementation yourself, in order
  to have auto-generated Grid filter components.

## Support for Grid filters

This module provides a default set of filter components intended to be used with Vaadin Grid, to
perform filtering of the data shown in the Grid:

* A `NumberFilterPopup` which allows the user to specify a numeric range of accepted values, which may be
  potentially open.
* A `DateFilterPopup` which allows the user to specify a date range of accepted values.
* A `FilterFieldFactory` which allows for automatic filter creation for beans, based on reflection and
  bean property type;
* The `DefaultFilterFieldFactory` implementation which is able to provide filter components for the following types:
  * Numbers such as Int, Short, Long, Byte - shows a `NumberFilterPopup`
  * Boolean - shows a `ComboBox` with `true`, `false` and `null` (to disable the filter)
  * Enum - shows a `ComboBox` with all possible Enum values.
  * Date, LocalDate, LocalDateTime, Instant - uses the `DateFilterPopup`
  * Strings - uses in-place `TextField` which performs substring matches

Note that the filter components need an implementation of the `FilterFactory` to properly
generate filter objects for a particular database backend. The [vok-framework-sql2o](../vok-framework-sql2o) module
provides such implementation for filtering with the VoK-ORM framework (recommended);
The [vok-framework-jpa](../vok-framework-jpa) module
provides such implementation for filtering with the JPA framework.

### Wiring filters to custom database backend

In order for the Grid to offer filtering components to the user, the programmer needs to create the
filter components first and attach them to the Grid. Typically a special header row is created, to accommodate
the filter components.

The filter component creation flow is as follows:

* The programmer (you) creates the Grid header row, by calling `Grid.appendHeaderRow()`.
* The programmer creates the filter components by hand and attaches them to the header row cells;
  the programmer then needs to intercept the value changes, recreate the filters and pass them to
  the data provider.

This manual process is a lot of work. Luckily, this process can be vastly automatized by a proper set of
utility classes provided by VoK. 

* The programmer uses the `DefaultFilterFieldFactory` to create the filter components based on the type
  of the value shown by the particular column (say, it will create a `TextField` when the column type is `String`).
  The
  `DefaultFilterFieldFactory` will then handle value changes and will set the filters properly into the
  data provider.
* In order for the `DefaultFilterFieldFactory` to do that, it needs an implementation of `FilterFactory`
  for your particular database backend.

The filter component call flow is then as follows:

* The user changes the value in the filter component; say, a `String`-based TextField which does substring filtering.
* The `FilterFieldFactory` intercepts the change and polls all filter components for the current values. In this example
  there will be just a single value of type `String`.
* Since these values can't be passed directly into the `DataProvider`, the values are passed to the
  `FilterFactory` implementation instead, which then provides proper filter objects accepted by the `DataProvider`.
* Those filter objects are then passed into the `ConfigurableFilterDataProvider.setFilter()` method which
  will then notify the Grid that the data set may have been changed.
* The Grid component will then refresh the data by calling `DataProvider.fetch()`; the `ConfigurableFilterDataProvider`
  implementation will make sure to include the filters configured in the above step.

With this automatized approach, all you need to provide is:

* A `DataProvider` which is able to fetch data from your backend
* You will need to define a set of filter objects (say, `LessThanFilter(25)` and others) which your
  data provider will then accept and will be able to filter upon.
* A `FilterFactory` implementation which will then produce your custom filter objects.

With this machinery in place, we can now ask the `FilterFieldFactory` to create fields for particular column,
or even for all Grid columns. That's precisely what the `generateFilterComponents()` function does.
It's a good practice create this function as an extension function on the `HeaderRow` class so that the
programmer can generate filter components simply by calling

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid)
```

For more details please see the `VaadinFilters.kt` file in the `vok-framework-sql2o` module.

## Support for Session

Provides a `Session` object which gives handy access to the `VaadinSession`:

* `Session.current` returns the current `VaadinSession`.
* `Session["key"] = value` allows you to retrieve and/or store values into the session
* `Session[MySessionScopedService::class] = MySessionScopedService()` allows you to store session-scoped services into the session.
  However, read below on how to do this properly.

Another important role of the `Session` object is that it provides a default point to which you can
attach your session-scoped services. For example, the user
login module of your app can attach the `LoggedInUser` service which contains both the currently logged in user,
and the means to log in and log out:

```kotlin
class LoggedInUser : Serializable {
  val user: User? = null
    private set
    
  val isLoggedIn: Boolean
    get() = user != null

  fun login(username: String, password: String) {
    val user = User.findByUsername(username) ?: throw LoginException("No such user $username")
    if (!user.validatePassword(password)) throw LoginException("$username: invalid password")
    this.user = user
  }
  
  fun logout() {
    user = null
    // http://stackoverflow.com/questions/26404821/how-to-restart-vaadin-session
    Page.getCurrent().setLocation(VaadinServlet.getCurrent().servletConfig.servletContext.contextPath)
    Session.current.close()
  }
}
val Session.loggedInUser: LoggedInUser get() = getOrPut { LoggedInUser() }
```

By using the above code, you will now be able to access the `LoggedInUser` from anywhere, simply by calling
`Session.loggedInUser.login()`. No DI necessary!

> Note: the session is accessible only from the code being run by
Vaadin, with Vaadin UI lock properly held. It will not be accessible for example from background threads -
you will need to store the UI reference and call `ui.access()` from your background thread.

## Cookies

There is a `Cookies` singleton which provides access to cookies attached to the current request:

* Use `Cookies += Cookie("autologin", "secret")` to add a cookie;
* Use `Cookies.delete("autologin")` to remove a cookie.
* Use `Cookies["autologin"]` to access a cookie for the current request.
