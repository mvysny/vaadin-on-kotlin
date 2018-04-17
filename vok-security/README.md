# VoK-Security

When securing your apps, you generally need to perform two duties:

* Only allow known users to access the app: *authentication*
* Only allow users with appropriate rights to access particular parts of the app: *authorization*.
  For example you'd only want administrators to manage users in the app.

Vaadin-on-Kotlin uses *role-based* authorization. Every user in the app is assigned
a set of roles - a set of duties it is expected to perform in the app. Every Vaadin view
then declares roles allowed to see that particular view; only users which are assigned at least one
of the roles declared on the view are then allowed to visit that view.

## Example: Fictional Book Shop

In a fictional book shop:

* Only the users which are assigned to the `administrator` role are allowed to create/delete other users
* Only users with the `bookkeeper` and `administrator` role may edit the book information;
* Only `sales` users may view/edit any orders
* Every user should be able to see its own orders.

In this example, we will have three roles, `administrator`, `bookkeeper` and `sales`. We will pre-create
the admin user which will then create other users and assign the roles. We will then annotate Vaadin views
so that `OrdersView` can only be viewed by the `sales` users:
```
@AllowRoles("sales")
class OrdersView : VerticalLayout(), View {}

@AllowRoles("administrator")
class UsersView : VerticalLayout(), View {}

@AllowRoles("bookkeeper", "administrator")
class BookListView : VerticalLayout(), View {}
```

The last rule is somewhat special: the view need to check whether the order in question belongs to the
currently logged-in user. We can't express this with annotations, so we'll simply use Kotlin code to do that:
```
@AllowAllUsers
class OrderView : VerticalLayout(), View {
  override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    val user: User = Session.loginManager.loggedInUser!!
    val order: Order = Order.getById(event.parameterList[0].toLong())
    if (order.userId != user.id) {
      throw AccessRejectedException("Access rejected to order ${order.id}", OrderView::class.java, setOf())
    }
    // .. rest of the code, init the view, show the details about the order
  }
}
```

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
* For Vaadin 10 there's todo

## VoK Authorization

The VoK API authorization API uses role-based authorization on Vaadin views. There are
three annotations in the [AllowRoles.kt](src/main/kotlin/com/github/vok/security/AllowRoles.kt) file,
and your view must list exactly one of them otherwise it will be inaccessible:

* `AllowRoles` lists roles that are allowed to visit that view; the user must be logged in and must be assigned at least one of the roles listed in the annotation
* `AllowAll` allows anybody to see this view, even if there is no user logged in.
* `AllowAllUsers` allows any logged-in user to see this view.

These rules are quite simple and cover only the basic authorization needs. You can simply
define more complex rules as a Kotlin code in the `View.enter()` which is invoked on navigation
to that particular view.
For example, the View checks e.g. whether given user has the right
to see particular record or a document. If not, [AccessRejectedException](src/main/kotlin/com/github/vok/security/AccessRejectedException.kt) must be simply thrown.
The exception is then caught by the Vaadin exception handler and either
an error notification "access rejected" should be shown (Vaadin 8), or
the user will be presented by the "access rejected" page with HTTP 403 (Vaadin 10).

The API is intended to be very simple so that it can be backed easily by any kind
of auth scheme you need: VoK-Security built-in Simple scheme, the [OACC](http://oaccframework.org/oacc-features.html)
library, [Apache Shiro](https://shiro.apache.org/) or others.

### The vok-security module

This module only provides basic API classes which lays out the foundation of the
security mechanism. The actual integration code differs for Vaadin 8 and Vaadin 10
and is therefore located in the [vok-util-vaadin8](../vok-util-vaadin8) and
[vok-util-vaadin10](../vok-util-vaadin10) modules:
* For Vaadin 8, please see the [VokSecurity](../vok-util-vaadin8/src/main/kotlin/com/github/vok/framework/VokSecurity.kt) class;
* For Vaadin 10 please see the @todo class.

## Example projects

Please find example projects below. Both projects are using the username+password authentication with users
stored in the SQL database:

* For Vaadin 8 there's [vok-security-demo](https://github.com/mvysny/vok-security-demo)
* For Vaadin 10 there's todo

## VoK-Security Simple

The VoK-Security API does not provide much functionality out-of-the-box and it may
be hard to use it properly. That's
why VoK-Security provides a simple password-based auth with all of the best practices:

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
