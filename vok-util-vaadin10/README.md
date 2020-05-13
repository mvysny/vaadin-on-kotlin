[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-util-vaadin10/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-util-vaadin10)

# VoK additional utilities for Vaadin 14

Provides utilities for creating UIs with Vaadin 14, but does not introduce
support for any particular database type. Includes the
[Karibu-DSL](https://github.com/mvysny/karibu-dsl) library, depends on the
[vok-framework core](../vok-framework) and provides additional Vaadin 14 Kotlin wrappers.

Just add the following to your Gradle script, to depend on this module:
```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-util-vaadin10:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

## When to use this module

Use this module when you want to use Vaadin 14+ and you need to have additional
support for Session and Vaadin Grid (namely for filter components).
Since VoK includes built-in support for SQL databases you may also want to
include additional modules - read below.

When you want to also use the SQL database with the
recommended approach ([vok-db](../vok-db)):

* Depend on the [vok-framework-v10-vokdb](../vok-framework-v10-vokdb) module
instead - it will include this module, the [vok-db](../vok-db) module which
sports VoK-ORM, and will implement proper filters which work with SQL databases.

VoK currently does not support usage of Vaadin 14 with JPA.
  
When you want to also use the SQL database plus some other ORM library, or the JDBC directly:

* Depend on this module and implement the proper `FilterFactory` implementation
  yourself, in order to have support for Grid filter components.
  Read below on how to do that.

When you want to use the NoSQL database:

* Depend on this module and implement the proper `FilterFactory` implementation
  yourself, in order to have support for Grid filter components.

## Support for Grid Filter Bar

Hooking a Grid to a data provider is easy:

```kotlin
grid.dataProvider = Person.dataProvider  // uses vok-orm's DataLoader
```

To create a filtering field, monitor its value and update the DataProvider filter accordingly,
you need to do something along these lines:

```kotlin
personGrid = grid(dataProvider = Person.dataProvider) {
  flexGrow = 1.0
  appendHeaderRow() // workaround for https://github.com/vaadin/vaadin-grid-flow/issues/973
  val filterBar: VokFilterBar<Person> = appendHeaderRow().asFilterBar(this)

  addColumnFor(Person::name) {
    filterBar.forField(TextField(), this).ilike()
  }
}
```

This module provides a default set of filter components intended to be used with
Vaadin Grid, to perform filtering of the data shown in the Grid:

* A `NumberFilterPopup` which allows the user to specify a numeric range of accepted values, which may be
  potentially open.
* A `DateRangePopup` which allows the user to specify a date range of accepted values.
* A `BooleanComboBox` which is betterly suited for filtering than a `Checkbox`
  since it has three states: `true`: filters beans having the `true`
  value in given property; `false`: filters beans having the `false`
  value in given property; `null` which disables the filter.
* An `enumComboBox()` function which allows the user to filter for a particular
  enum constant.

In addition, you can use any Vaadin field component:

* A `TextField` for ILIKE or full-text filtering.
* A `ComboBox` with pre-populated values, to mimic `enumComboBox()` when
  there's a limited set of values present in the column.
* Possibly others.

`FilterBar.configure()` configures all filter fields by default as follows:

* the width is set to 100%
* the clear button is made visible for `TextField` and `ComboBox`.
* `HasValueChangeMode.setValueChangeMode` is set to `ValueChangeMode.LAZY`: not to bombard the database with EAGER, but
  also not to wait until the focus is lost from the filter - not a good UX since the user types in something and waits and waits and waits with nothing going on.

You can override the `configure()` function to modify this behaviour.

Note that the filter components need an implementation of the `FilterFactory` to
properly generate filter objects for a particular database backend.
By default the [DataLoaderFilterFactory] is used: it produces `vok-dataloader`-compatible
`Filter` instances which are accepted by [vok-framework-v10-vokdb](../vok-framework-v10-vokdb)
module. This detail is hidden within the `asFilterBar()` function.

There is no support for JPA.

### How this works: Wiring Filters To Custom Database Backend

TODO revisit

In order for the Grid to offer filtering components to the user, the programmer
needs to create the filter components first and attach them to the Grid. Typically
a special header row is created, to accommodate the filter components.

The filter component creation flow is as follows:

* The programmer (you) creates the Grid header row, by calling `Grid.appendHeaderRow()`.
* The programmer creates the filter components by hand and attaches them to the
header row cells; the programmer then needs to intercept the value changes,
recreate the filters and pass them to the data provider.

This manual process is a lot of work. Luckily, this process can be vastly
automatized by a proper set of utility classes provided by VoK. 

* The programmer uses the `FilterRow` class to create filter components and
  'bind' them to the `FilterRow`.
* The `FilterRow` then uses `FilterBinder` to handle value changes and will set
  the filters properly into the data provider.
* In order for the `FilterRow` to do that, it needs an implementation of
  `FilterFactory` for your particular database backend.

The filter component call flow is then as follows:

* The user changes the value in the filter component; say, a `String`-based
  TextField which does substring filtering.
* The `FilterBinder` intercepts the change and polls all filter components for
  the current values. In this example there will be just a single value of type `String`.
* Since these values can't be passed directly into the `DataProvider`, the values
  are passed to the
  `FilterFactory` implementation instead, which then provides proper filter objects
  accepted by the `DataProvider`.
* Those filter objects are then passed into the `VokDataProvider.setFilter()`
  method which will then notify the Grid that the data set may have been changed.
* The Grid component will then refresh the data by calling `DataProvider.fetch()`;
  the `VokDataProvider` implementation will make sure to include the filters
  configured in the above step.

With this automatized approach, all you need to provide is:

* A `DataProvider` which is able to fetch data from your backend
* You can either make your `DataProvider` use vok-dataloader filters, or you will need to define a set of filter objects (say, `LessThanFilter(25)` and others) which your data provider will then accept and will be able to filter upon.
* A `FilterFactory` implementation which will then produce your custom filter objects. For vok-dataloader filters there is such implementation: the `DataLoaderFilterFactory` class.

With this machinery in place, we can now ask the `FilterFieldFactory` to create
fields for particular column, or even for all Grid columns. That's precisely what
the `generateFilterComponents()` function does. It's a good practice create this
function as an extension function on the `HeaderRow` class so that the programmer
can generate filter components, simply by calling

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid)
```

For more details please see the `VaadinFilters.kt` file in the `vok-framework-v10-vokdb`module.

For more information about using filters with `DataProviders` please see
the [Databases Guide](http://www.vaadinonkotlin.eu/databases-v10.html).

### DataLoaders

Even easier way is to use [vok-dataloader](https://gitlab.com/mvysny/vok-dataloader)
which provides a rich hierarchy of filters out-of-the-box, and the `DataLoader`
interface is way simpler to implement than `DataProvider`. The `FilterBar`
class already reuses `vok-dataloader` filter hierarchy, so all we need is to
convert a data loader to a data provider. Luckily, that's very easy:

```kotlin
val dataLoader: DataLoader<Person> = Person.dataLoader // to load stuff from a SQL database via vok-orm
val dataLoader: DataLoader<Person> = CrudClient("http://localhost:8080/rest/person/", Person::class.java) // to load stuff from a REST endpoint via vok-rest-client
    // or you can implement your own DataLoader
    
val dataProvider: VokDataProvider<Person> = dataLoader.asDataProvider {it.id!!}
grid.dataProvider = dataProvider
val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(this)
grid.addColumnFor(Person::name) {
  filterBar.forField(TextField(), this).ilike()
}
```

Or even shorter, by using VoK-provided extension methods to set the data loader
to a Grid directly:
```kotlin
val dataLoader = // as above
grid.setDataLoader(dataLoader) { it.id!! }
val filterBar: VokFilterBar<Person> = grid.appendHeaderRow().asFilterBar(this)
grid.addColumnFor(Person::name) {
  filterBar.forField(TextField(), this).ilike()
}
```

See [vok-rest-client](../vok-rest-client) for how to use the REST client DataLoader.

## Support for Session

Provides a `Session` object which gives handy access to the `VaadinSession`:

* `Session.current` returns the current `VaadinSession`.
* `Session["key"] = value` allows you to retrieve and/or store values into the session
* `Session[MySessionScopedService::class] = MySessionScopedService()` allows you
  to store session-scoped services into the session. However, read below on how to
  do this properly.

Another important role of the `Session` object is that it provides a default point
to which you can attach your session-scoped services. For example, the user login
module of your app can attach the `LoggedInUser` service which contains both the
currently logged in user, and the means to log in and log out:

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

By using the above code, you will now be able to access the `LoggedInUser` from
anywhere, simply by calling `Session.loggedInUser.login()`. No DI necessary!

> Note: the session is accessible only from the code being run by Vaadin, with
>Vaadin UI lock properly held. It will not be accessible for example from
>background threads - you will need to store the UI reference and call
>`ui.access()` from your background thread.

## Cookies

There is a `Cookies` singleton which provides access to cookies attached to the current request:

* Use `Cookies += Cookie("autologin", "secret")` to add a cookie;
* Use `Cookies.delete("autologin")` to remove a cookie.
* Use `Cookies["autologin"]` to access a cookie for the current request.
