[![Build Status](https://travis-ci.org/mvysny/vaadin-on-kotlin.svg?branch=master)](https://travis-ci.org/mvysny/vaadin-on-kotlin)

# Welcome to Vaadin-On-Kotlin

Vaadin-on-Kotlin is a web-application framework that includes everything needed to create database-backed web applications.

Vaadin-on-Kotlin does not enforce you to use [Model-View-Controller (MVC)](http://en.wikipedia.org/wiki/Model-view-controller),
Dependency Injection (DI) nor [Service-Oriented Architecture (SOA)](https://en.wikipedia.org/wiki/Service_(systems_architecture)).
It by default does not use Spring nor JavaEE. Instead, Vaadin-on-Kotlin focuses on simplicity.
 
The View layer leverages component-oriented
programming as offered by the [Vaadin](https://vaadin.com) framework. Vaadin offers powerful components which are built on AJAX;
programming in Vaadin resembles programming in a traditional client-side framework such as JavaFX or Swing.

The database access layer is covered by [JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) and [Hibernate](http://hibernate.org/orm/).
JPA allows you to present the data from database rows as objects and embellish these data objects with business logic methods.
Of course, you may decide not to use JPA and integrate with NoSQL instead.

Everything is combined with the conciseness of the [Kotlin](https://kotlinlang.org/)
programming language, which makes Vaadin-on-Kotlin a perfect starting point for newcomer programmers.
And Kotlin is statically-typed, so you can always Ctrl+Click on a code and learn how it works under the hood! 

For a Getting Started guide please see the official documentation at [http://www.vaadinonkotlin.eu/](http://www.vaadinonkotlin.eu/).

## Getting Started

1. Please install Java 8 JDK and git client if you haven't yet.

2. Then, at the command prompt, just type in:

    ```bash
    git clone https://github.com/mvysny/vok-helloword-app
    cd vok-helloworld-app
    ./gradlew clean build web:appRun
    ```

3. Using a browser, go to [http://localhost:8080](http://localhost:8080) and you'll see: "Yay! You're on Vaadin-on-Kotlin!"

4. Follow the guidelines to start developing your application. You may find the following resources handy:

    * [Getting Started](http://www.vaadinonkotlin.eu/gettingstarted.html)

5. For easy development, we encourage you to edit the project sources in [Intellij IDEA](https://www.jetbrains.com/idea/);
  the Community Edition is enough.

## Example project

A more polished example application which you can inspire from. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080).

## Run the example application from Intellij IDEA Community

1. In Intellij IDEA, open the project simply by opening the `build.gradle` file, and then selecting "Open as Project".
2. To run the application from IDEA, just open the [Server.kt](vok-example-crud/src/test/java/com/github/vok/example/crud/Server.kt) file and launch it.
   The web app will be running at [http://localhost:8080](http://localhost:8080). Please make sure that the launch/current working directory directory is set to 
   the `vok-example-crud` directory (Intellij: set `$MODULE_DIR$` to launcher's Working directory)

If you have the Intellij IDEA Ultimate version, we recommend you to use Tomcat for development, since it offers
better code hot-redeployment:

1. Open the project in IDEA
2. Launch the `vok-example-crud` WAR in Tomcat as described here: https://kotlinlang.org/docs/tutorials/httpservlets.html

## Contributing

We encourage you to contribute to Vaadin-on-Kotlin! Join us and discuss at [Vaadin Forums: Miscellaneous](https://vaadin.com/forum#!/category/11562).

Trying to report a possible security vulnerability in Vaadin-on-Kotlin? Please use [Vaadin Bug Tracker](https://github.com/vaadin/framework/issues).

For general Vaadin-on-Kotlin bugs, please use the [Vaadin-on-Kotlin Github Issue Tracker](https://github.com/mvysny/vaadin-on-kotlin/issues).

## Code Examples

### Easy database transactions:

```kotlin
button("Save", { db { em.persist(person) } })
```

### Prepare your database

Simply use [Flyway](http://flywaydb.org): write Flyway scripts, add a Gradle dependency:
```groovy
compile 'org.flywaydb:flyway-core:4.0.3'
```
and introduce a context listener, to auto-update your database to the newest version before your app starts:
```kotlin
@WebListener
class Bootstrap: ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        VaadinOnKotlin.init()
        val flyway = Flyway()
        flyway.dataSource = VaadinOnKotlin.getDataSource()
        flyway.migrate()
    }
}
```
Please scroll below for more details.

### Defining UI DSL-style

```kotlin
verticalLayout {
  formLayout {
    isSpacing = true
    textField("Name:") {
      focus()
    }
    textField("Age:")
  }
  horizontalLayout {
    w = 100.perc
    isSpacing = true
    button("Save") {
      onLeftClick { okPressed() }
      setPrimary()
    }
  }
}
```

### Simple popups

```kotlin
popupView("Details") {
  verticalLayout {
    formLayout { ... }
    button("Close", { isPopupVisible = false })
  }
}
```

### JPA-based grid is a breeze

Support for sorting and filtering out-of-the-box:

```kotlin
grid(Person::class, dataProvider = jpaDataProvider<Person>().withConfigurableFilter()) {
  setSizeFull()
  cols {
    column(Person::id) {
      isSortable = false
    }
    column(Person::name)
    column(Person::age)
    button("edit", "Edit", { createOrEditPerson(db { em.get<Person>(it.itemId) } ) })
    button("delete", "Delete", { deletePerson(it.itemId as Long) })
  }
  // automatically create filters, based on the types of values present in particular columns.
  appendHeaderRow().generateFilterComponents(this)
}
```

### Advanced syntax

#### Keyboard shortcuts via operator overloading

```kotlin
import com.github.vok.framework.vaadin.ModifierKey.Alt
import com.github.vok.framework.vaadin.ModifierKey.Ctrl
import com.vaadin.event.ShortcutAction.KeyCode.C

button("Create New Person (Ctrl+Alt+C)") {
  onLeftClick { ... }
  clickShortcut = Ctrl + Alt + C
}
```

#### Width/height

```kotlin
button {
  icon = ...
  w = 48.px
  h = 50.perc
}
if (button.w.isFillParent) { ... }
```

## How this is done / Sample application

Please find the very simple sample application here: [vok-example-crud](vok-example-crud). The application demonstrates the following things:

* Linking to a database. VaadinOnKotlin uses Hibernate for JPA O/R mapping when accessing the database. The example project is simply using an in-memory H2 database, so that no additional setup is necessary. See 
  [build.gradle](vok-example-crud/build.gradle) the db section for more details.
  To link to the database, we use the traditional JPA [persistence.xml](vok-example-crud/src/main/resources/META-INF/persistence.xml). Please note that HikariCP is used for DB
  connection pooling, which provides production-grade performance.
* Preparing the database: simply run Flyway migration every time before the app is started, to make sure that the app has newest database ready.
  The migration is safe on cluster as well as a database lock is obtained.
  Please see [Bootstrap.kt](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/Bootstrap.kt)
  You will need to write the database migration scripts yourself: see [sample migrations](vok-example-crud/src/main/resources/db/migration) for details. More details here: https://flywaydb.org/documentation/migration/sql
* Accessing the database: just create your JPA beans [(example Person)](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/personeditor/Person.kt) and use them in any way you see fit:
  `val allPersons = db { em.findAll<Person>() }`. The `db` is just a function defined in [DB.kt](vok-framework/src/main/kotlin/com/github/vok/framework/DB.kt), you can call this from anywhere, be it Vaadin click listener or background thread. No injections/beans/EJBs/whatever necessary!
* Serving the data via REST: add RESTEasy to your project, see [build.gradle](vok-example-crud/build.gradle). Then, declare REST Application to bind the REST to a particular URL endpoint, see
  [Bootstrap.kt](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/Bootstrap.kt)
  the `@ApplicationPath("/rest")` stanza. After that, just define your REST-accessing classes, for example
  [PersonRest](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/PersonRest.kt)
* Creating the UI: there are lots of great Vaadin tutorials, in general you declare UI and populate it with components. See
  [MyUI](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/MyUI.kt)
* Create Update Delete (CRUD): no Scaffolding-like UI generator for now, but you can see the [crud example](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/personeditor) on how to write the CRUD UI yourself very easily.
* Logging: uses SLF4j with Logback, configured as follows: [logback.xml](vok-example-crud/src/main/resources/logback.xml)
* Session-stored cache which of course can access database anytime: see [LastAddedPersonCache.kt](vok-example-crud/src/main/kotlin/com/github/vok/example/crud/LastAddedPersonCache.kt).
* Running: [vok-example-crud](vok-example-crud) is a standard WAR application which you can run from your IDE directly. Please see below for some tips on how to do that.

## Motivation

In the past I have implemented a Vaadin-based JavaEE project. During the implementation I was constantly plagued with the following JavaEE issues:

* Crashes when accessing @SessionScoped beans from websocket xhr code - https://vaadin.com/forum#!/thread/11474306 ; @NormalUIScoped produces
org.apache.openejb.core.stateful.StatefulContainer$StatefulCacheListener timedOut and javax.ejb.NoSuchEJBException - you need to add
@StatefulTimeout(-1) everywhere and use @PreserveOnRefresh on your UI - stupid.
* Moronic async API: when async method is called directly from another method in that very SLSB, the method is actually silently
called synchronously. To call async, you need to inject self and call the method as `self.method()`
* You can only inject stuff into UI and View - you cannot inject stuff into arbitrary Vaadin components. Well, you can if 
you make every Vaadin component a managed bean, but that's just plain weird. How about
having a global val with a getter which produces the correct bean instance on demand?
* Imagine that you wish to store a class, which is able to access a database, to a session. Some sort of cache, perhaps. In order to do this
JavaEE-way, you need to use CDI, annotate the class with @SessionScoped, @Inject some SLSB to it (cause managed beans do not yet have support for
transactions), manually store it into the session and then run into above-mentioned issues with websocket xhr. What the heck? I want to focus on coding,
not @configuring the world until JavaEE is satisfied.

Lots of projects actually do not use all capabilities of JavaEE, just a subset of JavaEE features: mostly the database access of course,
the Async, the REST webservices, and that's it.

This project is an (opiniated) attempt to simplify such projects:

* Allow them to run in a pure servlet environment (such as Jetty, Tomcat)
* Remove complex stuff such as injections, SLSBs, SFSBs
* Allow any object to be bound to a session (e.g. caches) in a simple manner
