[![Build Status](https://travis-ci.org/mvysny/vaadin-on-kotlin.svg?branch=master)](https://travis-ci.org/mvysny/vaadin-on-kotlin)

# Welcome to Vaadin-On-Kotlin

Vaadin-on-Kotlin is a web-application framework that includes everything needed to create database-backed web applications.
Please see the official documentation at [www.vaadinonkotlin.eu](http://www.vaadinonkotlin.eu).

Vaadin-on-Kotlin does not enforce you to use [Model-View-Controller (MVC)](http://en.wikipedia.org/wiki/Model-view-controller),
Dependency Injection (DI) nor [Service-Oriented Architecture (SOA)](https://en.wikipedia.org/wiki/Service_(systems_architecture)).
It by default does not use Spring nor JavaEE. Instead, Vaadin-on-Kotlin focuses on simplicity.
 
The View layer leverages component-oriented
programming as offered by the [Vaadin](https://vaadin.com) framework. Vaadin offers powerful components which are built on AJAX;
programming in Vaadin resembles programming in a traditional client-side framework such as JavaFX or Swing.

The database access layer is covered by [vok-orm](https://github.com/mvysny/vok-orm) which uses the Sql2o library with additional VoK helpers.
`vok-orm` allows you to present the data from database rows as objects and embellish these data objects with business logic methods.
Of course, you may decide not to use Sql2o and integrate with NoSQL instead, or use [JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) and [Hibernate](http://hibernate.org/orm/).

Everything is combined with the conciseness of the [Kotlin](https://kotlinlang.org/)
programming language, which makes Vaadin-on-Kotlin a perfect starting point for beginner programmers.
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
./gradlew vok-example-crud-sql2o:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080).

You can find the [VoK-CRUD Live Demo](https://vok-crud.herokuapp.com/) running on Heroku.

For more information check out the [vok-example-crud-sql2o](vok-example-crud-sql2o) module.

## Vaadin 10 Flow Example project

Head to [Beverage Buddy VoK](https://github.com/mvysny/beverage-buddy-vok) for the standalone example project.

## Run the example application from Intellij IDEA Community

1. In Intellij IDEA, open the project simply by opening the `build.gradle` file, and then selecting "Open as Project".
2. To run the application from IDEA, just open the [Server.kt](vok-example-crud-sql2o/src/test/java/com/github/vok/example/crud/Server.kt) file and launch it.
   The web app will be running at [http://localhost:8080](http://localhost:8080). Please make sure that the launch/current working directory directory is set to 
   the `vok-example-crud-sql2o` directory (Intellij: set `$MODULE_DIR$` to launcher's Working directory)

If you have the Intellij IDEA Ultimate version, we recommend you to use Tomcat for development, since it offers
better code hot-redeployment:

1. Open the project in IDEA
2. Launch the `vok-example-crud-sql2o` WAR in Tomcat as described here: https://kotlinlang.org/docs/tutorials/httpservlets.html

## Contributing

We encourage you to contribute to Vaadin-on-Kotlin! Join us and discuss at [Vaadin Forums: Miscellaneous](https://vaadin.com/forum#!/category/11562).

Trying to report a possible security vulnerability in Vaadin-on-Kotlin? Please use [Vaadin Bug Tracker](https://github.com/vaadin/framework/issues).

For general Vaadin-on-Kotlin bugs, please use the [Vaadin-on-Kotlin Github Issue Tracker](https://github.com/mvysny/vaadin-on-kotlin/issues).

## Modules

Vaadin-on-Kotlin consists of several modules which offer you with handy functionality. You include the modules
into your project simply as a Gradle dependencies on simple jar artifacts, there is no magic.

Every module contains a description of what exactly the module does, when you should use it and when
it might be better to use something else.

The list of modules:

* [vok-framework](vok-framework) - the very core of Vaadin-on-Kotlin which contains machinery
  for developing VoK plugins, and also the means to bootstrap/teardown the VoK runtime.
  Always included in your project when you build your app with VoK.
* [vok-util-vaadin8](vok-util-vaadin8) - when you want to have additional support for Vaadin 8.
  You typically include this module when you build your Vaadin8-based app with VoK.
* [vok-util-vaadin10](vok-util-vaadin10) - when you want to have additional support for Vaadin 10.
  You typically include this module when you build your Vaadin10-based app with VoK.
* [vok-framework-sql2o](vok-framework-sql2o) - when you want to have additional support for Vaadin 8 and
  the support for the database using the recommended approach. Includes [vok-util-vaadin8](vok-util-vaadin8) and [vok-db](vok-db).
* [vok-framework-jpa](vok-framework-jpa) - when you want to have additional support for Vaadin 8 and
  the support for the database using the JPA access approach. Includes [vok-util-vaadin8](vok-util-vaadin8).
* [vok-framework-v10-sql2o](vok-framework-v10-sql2o) - when you want to have additional support for Vaadin 10 and
  the support for the database using the recommended approach. Note that there is no JPA support for Vaadin 10.
  Includes [vok-util-vaadin10](vok-util-vaadin10) and [vok-db](vok-db).
* [vok-rest](vok-rest) - when you want to access your database or your app classes or via the REST interface.
* [vok-db](vok-db) - Provides access to the database; uses [VoK-ORM](https://github.com/mvysny/vok-orm)

## Code Examples

### Easy database transactions:

vok-orm:
```kotlin
button("Save", { db { person.save() } })
```

See [vok-orm](https://github.com/mvysny/vok-orm) for an explanation on how this works.

JPA:

```kotlin
button("Save", { db { em.persist(person) } })
```

### Prepare your database

Simply use [Flyway](http://flywaydb.org): write Flyway scripts, add a Gradle dependency:
```groovy
compile 'org.flywaydb:flyway-core:4.2.0'
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

### Sql2o-based grid is a breeze

Support for sorting and filtering out-of-the-box:

```kotlin
grid(Person::class, dataProvider = Person.dataProvider.withConfigurableFilter()) {
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

JPA version: 

```kotlin
grid(Person::class, dataProvider = jpaDataProvider<Person>().withConfigurableFilter()) {
  ...
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

### Sample application which uses JPA

If you prefer the evil you are accustomed to, then you can find the very simple
sample JPA-based application here: [vok-example-crud-jpa](vok-example-crud-jpa).
