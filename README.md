# pokusy_kotlinee

Lots of projects actually do not use all capabilities of JavaEE, just a subset of JavaEE features: mostly the database access of course,
the Async, the REST webservices, and that's it.

This project is an (oppiniated) attempt to simplify such projects:

* Allow them to run in a pure servlet environment (such as Jetty, Tomcat)
* Remove complex stuff such as injections, SLSBs, SFSBs
* Allow any object to be bound to a session (e.g. caches) in a simple manner

Uses Kotlin. Currently starts its own embedded H2 database. Basically, what I'm trying to do is a very simple Vaadin-based project with async/push support
and database support - a very simple but powerful quickstart project.

## Examples

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
        val flyway = Flyway()
        flyway.dataSource = Kotlinee.getDataSource()
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
      trimmingConverter()
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
grid(dataSource = jpaContainer<Person>()) {
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
import com.github.kotlinee.framework.vaadin.ModifierKey.Alt
import com.github.kotlinee.framework.vaadin.ModifierKey.Ctrl
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

Please find the very simple sample application here: [kotlinee-example-crud](kotlinee-example-crud). The application demonstrates the following things:

* Linking to a database. Kotlinee uses Hibernate for JPA O/R mapping when accessing the database. The example project is simply using an in-memory H2 database, so that no additional setup is necessary. See 
  [build.gradle](kotlinee-example-crud/build.gradle) the db section for more details.
  To link to the database, we use the traditional JPA [persistence.xml](kotlinee-example-crud/src/main/resources/META-INF/persistence.xml). Please note that HikariCP is used for DB
  connection pooling, which provides production-grade performance.
* Preparing the database: simply run Flyway migration every time before the app is started, to make sure that the app has newest database ready.
  The migration is safe on cluster as well as a database lock is obtained.
  Please see [Bootstrap.kt](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/Bootstrap.kt)
  You will need to write the database migration scripts yourself: see [sample migrations](kotlinee-example-crud/src/main/resources/db/migration) for details. More details here: https://flywaydb.org/documentation/migration/sql
* Accessing the database: just create your JPA beans [(example Person)](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/crud/Person.kt) and use them in any way you see fit:
  `val allPersons = db { em.findAll<Person>() }`. The `db` is just a function defined in [DB.kt](kotlinee-framework/src/main/java/com/github/kotlinee/framework/DB.kt), you can call this from anywhere, be it Vaadin click listener or background thread. No injections/beans/EJBs/whatever necessary!
* Serving the data via REST: add RESTEasy to your project, see [build.gradle](kotlinee-example-crud/build.gradle). Then, declare REST Application to bind the REST to a particular URL endpoint, see
  [Bootstrap.kt](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/Bootstrap.kt)
  the `@ApplicationPath("/rest")` stanza. After that, just define your REST-accessing classes, for example
  [PersonRest](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/PersonRest.kt)
* Creating the UI: there are lots of great Vaadin tutorials, in general you declare UI and populate it with components. See
  [MyUI](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/MyUI.kt)
* Create Update Delete (CRUD): no Scaffolding-like UI generator for now, but you can see the [crud example](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/crud) on how to write one yourself.
* Logging: uses SLF4j with Logback, configured as follows: [logback.xml](kotlinee-example-crud/src/main/resources/logback.xml)
* Session-stored cache which of course can access database anytime: see [LastAddedPersonCache.kt](kotlinee-example-crud/src/main/java/com/github/kotlinee/example/crud/LastAddedPersonCache.kt).
* Running: [kotlinee-example-crud](kotlinee-example-crud) is a standard WAR application which you can run from your IDE directly. Please see below for some tips on how to do that.

## Motivation

Please read this (potentionally opinionated, offensive and subjective - I apologize upfront) post first: http://vyzivus.blogspot.sk/2016/04/java-sucks.html

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

## Status

This is just a prototype project. A real-world app needs to be built on top of this, to see how well this quasi-framework will fare. There is a sample
CRUD application implemented, please see `MyUI.kt` for details.

Done:

* JPA (via Hibernate) and transactions (via `db {}`); Extended EntityManager is also supported
* Migrations (Flyway) - the migrations are run automatically when the WAR is started.
* Vaadin with JPAContainer and Extended EntityManager, including a filter generator which auto-generates filter Fields for your Grid
* Async tasks & Vaadin Push
* Drop-in replacements for SFSBs bound to session: see `LastAddedPersonCache.kt` for details.
* REST+JSON (via RESTEasy); see `PersonRest.kt` for details.
* Vaadin DSL builder - see `MyUI.kt` for details.
* JDBC connection pooling (HikariCP)

Ignored:

* Messaging
* Security
* Injections

## To run the WAR outside of any IDE:

Run it with Jetty Runner:

* Download Jetty Runner here: http://www.eclipse.org/jetty/documentation/current/runner.html
* Run `./gradlew`
* Locate the WAR in `build/libs/`
* Run the WAR via the Runner: `java -jar jetty-runner*.jar *.war`
* Open http://localhost:8080/

## To develop in IDEA:

### Jetty

* Open the project in IDEA
* Download the Jetty Distribution zip file from here: http://download.eclipse.org/jetty/stable-9/dist/
* Unpack the Jetty Distribution
* In IDEA, add Jetty Server Local launcher, specify the path to the Jetty Distribution directory and attach the `kotlinee-example-crud` WAR-exploded artifact to the runner
* Run or Debug the launcher

### Tomcat

* Open the project in IDEA
* Launch the `kotlinee-example-crud` WAR in Tomcat as described here: https://kotlinlang.org/docs/tutorials/httpservlets.html

