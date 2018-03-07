# VoK additional utilities for Vaadin 8

Provides utilities for creating UIs with Vaadin 8, but does not
introduce support for any particular database type. Depends on Karibu-DSL and VoK Core and
provides additional Vaadin 8 Kotlin wrappers.

Just add the following to your Gradle script, to depend on this module:
```groovy
dependencies {
    compile('com.github.vok:vok-util-vaadin8:0.4.1')
}
```

## When to use this module

Use this module when you want to use Vaadin 8 with Grids. Since VoK includes built-in support for SQL
databases you may also include additional modules.

When you want to also use the SQL database plus VoK-ORM:

* Depend on the [vok-framework-sql2o](../vok-framework-sql2o) module instead - it will include this
  module, the [vok-db](../vok-db) module which sports VoK-ORM,
  and will implement proper filters which work with SQL databases.

When you want to also use the SQL database plus JPA:

* Depend on the [vok-framework-jpa](../vok-framework-jpa) module instead - it will include this
  module, JPA+Hibernate and will implement proper filters which work with the JPA Criterion API.
  
When you want to also use the SQL database plus something else:

* Depend on this module and implement the proper `FilterFactory` implementation yourself, in order
  to have auto-generated Grid filter components.

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
  * Date - uses the `DateFilterPopup`
  * Strings - uses in-place `TextField` which performs substring matches

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
// must have a zero-arg constructor so that the lazySession() function can instantiate this class lazily
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
val Session.loggedInUser: LoggedInUser by lazySession()
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
