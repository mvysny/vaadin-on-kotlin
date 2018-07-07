[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Writing Services

In a simple app it is enough for the UI to access the database directly, through
the entity's `Dao`s. However, more complex app tends to have so-called
*business logic code* which deals with the app's intrinsic functionality.

For example, in a fictional hotel booking system, a successful booking must ensure that:

* The room vacancy service picks a free room and books it for the duration of the stay;
* The key card issuing service has been notified to issue a card for that room;
* The billing service needs to open an account for the room and the customer for
  the duration of the stay;
* The cleaning service must be notified to make preparations for the room

This could potentially be a lot of code; mixing this kind of code with the UI-dealing code
may create unnecessarily complex objects. Also, this kind of functionality may be invoked
from multiple parts of the code base:

* From the Vaadin UI controlled by the customer;
* from the administrative pages by the hotel receptionist;
* From a partner booking service via a REST endpoint.

The best way to have this kind of functionality in one place only is to extract it into a *service*.

> **Note:** When using *dependency injection* (DI) containers such as Spring and JavaEE, you would typically turn the
services into a bean, so that you can have access to the database. However, your UI components can't simply obtain bean
instances - you can't just instantiate the bean yourself since Spring wouldn't inject stuff into the bean.
>
> You need to either obtain the instances from a global `ApplicationContext` instance, or have them injected into the View
and provide them for all data-bound UI components, or turn all of your UI components into beans. All of these solutions have
disadvantages, and therefore VoK is not using DI and is using a much simpler approach.

## The stateless service

The fictional booking system above is just a set of steps to be performed. The process does not have any state, at most couple of
local variables. We don't have any preparation steps which need to be followed in order
for the function to work. We don't have to run special steps to access the database since the database
can simply be accessed by the means of the `db{}` block or via `Dao` methods which
can be called from anywhere (read more about this in the [Accessing SQL databases](databases.md) Guide).

We can therefore simply create a single standalone *function* which performs the booking; a set of such standalone functions
can then form the *service layer*.

We can also *group* functions which perform related tasks together. Say that we expect to have
multiple functions dealing with the bookings; for example another function can handle printing of a receipt when the guest leaves.
We can group such function in a single common class:

```kotlin
object BookingService {
  fun book(from: LocalDate, to: LocalDate) {
    ...
  }
  fun printReceiptAsPdf(bookingId: Long): ByteArray {
    ...
  }
}
```

> **Note:** The `object` keyword is just a Kotlin word for singleton; since the functions do not need global state, we don't need multiple
instances of the `BookingService`

Now you can simply call the function as follows:

```kotlin
BookingService.book()
```

Since it's a singleton, multiple threads may call the `book()` function
at the same time, on the same instance. It is therefore important that the `book()` function does not store its computation (not even temporarily) in
some kind of a global variable, or into a field/property of the `BookingService`. If this is needed,
simply turn the service into a class:

```kotlin
class BookingService {
  fun book(from: LocalDate, to: LocalDate) {
    ...
  }
}
```

To call it, just call

```kotlin
BookingService().book()
```

Creating a new instance of an object is very cheap on current JVMs so there is no worry for performance.

## A repository of services

Having `BookingService` as a simple POJO is great since there is no dark injection/enhancement magic going on. However,
it has couple of disadvantages:

1. Refactoring of the service from `object` to `class` and vice versa requires you to change all call sites,
   `BookingService().book()` to `BookingService.book()` and vice versa.
2. For a new programmer in the project it is next to impossible to discover all services which are at his disposal.
   You can establish some naming convention (all service classes must end with `Service`), or
   implement a `Service` interface, but that still doesn't provide a simple and handy way of
   listing all services in your IDE.

Luckily, there is a way. We will build a repository of services.

Consider having the following object:
```kotlin
object Services
```
This is just an empty singleton with no methods and no fields, and it appears to be completely useless.
However, with Kotlin we can extend any class with additional fields and methods, by the
means of extension methods. So, we can simply add getters for all services into the `Services`
object. That would allow us to simply type `Services.` into your IDE, then press `Ctrl+Space`
and your IDE will list all services you have in your app. Awesome!

In other words: when we 'extend' the `Services` object with services, we are effectively building up a *repository* (a *directory*) of services.
This directory can be iterated in development time, simply by using IDE's auto-completion features. In your IDE, you simply type in
the "`Services.`" stanza, press the `Ctrl+Space` keys and your IDE will list all extension properties including the `bookingService`.

This approach has the following advantages:

* Different modules can attach different services to the `Services` object, thus building up a service repository. This way you can
  reuse services within different apps.
* The IDE's auto-completion will provide the searchable list of all services. No IDE plugin is needed for this since the extension property
  is a language feature of the Kotlin programming language itself.
* The type safety ensures that the service exists in compile time and is accessible. If it isn't, your program won't compile.
  This is superior to runtime configuration since the configuration errors show up much faster, at compile time.
* There are no proxy classes and there is no runtime class enhancement magic; hence there are no 30+ stack traces through
  proxies/interceptors/DI container internals. Because of that, this approach is fast, simple and reliable.

An example of attaching of the `BookingService` to the `Services` object:

```kotlin
val Services.booking: BookingService get() = BookingService()
```

Now you can write this into your code:

```kotlin
...
Services.booking.book()
...
```

## The stateful service

Sometimes services need to have a state. For example we could have a `LoginService` which would deal with user
logins, logouts and would provide access to the currently logged-in user.
Of course we could implement this kind of service simply as a stateless service as demoed above, simply storing
the current `User` entity into the session. However, that would allow other parts of the app to access
(and even remove) the entity from the session. To better guide the users of the API,
let's create a stateful service:

```kotlin
class LoginService: Serializable {
    var user: User? = null
    private set

    val isLoggedIn: Boolean get() = user != null

    fun login(user: User) {
        check(this.user == null) { "An user is already logged in" }
        this.user = user
        Page.getCurrent().reload()
    }

    fun logout() {
        Session.current.close()
        Page.getCurrent().reload()
    }
}
```

Since we don't have any DI, this is just a simple Kotlin class, nothing more. Obviously we need to bind
the instance of this class to the user seat, or user session. Therefore the easiest way is to store the service
into the http session and only make it accessible from there. We can define a static function which will
provide the service instance for us, creating it if it doesn't exist yet:

```kotlin
fun getLoginService(): LoginService {
    var service: LoginService? = Session["loginService"] as LoginService?
    if (service == null) {
        service = LoginService()
        Session["loginService"] = service
    }
    return service
}
```

This is a lot of code to write for every single service. Luckily, we can do better:

* Instead of `Session["loginService"]` we can use `Session[LoginService::class]` which already provide instances of `LoginService`, so no need to cast.
  It will store the object under the String key which is the full class name of `LoginService`.
* We can introduce a `getOrPut()` function which will only instantiate `LoginService` if it's not yet in the Session
* Finally, and most importantly, we can create the `loginService` to be an extension property on the `Session` itself.

```kotlin
val Session.loginService: LoginService get() = getOrPut { LoginService() }
```

Now you will simply call the login service as follows (an excerpt of the `LoginView`):

```kotlin
onLogin { username, password ->
    val user = User.findByUsername(username)
    if (user == null) {
        usernameField.componentError = UserError("The user does not exist")
    } else if (!user.passwordMatches(password)) {
        passwordField.componentError = UserError("Invalid password")
    } else {
        Session.loginService.login(user)
    }
}
```

When we 'extend' the `Session` object with services, we are effectively building up a *repository* (a *directory*) of services.
This directory can be iterated in development time, simply by using IDE's auto-completion features. In your IDE, you simply type in
the "`Session.`" stanza, press the `Ctrl+Space` keys and your IDE will list all extension properties including the `loginService`.
