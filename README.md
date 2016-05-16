# pokusy_kotlinee

Lots of projects actually do not use all capabilities of JavaEE, just a subset of JavaEE features: mostly the database access of course,
the Async, the REST webservices, and that's it.

This project is an attempt to simplify such projects:

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

### The DSL structural Example

```kotlin
formLayout {
  isSpacing = true
  textField("Name:") {
    trimmingConverter()
    focus()
  }
  textField("Age:")
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

### Advanced syntax

#### Keyboard shortcuts via operator overloading

```kotlin
import com.github.kotlinee.framework.vaadin.ModifierKey.Alt
import com.github.kotlinee.framework.vaadin.ModifierKey.Ctrl
import com.vaadin.event.ShortcutAction.KeyCode.C

button("Create New Person (Ctrl+Alt+C)") {
  setLeftClickListener { ... }
  setClickShortcut(Ctrl + Alt + C)
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


### JPA-based grid is a breeze

Support for sorting and filtering out-of-the-box:

```kotlin
grid(dataSource = jpaContainer<Person>()) {
  setSizeFull()
  // add a generated column with a single button which deletes the person in question
  addButtonColumn("delete", "Delete", ClickableRenderer.RendererClickListener {
    db { em.deleteById<Person>(it.itemId) }
    refreshGrid()
  })
  setColumns("id", "name", "age", "edit", "delete")
  // automatically create filters, based on the types of values present in particular columns.
  appendHeaderRow().generateFilterComponents(this)
}
```

## Motivation

Please read this first: http://vyzivus.blogspot.sk/2016/04/java-sucks.html

In the past I have implemented a Vaadin-based JavaEE project. During the implementation I was constantly plagued with the following issues:

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
transactions), manually store it into the session and then run into abovementioned issues with websocket xhr. What the heck? I want to focus on coding,
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

* Download Jetty Runner here: http://www.eclipse.org/jetty/documentation/current/runner.html
* Run `./gradlew`
* Locate the WAR in `build/libs/`
* Run the WAR via the Runner: `java -jar jetty-runner*.jar *.war`

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

