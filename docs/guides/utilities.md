---
layout: default
title: Utilities
permalink: /utilities/
parent: Guides
nav_order: 10
---

<br/>
<details close markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>
<br/>

# VoK-Util Module

Repository: [vok-util-vaadin10](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin10)

Provides utilities for creating UIs with Vaadin, but does not introduce
support for any particular database type. Includes the
[Karibu-DSL](https://github.com/mvysny/karibu-dsl) library, depends on the
[vok-framework core](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-framework) and provides additional Vaadin Kotlin wrappers.

Just add the following to your Gradle script, to depend on this module:
```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-util-vaadin:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

## When to use this module

Use this module when you want to use Vaadin and you need to have additional
support for Session and Vaadin Grid (namely for filter components).
Since VoK includes built-in support for SQL databases you may also want to
include additional modules - read below.

When you want to also use the SQL database with the
recommended approach ([vok-db](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-framework-vokdb)):

* Depend on the [vok-framework-v10-vokdb](../vok-framework-v10-vokdb) module
instead - it will include this module, the [vok-db](../vok-db) module which
sports VoK-ORM, and will implement proper filters which work with SQL databases.

VoK currently does not support usage of Vaadin with JPA.
  
When you want to also use the SQL database plus some other ORM library, or the JDBC directly:

* Depend on this module and implement the proper `FilterFactory` implementation
  yourself, in order to have support for Grid filter components.
  Read below on how to do that.

When you want to use the NoSQL database:

* Depend on this module and implement the proper `FilterFactory` implementation
  yourself, in order to have support for Grid filter components.

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