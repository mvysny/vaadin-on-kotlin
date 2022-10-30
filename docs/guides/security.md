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

Repository: [vok-security](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-security)

When securing your apps, you generally need to perform two duties:

* Only allow known users to access the app: *authentication*
* Only allow users with appropriate rights to access particular parts of the app: *authorization*.
  For example you'd only want administrators to manage users in the app.

Vaadin-on-Kotlin offers *role-based* authorization built-in. Every user in the app is assigned
a set of roles - a set of duties it is expected to perform in the app. Every Vaadin view
then declares roles allowed to see that particular view; only users which are assigned at least one
of the roles declared on the view are then allowed to visit that view.

To use this module, simply add the following to your `build.gradle` file:

```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-security:x.y.z")
}
```

Note that you don't have to use the built-in authorization at all if it doesn't suit your need -
you are free to use your favourite framework or implement your own authorization from scratch.

## The 'Fictional Book Shop' Example

Let's create a fictional book shop, with the following set of access rules:

1. Only the users which are assigned to the `administrator` role are allowed to create/delete other users
2. Only users with the `bookkeeper` and `administrator` role may edit the book information;
3. Only `sales` users may view/edit any orders
4. Every user should be able to see its own orders.

We can use the role-based authorization scheme, to implement the rules.
We can solve the task by having three roles, `administrator`, `bookkeeper` and `sales`. We will pre-create
the admin user which will then create other users and assign the roles. We will then annotate Vaadin routes
so that `OrdersView` can only be viewed by the `sales` users:

```kotlin
import javax.annotation.security.RolesAllowed

@Route("orders", layout = MainLayout::class)
@RolesAllowed("sales")
class OrdersView : VerticalLayout() { ... }

@Route("users", layout = MainLayout::class)
@RolesAllowed("administrator")
class UsersView : VerticalLayout() { ... }

@Route("booklist", layout = MainLayout::class)
@RolesAllowed("bookkeeper", "administrator")
class BookListView : VerticalLayout() { ... }
```

The last rule is somewhat special: we will have an `OrderView` which shows a complete details of a particular order
(specified in the URL as an order ID). Since the view will contain sensitive data (say a delivery address),
the view may not be viewed by anybody else than the user which created the order, and the members of the `sales`
group. We therefore need to check whether the current is user is `sales` user, or the
order in question belongs to the
currently logged-in user. We can't express this complex rule with annotations alone, hence we'll simply use Kotlin code to do that:

```kotlin
import javax.annotation.security.PermitAll

@PermitAll // since the currently logged-in user may not be from the "sales" group but may still see his order.
class OrderView : VerticalLayout(), BeforeEnterObserver {
  override fun beforeEnter(event: BeforeEnterEvent) {
    val user: User = Session.loginManager.loggedInUser!!  // there is a user since that's mandated by @PermitAll
    val order: Order = Order.getById(event.parameterList[0].toLong())
    val authorized: Boolean = user.hasRole("sales") || order.userId == user.id
    if (!authorized) {
      throw AccessRejectedException("Access rejected to order ${order.id}", OrderView::class.java, setOf("sales"))
    }
    // .. rest of the code, init the view, show the details about the order
  }
}
```

### Making The Annotations Work

In order to enforce the rules set by the annotations, we need to hook into the navigation cycle.
Before a view is rendered, we will check whether it can be navigated to.

First step is to register the `loggedInUserResolver`. It doesn't do anything on its own, but will serve current user
to a function which we will setup next:

```kotlin
VaadinOnKotlin.loggedInUserResolver = object : LoggedInUserResolver {
    override fun getCurrentUser(): Principal? = Session.userService.currentUserPrincipal
    override fun getCurrentUserRoles(): Set<String> = Session.userService.currentUserRoles
}
```

(Note: We will create the `UserService` later on)

Now, to the hook itself. The best way is to provide your own init listener, which will handle all
layouts, even future ones that haven't been created yet:

```kotlin
class BookstoreInitListener : VaadinServiceInitListener {
    override fun serviceInit(initEvent: ServiceInitEvent) {
        initEvent.source.addUIInitListener { uiInitEvent ->
            val checker = VokViewAccessChecker()
            checker.setLoginView(LoginView::class.java)
            uiInitEvent.ui.addBeforeEnterListener(checker)
        }
    }
}
```

Don't forget to register the init listener: create a file in your `src/main/resources/META-INF/services` named `com.vaadin.flow.server.VaadinServiceInitListener`
containing the full class name of your `BookstoreInitListener` class.

The `VokViewAccessChecker` builds on Vaadin's built-in ViewAccessChecker which
checks whether current user has access to the route being navigated to.
The current user is obtained via `VaadinOnKotlin.loggedInUserResolver`.

**Important:** `VokAccessAnnotationChecker` will not navigate away from the `LoginView`. It is
the application's responsibility to navigate to appropriate welcome view after successful login,
otherwise the user will be endlessly presented `LoginView`.

### Login View

An example of a very simple `LoginView`:

```kotlin
/**
 * The login view which simply shows the login form full-screen. Allows the user to log in.
 * After the user has been logged in,
 * the page is refreshed which forces the MainLayout to reinitialize.
 * However, now that the user is present in the session,
 * the reroute to login view no longer happens and the MainLayout is displayed on screen properly.
 */
@Route("login")
class LoginView : KComposite() {
    private lateinit var loginForm: LoginForm
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = false; content { center() }

            val loginI18n: LoginI18n = loginI18n {
                header.title = "My App"
            }
            loginForm = loginForm(loginI18n)
        }
    }

    init {
        loginForm.addLoginListener { e ->
            try {
                Session.userService.login(e.username, e.password)
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
        @JvmStatic
        private val log = LoggerFactory.getLogger(LoginView::class.java)
    }
}
```

The `Session.userService` doesn't exist yet. We'll create it later on in this tutorial.

## VoK Authentication

Authentication identifies the user and tries to prove that it's indeed the user who's
accessing the app, and not an impersonator. Usually the user provides a secret that only he
knows, say, a password. The authentication process then verifies the password, and if the
verification is successful, stores the user along with his roles into the session so that it is
handily available throughout the app.

There are many different ways to authenticate
that it is impossible to craft a unified API
which would be simple and understandable at the same time. That's why VoK doesn't
provide its own authentication API (since it would either be incomplete or complex).

VoK provides direct support for the most common case: the username+password authentication schema.
For other types of authentication schemata, VoK relies on a set of guides for
particular authentication scheme.
We rely on the programmer to read the tutorials, the documentation and copy-paste
from code examples to create authentication mechanism that's best tailored for his project.

> Note: We apologize, but currently there are no other tutorials beside the most simple username+password authentication,
with role-based authorization and users stored in a SQL database.

VoK also provide basic login forms and the documentation on how to integrate them
with your app. There is also a set of example projects:

* [Security Demo](https://github.com/mvysny/vok-security-demo)
* [Bookstore Demo](https://github.com/mvysny/bookstore-vok).

### UserService example

An example of a very simple user service which performs logins and logouts, and stores
itself into the session. That way, the user itself is also stored in the session.

```kotlin
val Session.userService: UserService get() = getOrPut { UserService() }

// guarded-by: Vaadin Session
class UserService : Serializable {
    var loggedInUser: String? = null
    private set

    val loggedInUserPrincipal: Principal?
        get() = loggedInUser?.let { BasicUserPrincipal(it) }

    var currentUserRoles: Set<String> = setOf()
    private set

    /**
     * Logs out the user, clears the session and reloads the page.
     */
    fun logout() {
        Session.current.close()
        // The UI is recreated by the page reload, and since there is no user in the session (since it has been cleared),
        // the UI will show the LoginView.
        UI.getCurrent().page.reload()
    }

    /**
     * Logs in user with given [username] and [password]. Fails with [javax.security.auth.login.LoginException]
     * on failure.
     */
    fun login(username: String, password: String) {
        // the users should come from the database; here we'll just fake some users
        val knownUser = when(username) {
            "user" -> password == "user"
            "admin" -> password == "admin"
            else -> false
        }
        if (!knownUser) {
            throw FailedLoginException("Invalid username or password")
        }
        login(username)
    }

    /**
     * Logs in given [user].
     */
    private fun login(user: String) {
        this.loggedInUser = user
        // the roles should come from the database; here we'll just fake the roles
        currentUserRoles = when (user) {
            "user" -> setOf("user")
            "admin" -> setOf("user", "admin")
            else -> setOf()
        }

        // creates a new session after login, to prevent session fixation attack
        VaadinService.reinitializeSession(VaadinRequest.getCurrent())
        // reload the page. Since the user is now logged in, VokViewAccessChecker
        // will now let the navigation to MainView succeed.
        navigateTo<MainView>()
    }

    val isLoggedIn: Boolean get() = loggedInUser != null
  
    fun ensureLoggedIn(): String = checkNotNull(loggedInUser) { "Not logged in" }

    val isAdmin: Boolean get() = isLoggedIn && currentUserRoles.contains("admin")

    fun checkAdmin() {
        ensureLoggedIn()
        if (!currentUserRoles.contains("admin")) {
//            throw AccessRejectedException("Not admin", null, setOf("admin"))
            throw IllegalStateException("Not admin")
        }
    }
}
```

## VoK Authorization

The VoK API authorization API uses role-based authorization on Vaadin views. There are
three annotations available,
and your view must list at least one of them otherwise it will be inaccessible:

* `javax.annotation.security.RolesAllowed` lists roles that are allowed to visit that view;
   the user must be logged in and must be assigned at least one of the roles listed in the annotation
* `com.vaadin.flow.server.auth.AnonymousAllowed` allows anybody to see this view, even if there is no user logged in.
* `javax.annotation.security.PermitAll` allows any logged-in user to see this view.

These rules are quite simple and cover only the basic authorization needs. You can simply
define more complex rules as a Kotlin code in the `BeforeEnterObserver.beforeEnter()` function
of a particular route.

For example, the View may check e.g. whether given user has the right
to see particular record or a document. If not, [AccessRejectedException](src/main/kotlin/eu/vaadinonkotlin/security/AccessRejectedException.kt) must be simply thrown.
The exception is then caught by the Vaadin exception handler and
the user will be presented by the "access rejected" page with HTTP 403.

The API is intended to be very simple so that it can be backed easily by any kind
of auth scheme you need: VoK-Security built-in Simple scheme, the [OACC](http://oaccframework.org/oacc-features.html)
library, [Apache Shiro](https://shiro.apache.org/) or others.

### Replace 'roles' with 'permissions'

In bigger apps with lots of functionality and lots of views the number of roles may grow big. For example,
you may need multiple administrator types which have access to a particular part of an app but not to the other -
say, manager-admin, admin, super-admin etc etc. In such app, the difference between roles starts
to blur and it becomes hard to differentiate between roles.

In such case we recommend to start thinking of 'permissions' rather than 'roles'. For example, instead of a
user having an administrator role, the user would have a permission to view and edit the list of all users.

To convert your app to this new security paradigm:

1. Introduce a permission for every action, say, `can-view-users`, `can-create-order`.
2. Instead of assigning users roles, you assign them permissions.
3. A view listing users will then be annotated with `@RolesAllowed("can-view-users")`

### The vok-security module

This module only provides basic API classes which lays out the foundation of the
security mechanism. The actual integration code is located in the [vok-util](/utilities) module:

* [VokViewAccessChecker](../vok-util-vaadin/src/main/kotlin/eu/vaadinonkotlin/vaadin/VokViewAccessChecker.kt) class

## VoK-Security Simple

VoK-Security provides a simple password-based auth with all of the best practices:

* A login dialog is provided, to ask the user for the user name/password
* The username/password is checked against a SQL database. The passwords are stored
  hashed and salted to the database.
* The SQL database also stores the user roles, simply as a comma-separated list of strings.
* Upon successful login, the user object is stored into the session.
* Every view is then annotated with `@HasRoles`; a special `Navigator` hook
  then checks that a navigation is allowed.
* If the navigation is rejected, the `AccessRejectedException` is thrown.
* A customized Vaadin Error Handler must be set, which properly handles such exceptions.

It is highly recommended to serve the app over https. While it could be theoretically
possible to transmit the password encrypted to the server over plain http,
the app would still be susceptible to man-in-the-middle attack, session hijacking,
eavesdropping and html tampering.

If https is used, then there is no need to transmit the password encrypted since
all communication from the client to the server is automatically encrypted.

## Why there is no Authentication API in VoK

There are many security frameworks already present in Java. However, while attempting
to support all authentication/authorization schemes those frameworks have became highly
abstract and hard to understand. And rightly so: the authentication schemes are wildly
variant:

* Authentication using username + password:
  * Against a local SQL database of users
  * Against a LDAP/AD server
* Client-side x509 certificates
* Kerberos-provided security token (client-to-server tickets):
  * Authentication via NTLM/SPNEGO/Windows login via a NTLM servlet filter (the Waffle library)
* SAML-based solutions which are anything but simple
* Oauth2
* Other means: smart cards, fingerprints, ...
* All that while supporting SSO, or using the Servlet container-provided authentication mechanism
  (the `ServletContext.login()` method).

It is impossible to create an API convering all those cases without going abstraction-crazy.
That's why we deliberately avoid to use an all-encompassing library like [Apache Shiro](https://shiro.apache.org/)
or [Spring Security](https://projects.spring.io/spring-security/)
with insanely complex APIs. We also don't provide our own authentication API (since it would
either be incomplete or complex). In this case, the best abstraction is no abstraction at all.

However, if need be, we may add support for most used combinations (e.g. username+password via LDAP).
A standalone library will then be created.
