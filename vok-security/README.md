# VoK-Security

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
* Other means: smart cards, fingerprints, ...
* All that while supporting SSO, or using the Servlet container-provided authentication mechanism
  (the `ServletContext.login()` method).

It is impossible to create an API convering all those cases without going abstraction-crazy.
That's why we deliberately avoid to use an all-encompassing library like Shiro or Spring Security
with insanely complex APIs. We also don't provide our own authentication API (since it would
either be incomplete or complex). In this case, the best abstraction is no abstraction at all.

Instead we demo the best practices on the simplest username+password scenario.
We then rely on the programmer to read the tutorials, the documentation and copy-paste
from code examples to create authentication mechanism that's best tailored for his project.

> Note: sorry, there are no other tutorials beside the most simple username+password authentication,
with role-based authorization and users stored in a SQL database.

## VoK-Security API

The VoK API provides no direct support for authentication. It is expected that the
application programmer will employ appropriate authentication mechanism himself.
We provide a very simple password-based example though, to get you started;
VoK also provide basic login forms and the documentation on how to integrate them
with your app.

### Authorization

The VoK API authorization API uses role-based authorization on Vaadin views - the
user simply must have all roles present in the [@HasRoles](src/main/kotlin/com/github/vok/security/HasRoles.kt) annotation attached on the `View` class to qualify, otherwise it's a security violation.
It is expected that a user name with assigned roles can be obtained from the authentication
process.

More complex authorization is simply provided via Kotlin code in the
View itself. On navigation, the View checks e.g. whether given user has the right
to see particular record or a document. If not, [AccessRejectedException](src/main/kotlin/com/github/vok/security/AccessRejectedException.kt) must be simply thrown.
The exception is then caught by the Vaadin exception handler and either
an error notification "access rejected" should be shown (Vaadin 8), or
the user will be presented by the "access rejected" page with HTTP 403 (Vaadin 10).

The API is intended to be very simple so that it can be backed easily by any kind
of auth scheme: VoK-Security built-in Simple scheme, the [OACC](http://oaccframework.org/oacc-features.html)
library, or others.

### The vok-security module

This module only provides basic API classes which lays out the foundation of the
security mechanism. The actual security-checking code differs for Vaadin 8 and Vaadin 10
and is therefore located in the [vok-util-vaadin8](../vok-util-vaadin8) and
[vok-util-vaadin10](../vok-util-vaadin10) modules.
For Vaadin 8, please see the [VokSecurity](../vok-util-vaadin8/src/main/kotlin/com/github/vok/framework/VokSecurity.kt) class;
for Vaadin 10 please see the @todo class.

## Example projects

@todo add example projects for Vaadin8 and Vaadin10

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
