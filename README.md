[![Build Status](https://travis-ci.org/mvysny/vaadin-on-kotlin.svg?branch=master)](https://travis-ci.org/mvysny/vaadin-on-kotlin)

NOTE: Current version only supports Vaadin 8 (with v7 compatibility extensions). For Vaadin 7 version please
 see the [vaadin7 branch](https://github.com/mvysny/vaadin-on-kotlin/tree/vaadin7).

# Vaadin On Kotlin

A new way of writing simple full-stack Vaadin-based web apps in Kotlin. Only requires Servlet container such as Jetty or Tomcat to run.
Features:

* Full database stack, from automatic database migrations to O/R mapping
* Simple DSL-like UI definition
* Provides a simple JPA DataProvider for easy integration of JPA beans with Grid
* No Spring nor JavaEE EJBs nor CDI necessary!

For a Getting Started guide please see the official documentation at http://www.vaadinonkotlin.eu/ .

## QuickStart

#### Run the example application from the command-line

You will only need Java 8 JDK installed. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:appRun
```

The web app will be running at http://localhost:8080/vok-example-crud

#### Run the example application from your IDE:

1. Clone this git repository and import it into your IDE, simply by opening the `build.gradle` file.
2. To run it from your IDE, just open the [Server.kt](vok-example-crud/src/test/java/com/github/vok/example/crud/Server.kt) file and launch it.
   The web app will be running at http://localhost:8080 . Please make sure that the launch/current working directory directory is set to 
   the `vok-example-crud` directory (Intellij: set `$MODULE_DIR$` to launcher's Working directory)

For other launch options please see below.

### Create your own project

You should start off the very simple [Vaadin-on-Kotlin Hello World example application](https://github.com/mvysny/vok-helloword-app) as a template.
Feel free to add your functionality to the sample app. The full Getting Started guide can be found at http://www.vaadinonkotlin.eu/ .

Just type this into your terminal:
 
```bash
git clone https://github.com/mvysny/vok-helloword-app
cd vok-helloworld-app
./gradlew clean build web:appRun
```

The web app will be running at http://localhost:8080/

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


## Status

There is [Aedict Online](https://aedict-online.eu) running on top of VoK, so there are no obvious bugs in VoK. Yet, more projects are needed to battle-prove VoK's API.

Todo:

* Improve documentation!
  * Introduce a nice web page with getting started. Probably hosted on github pages. Nobody is interested in techno-babble. Rip off Ruby-on-Rails site.
  * Introduce a Maven or Gradle archetype! For a simple Vaadin+JPA app.
    * Make the sample app use some really nice theme; Material theme? Rip off the Ruby-on-Rails tutorial.
    * Make the sample app not depend on any IDE; yet include tutorial for Intellij
  * Explain the motivation properly. Something along these lines:
    * A Java newbie tries out Vaadin and loves it. Then he tries to save stuff into the database,
      only to discover that in the Java world one needs to use Spring or JavaEE, needs to learn how to write @Transactional services, dependency injection, then configure CDI or SpringServlet or whatnot.
      That is a lot of stuff which is a) simply too overwhelming and b) totally unnecessary to do a stupid database insert. So the feeling is generally "screw that I'm moving to Ruby on Rails/Django/Grails".
      And I believe that a documentation, no matter how greatly it is written, can not hide the complexity of JavaEE and Spring. Java world is basically repulsing new developers.
    * It is amazing how many (all) web frameworks miss a simple database support: Ktor, Kara, Sparkjava, nobody offers a braindead-simple solution for simple PWAs which want to store shit into the database.
      Developers want http://loopback.io but for Java. But LoopBack is just a bloody web framework with a database support, REST and Oauth, nothing more!
    * Vaadin-on-Kotlin changes that. It allows you to write simple SQL-based apps which run on Kotlin; it is modular so you can ditch the SQL module and include a NoSQL module instead, etc etc.
  * Coordinate with the Vaadin guys to write tutorials on writing Vaadin JavaScript component in kotlin2js
* Port to Karibu-DSL
* NoSQL support?

Done:

* JPA (via Hibernate) and transactions (via `db {}`); Extended EntityManager is also supported.
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
* Locate the WAR in `vok-example-crud/build/libs/`
* Run the WAR via the Runner: `java -jar jetty-runner*.jar *.war`
* Open [http://localhost:8080](http://localhost:8080)

## To develop in IDEA:

### Embedded Jetty

* The easiest option: just open the [Server.kt](vok-example-crud/src/test/java/com/github/vok/example/crud/Server.kt) and launch it.

### Jetty

* Open the project in IDEA
* Download the Jetty Distribution zip file from here: http://download.eclipse.org/jetty/stable-9/dist/
* Unpack the Jetty Distribution
* In IDEA, add Jetty Server Local launcher, specify the path to the Jetty Distribution directory and attach the `vok-example-crud` WAR-exploded artifact to the runner
* Run or Debug the launcher

### Tomcat

* Open the project in IDEA
* Launch the `vok-example-crud` WAR in Tomcat as described here: https://kotlinlang.org/docs/tutorials/httpservlets.html
