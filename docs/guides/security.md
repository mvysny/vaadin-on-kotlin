---
layout: default
title: Security
permalink: /security/
parent: Guides
nav_order: 9
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

# VoK-Security Module

When securing your apps, you generally need to perform two duties:

* Only allow known users to access the app: *authentication*
* Only allow users with appropriate rights to access particular parts of the app: *authorization*.
  For example you'd only want administrators to manage users in the app.

Vaadin-on-Kotlin offers [Role-Based Access Control (RBAC)](https://en.wikipedia.org/wiki/Role-based_access_control). Every user in the app is assigned
a set of roles - a set of duties it is expected to perform in the app. Every Vaadin view
then declares roles allowed to see that particular view; only users which are assigned at least one
of the roles declared on the view are then allowed to visit that view.

VoK apps use the [Vaadin Simple Security](https://github.com/mvysny/vaadin-simple-security)
library. To use the library, add it to your `gradle/libs.versions.toml`:

```toml
[libraries]
vaadin-security-simple = "com.github.mvysny.vaadin-simple-security:vaadin-simple-security:1.1"
```

and depend on it from your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.vaadin.security.simple)
}
```

> Note: you don't have to use the Vaadin Simple Security library at all if it doesn't suit your need -
you are free to use your favourite framework or implement your own authorization from scratch.

A complete runnable demo lives at [vok-security-demo](https://github.com/mvysny/vok-security-demo);
the code samples below are taken from there. There is also a larger
[Bookstore Demo](https://github.com/mvysny/bookstore-vok).

## Example app at a glance

The demo uses username + password authentication; users are persisted in an H2 database via
[ktorm](https://www.ktorm.org/) + [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) and
seeded by `Bootstrap.kt`. There are two pre-created users:

* `user` / `user` with the role `ROLE_USER`
* `admin` / `admin` with the roles `ROLE_ADMIN`, `ROLE_USER`

…and four routes:

* `LoginRoute` — public, shown to anyone not yet logged in (`@AnonymousAllowed`).
* `WelcomeRoute` — shown to every logged-in user (`@PermitAll`).
* `UserRoute` — restricted to users with `ROLE_USER` or `ROLE_ADMIN` (`@RolesAllowed("ROLE_USER", "ROLE_ADMIN")`).
* `AdminRoute` — restricted to `ROLE_ADMIN` only.

## Annotating routes with roles

Use the standard Jakarta security annotations on each `@Route`:

```kotlin
import jakarta.annotation.security.RolesAllowed

@Route("admin", layout = MainLayout::class)
@RolesAllowed("ROLE_ADMIN")
class AdminRoute : KComposite() { /* … */ }

@Route("user", layout = MainLayout::class)
@RolesAllowed("ROLE_USER", "ROLE_ADMIN")
class UserRoute : KComposite() { /* … */ }
```

Three annotations are recognized; every route must carry exactly one of them, otherwise it is
inaccessible:

* `jakarta.annotation.security.RolesAllowed` — user must be logged in and must hold at least one
  of the listed roles.
* `jakarta.annotation.security.PermitAll` — any logged-in user.
* `com.vaadin.flow.server.auth.AnonymousAllowed` — anyone, even anonymous visitors. This is what
  you put on the `LoginRoute`.

> **Vaadin 25 note:** parent layouts (`RouterLayout` implementations such as `MainLayout`) must
> declare their own access rules. A child route's annotation no longer grants access through the
> layout chain, so `MainLayout` itself is annotated with `@PermitAll`.

### Per-instance authorization

Some rules cannot be expressed with annotations alone — e.g. "a user may view *their own* order,
and `sales` may view any order". Annotate the route with `@PermitAll` and check the rule inside a
`BeforeEnterObserver`:

```kotlin
import jakarta.annotation.security.PermitAll
import com.github.mvysny.vaadinsimplesecurity.AccessRejectedException

@Route("order", layout = MainLayout::class)
@PermitAll
class OrderRoute : KComposite(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = Session.loginService.currentUser!! // guaranteed by @PermitAll
        val order = Order.getById(event.parameterList[0].toLong())
        val authorized = "sales" in Session.loginService.currentUserRoles || order.userId == user.id
        if (!authorized) {
            throw AccessRejectedException("Access rejected to order ${order.id}",
                OrderRoute::class.java, setOf("sales"))
        }
        // … render the order …
    }
}
```

`AccessRejectedException` is caught by Vaadin's error handler and rendered as the standard
HTTP 403 page.

## Wiring the access checker

To enforce the route annotations, install a `SimpleNavigationAccessControl` as a Vaadin
`BeforeEnterListener`. This is done from a `VaadinServiceInitListener`:

```kotlin
class AppServiceInitListener : VaadinServiceInitListener {
    private val accessControl = SimpleNavigationAccessControl.usingService { Session.loginService }
    init {
        accessControl.setLoginView(LoginRoute::class.java)
    }

    override fun serviceInit(e: ServiceInitEvent) {
        e.source.addUIInitListener { uiInitEvent ->
            uiInitEvent.ui.addBeforeEnterListener(accessControl)
        }
    }
}
```

Register the listener by creating
`src/main/resources/META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener` containing
the fully-qualified class name (`com.example.AppServiceInitListener`).

`SimpleNavigationAccessControl.usingService { … }` obtains the current user/roles from your
`LoginService` (next section). When a not-yet-logged-in user navigates somewhere that requires
authentication, they're redirected to the configured login view.

> **Important:** the access checker will not redirect away from the login view. It is the
> application's responsibility to navigate to a welcome view after a successful login, otherwise
> the user will stay stuck on the login page.

## The LoginService

`AbstractLoginService<U>` from Vaadin Simple Security gives you a session-scoped object that
holds the currently logged-in user, exposes their roles, and handles logout. Subclass it and add a
`login(username, password)` method:

```kotlin
class LoginService : AbstractLoginService<User>() {
    fun login(username: String, password: String) {
        val user = User.findByUsername(username)
            ?: throw FailedLoginException("Invalid username or password")
        if (!user.passwordMatches(password)) {
            throw FailedLoginException("Invalid username or password")
        }
        login(user) // inherited; reinitializes the HTTP session and stores the user
    }

    override fun toUserWithRoles(user: User): SimpleUserWithRoles =
        SimpleUserWithRoles(user.username, user.roleSet)
}

/** Tie the LoginService to the Vaadin session — always get it through this property. */
val Session.loginService: LoginService
    get() = getOrPut(LoginService::class) { LoginService() }
```

The inherited `login(user)` method takes care of session-fixation prevention by reinitializing
the session, and stores the user so `currentUser` / `currentUserRoles` work for the rest of the
session. `logout()` is also inherited.

## The LoginRoute

```kotlin
@Route("login")
@PageTitle("Login")
@AnonymousAllowed
class LoginRoute : KComposite() {
    private lateinit var loginForm: LoginForm
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = false; content { center() }

            val loginI18n = loginI18n {
                form.title = "VoK Security Demo"
                additionalInformation = "Log in as user/user or admin/admin"
            }
            loginForm = loginForm(loginI18n)
        }
    }

    init {
        loginForm.addLoginListener { e ->
            try {
                Session.loginService.login(e.username, e.password)
            } catch (e: LoginException) {
                log.warn("Login failed", e)
                loginForm.setErrorMessage("Login failed", e.message)
            } catch (e: Exception) {
                log.error("Internal error", e)
                loginForm.setErrorMessage("Internal error", e.message)
            }
        }
    }

    companion object {
        @JvmStatic private val log = LoggerFactory.getLogger(LoginRoute::class.java)
    }
}
```

A logout button somewhere in the main layout simply calls `Session.loginService.logout()`.

## Storing users in the database

The demo's `User` entity uses ktorm for persistence and mixes in `HasPassword` to handle salted
password hashing automatically:

```kotlin
interface User : ActiveEntity<User>, HasPassword, Serializable {
    var id: Long?
    var username: String
    var passwordHash: String?
    var roles: String

    override fun getHashedPassword(): String? = passwordHash
    override fun setHashedPassword(hashedPassword: String?) { passwordHash = hashedPassword }

    override val table: Table<User> get() = Users

    val roleSet: Set<String> get() = roles.split(",").toSet()

    companion object : Entity.Factory<User>() {
        fun findByUsername(username: String): User? = db {
            database.sequenceOf(Users).filter { Users.username eq username }.firstOrNull()
        }
    }
}

object Users : Table<User>("users") {
    val id = long("id").primaryKey().bindTo { it.id }
    val username = varchar("username").bindTo { it.username }
    val passwordHash = varchar("hashedPassword").bindTo { it.passwordHash }
    val roles = varchar("roles").bindTo { it.roles }
}
```

The `passwordHash` Kotlin property is intentionally named differently from the inherited
`get/setHashedPassword` methods to avoid a JVM signature clash (a Kotlin `var hashedPassword`
would generate methods with the same JVM signature as the ones inherited from `HasPassword`).

Seed users via `setPassword("…")`, which delegates to the hashing implementation in `HasPassword`:

```kotlin
User {
    username = "admin"
    roles = "ROLE_ADMIN,ROLE_USER"
    setPassword("admin")
}.save()
```

## Roles vs. permissions

In bigger apps with lots of functionality and lots of views the number of roles tends to grow.
For example, you may need multiple administrator types which have access to a particular part of
the app but not to others — manager-admin, admin, super-admin, etc. The difference between roles
starts to blur and it becomes hard to keep them straight.

In that case we recommend thinking in *permissions* rather than *roles*. Instead of giving a user
the role "administrator", give them permissions like `can-view-users` or `can-create-order`. The
infrastructure stays exactly the same — `@RolesAllowed("can-view-users")` works just as well —
only the strings change meaning.

## Why there's no Authentication API in VoK

There are many security frameworks in Java, but in trying to support every authentication scheme
they end up highly abstract and hard to use. Authentication schemes vary wildly:

* Username + password against a local SQL database, or against LDAP/AD.
* Client-side x509 certificates.
* Kerberos / SPNEGO / Windows login via a servlet filter (Waffle).
* SAML, OAuth2.
* Smart cards, fingerprints, hardware tokens.
* Servlet container-provided auth (`ServletContext.login()`).

It's impossible to design one API that covers all of these without descending into abstraction
hell. That's why VoK doesn't ship an all-encompassing auth API in the style of Apache Shiro or
Spring Security. Instead it points you at small, focused building blocks (Vaadin Simple Security
for the common username/password case) and example apps you can copy-paste and adapt.

Always serve the app over HTTPS. Without TLS the app is exposed to man-in-the-middle attacks,
session hijacking, eavesdropping and HTML tampering — encrypting just the password in transit
solves none of that.
