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

# VoK Utilities

This page documents additional VoK utilities available to all VoK projects.

## Support for Session

VoK provides a `Session` object that wraps the current `VaadinSession`:

* `Session.current` — the current `VaadinSession`.
* `Session["key"] = value` — store/retrieve values keyed by string.
* `Session[MyService::class] = MyService()` — store a session-scoped service keyed by its class.
* `Session.getOrPut(MyService::class) { MyService() }` — the idiomatic way to lazily attach a session-scoped service.

`Session` is also the recommended hook point for session-scoped services, so you don't need a DI container.
For example, a user-login module can attach a `LoggedInUser` service that holds the currently logged-in user and the login/logout logic:

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

    fun logout() {
        user = null
        Session.current.close()
    }
}
val Session.loggedInUser: LoggedInUser get() = getOrPut(LoggedInUser::class) { LoggedInUser() }
```

`Session.loggedInUser.login(...)` is now usable from anywhere — no DI needed.

> Note: the session is accessible only from code holding the Vaadin UI lock — typically Vaadin-driven request handling.
> Background threads must capture a `UI` reference and call `ui.access { … }` to interact with the session.

## Cookies

There is a `Cookies` singleton for cookie access on the current request/response:

* `Cookies += Cookie("autologin", "secret")` — add a cookie.
* `Cookies.delete("autologin")` — remove a cookie.
* `Cookies["autologin"]` — read a cookie from the current request (returns `null` if absent).
