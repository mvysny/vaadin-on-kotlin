[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# VoK-Security

When securing your apps, you generally need to perform two duties:

* Only allow known users to access the app: *authentication*
* Only allow users with appropriate rights to access particular parts of the app: *authorization*.
  For example you'd only want administrators to manage users in the app.

Vaadin-on-Kotlin uses *role-based* authorization. Every user in the app is assigned
a set of roles - a set of duties it is expected to perform in the app. Every Vaadin view
then declares roles allowed to see that particular view; only users which are assigned at least one
of the roles declared on the view are then allowed to visit that view.

## The 'Fictional Book Shop' Example

In a fictional book shop:

* Only the users which are assigned to the `administrator` role are allowed to create/delete other users
* Only users with the `bookkeeper` and `administrator` role may edit the book information;
* Only `sales` users may view/edit any orders
* Every user should be able to see its own orders.

In this example, we will have three roles, `administrator`, `bookkeeper` and `sales`. We will pre-create
the admin user which will then create other users and assign the roles. We will then annotate Vaadin views
so that `OrdersView` can only be viewed by the `sales` users:

Vaadin 8:

```kotlin
@AllowRoles("sales")
class OrdersView : VerticalLayout(), View { ... }

@AllowRoles("administrator")
class UsersView : VerticalLayout(), View { ... }

@AllowRoles("bookkeeper", "administrator")
class BookListView : VerticalLayout(), View { ... }
```

Vaadin 10:

```kotlin
@Route("orders", layout = MainLayout::class)
@AllowRoles("sales")
class OrdersView : VerticalLayout() { ... }

@Route("users", layout = MainLayout::class)
@AllowRoles("administrator")
class UsersView : VerticalLayout() { ... }

@Route("booklist", layout = MainLayout::class)
@AllowRoles("bookkeeper", "administrator")
class BookListView : VerticalLayout() { ... }
```

The remaining rule is somewhat special: we will have an `OrderView` which shows a complete details of a particular order
(specified in the URL as an order ID). Since the view will contain sensitive data (say a delivery address),
the view may not be viewed by anybody else than the user which created the order, and the members of the `sales`
group. We therefore need to check whether the current is user is `sales` user, or the
order in question belongs to the
currently logged-in user. We can't express this complex rule with annotations alone, hence we'll simply use Kotlin code to do that:

Vaadin 8:

```kotlin
@AllowAllUsers
class OrderView : VerticalLayout(), View {
  override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    val user: User = Session.loginManager.loggedInUser!!
    val order: Order = Order.getById(event.parameterList[0].toLong())
    val authorized: Boolean = user.hasRole("sales") || order.userId != user.id
    if (!authorized) {
      throw AccessRejectedException("Access rejected to order ${order.id}", OrderView::class.java, setOf())
    }
    // .. rest of the code, init the view, show the details about the order
  }
}
```

Vaadin 10:

```kotlin
@AllowAllUsers
class OrderView : VerticalLayout(), BeforeEnterObserver {
  override fun beforeEnter(event: BeforeEnterEvent) {
    val user: User = Session.loginManager.loggedInUser!!  // there is a user since that's mandated by @AllowAllUsers
    val order: Order = Order.getById(event.parameterList[0].toLong())
    val authorized: Boolean = user.hasRole("sales") || order.userId != user.id
    if (!authorized) {
      throw AccessRejectedException("Access rejected to order ${order.id}", OrderView::class.java, setOf())
    }
    // .. rest of the code, init the view, show the details about the order
  }
}
```

### Making The Annotations Work

In order to enforce the rules set by the annotations, you need to hook into the navigator: before a view is rendered, we will check whether
it can be navigated to.

First step is to register the `loggedInUserResolver`. It doesn't do anything on its own, but will serve current user
to a function which we will setup next:

```kotlin
VaadinOnKotlin.loggedInUserResolver = object : LoggedInUserResolver {
    override fun isLoggedIn(): Boolean = Session.loginManager.isLoggedIn
    override fun getCurrentUserRoles(): Set<String> = Session.loginManager.getCurrentUserRoles()
}
```

Now, to the hook itself. For Vaadin 8 we simply install the security hook in the `UI.init()`:

```kotlin
class MyUI : UI() {

    override fun init(request: VaadinRequest?) {
        if (!Session.loginManager.isLoggedIn) {
            // If no user is logged in, then simply show the LoginView (a full-screen login form) and bail out.
            // When the user logs in, we will simply reload the page, which recreates the UI instance; since the user is stored in a session
            // and therefore logged in, the code will skip this block and will initialize the UI properly.
            content = LoginView()
            return
        }

        navigator = Navigator(this, content as ViewDisplay)
        navigator.addProvider(autoViewProvider)
        VokSecurity.install()
        ...
    }
}
```

#### Vaadin 10

For Vaadin 10 the situation is a bit more complex. If you have UI, you can simply override `UI.init()` method and check the security there:
```kotlin
class MyUI: UI() {
    override fun init(request: VaadinRequest) {
        addBeforeEnterListener { enterEvent ->
            if (!Session.loginManager.isLoggedIn && enterEvent.navigationTarget != LoginScreen::class.java) {
                enterEvent.rerouteTo(LoginScreen::class.java)
            } else {
                VokSecurity.checkPermissionsOfView(enterEvent.navigationTarget)
            }
        }
    }
}
```

If you don't have the UI class but you have one root layout, you can make the root layout implement `BeforeEnterObserver`, and then override the `beforeEnter()`:
```kotlin
class MainLayout : AppHeaderLayout(), RouterLayout, BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!Session.loginManager.isLoggedIn) {
            event.rerouteTo(LoginView::class.java)
        } else {
            VokSecurity.checkPermissionsOfView(event.navigationTarget)
        }
    }
}
```

Otherwise you can provide your own init listener:

```kotlin
class BookstoreInitListener : VaadinServiceInitListener {
    override fun serviceInit(initEvent: ServiceInitEvent) {
        initEvent.source.addUIInitListener { uiInitEvent ->
            uiInitEvent.ui.addBeforeEnterListener { enterEvent ->
                if (!Session.loginManager.isLoggedIn && enterEvent.navigationTarget != LoginScreen::class.java) {
                    enterEvent.rerouteTo(LoginScreen::class.java)
                } else {
                    VokSecurity.checkPermissionsOfView(enterEvent.navigationTarget)
                }
            }
        }
    }
}
```

Don't forget to register it though: create a file in your `src/main/resources/META-INF/services` named `com.vaadin.flow.server.VaadinServiceInitListener`
containing the full class name of your `BookstoreInitListener` class.

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

> Note: sorry, currently there are no other tutorials beside the most simple username+password authentication,
with role-based authorization and users stored in a SQL database.

VoK also provide basic login forms and the documentation on how to integrate them
with your app. There is also a set of example projects:

* For Vaadin 8 there's [vok-security-demo](https://github.com/mvysny/vok-security-demo)
* For Vaadin 10 there's [vok-security-demo-v10](https://github.com/mvysny/vok-security-demo-v10) and [Bookstore Demo](https://github.com/mvysny/bookstore-vok).

## VoK Authorization

The VoK API authorization API uses role-based authorization on Vaadin views. There are
three annotations in the [AllowRoles.kt](src/main/kotlin/eu/vaadinonkotlin/security/AllowRoles.kt) file,
and your view must list exactly one of them otherwise it will be inaccessible:

* `AllowRoles` lists roles that are allowed to visit that view; the user must be logged in and must be assigned at least one of the roles listed in the annotation
* `AllowAll` allows anybody to see this view, even if there is no user logged in.
* `AllowAllUsers` allows any logged-in user to see this view.

These rules are quite simple and cover only the basic authorization needs. You can simply
define more complex rules as a Kotlin code in the `View.enter()` which is invoked on navigation
to that particular view. For Vaadin 10, simply check the rules in the `BeforeEnterObserver.beforeEnter()` function.

For example, the View may check e.g. whether given user has the right
to see particular record or a document. If not, [AccessRejectedException](src/main/kotlin/eu/vaadinonkotlin/security/AccessRejectedException.kt) must be simply thrown.
The exception is then caught by the Vaadin exception handler and either
an error notification "access rejected" should be shown (Vaadin 8), or
the user will be presented by the "access rejected" page with HTTP 403 (Vaadin 10).

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
3. A view listing users will then be annotated with `@AllowRoles('can-view-users')`

### The vok-security module

This module only provides basic API classes which lays out the foundation of the
security mechanism. The actual integration code differs for Vaadin 8 and Vaadin 10
and is therefore located in the [vok-util-vaadin8](../vok-util-vaadin8) and
[vok-util-vaadin10](../vok-util-vaadin10) modules:
* For Vaadin 8, please see the [VokSecurity](../vok-util-vaadin8/src/main/kotlin/eu/vaadinonkotlin/vaadin8/VokSecurity.kt) class;
* For Vaadin 10 please see the [VokSecurity](../vok-util-vaadin10/src/main/kotlin/eu/vaadinonkotlin/vaadin10/VokSecurity.kt) class;

## Example projects

Please find example projects below. Both projects are using the username+password authentication with users
stored in the SQL database:

* For Vaadin 8 there's [vok-security-demo](https://github.com/mvysny/vok-security-demo)
* For Vaadin 10 there's [vok-security-demo-v10](https://github.com/mvysny/vok-security-demo-v10) and [Bookstore Demo](https://github.com/mvysny/bookstore-vok).

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
