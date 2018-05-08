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

The fictional booking system above is just a recipe which needs to be followed, a set of steps to be performed. It has no state, apart
from which part of the recipe we are executing now. We don't have any preparation steps which need to be followed in order
for the function to work. The database can simply be accessed by the means of the `db {}` method (or via `Dao` methods) which
can be called from anywhere. We can therefore simply create a *function* which performs the booking. Since we will probably
have multiple functions dealing with the bookings (say, print a receipt when the guest leaves), we will *group* the functions
in a single common class:

```kotlin
object BookingService {
  fun book(from: LocalDate, to: LocalDate) {
    ...
  }
}
```

The `object` thing is just a Kotlin word for singleton. Now you can simply call the function as

```kotlin
BookingService.book()
```

Since it's a singleton, multiple threads may call the `book()` function
at the same time. It is therefore important that the `book()` function does not store its computation (not even temporarily) in
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

Now you will simply call the login service as follows:

```kotlin
class LoginView : VerticalLayout() {
    init {
        setSizeFull()
        loginForm("VoK Security Demo") {
            alignment = Alignment.MIDDLE_CENTER
            (content as VerticalLayout).label("Log in as user/user or admin/admin")

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
        }
    }
}
```

When we tie our services to the `Session`, we are effectively building up a *repository* (a *directory*) of services.
It is extremely easy to look up the service we need, simply by using IDE's auto-completion features: simply type in
`Session.` and press `Ctrl+Space` and your IDE will list all extension properties including the `loginService`.

This has the following advantages:

* Different modules can attach different services to the `Session` object, thus building up a service repository. This way you can
  reuse services within different apps.
* The IDE's auto-completion will provide the searchable list of all services. No IDE plugin is needed for this since the extension property
  is a language feature of the Kotlin programming language.
* The type safety ensures that the service exists in compile time. If it doesn't, your program won't compile. This is superior to
  runtime configuration as provided by Spring, since you don't have to wait 2 minutes for your app to boot up and crash only because
  your misconfigured Spring annotations somewhere.
* No proxy classes and no runtime class enhancement magic. No 30+ stack traces through proxies/interceptors/DI container internals. Fast, simple
  and reliable.
