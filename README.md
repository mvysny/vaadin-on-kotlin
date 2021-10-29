[![Powered By Vaadin on Kotlin](https://www.vaadinonkotlin.eu/iconography/vok_badge.svg)](https://www.vaadinonkotlin.eu)
[![Join the chat at https://gitter.im/vaadin/vaadin-on-kotlin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/vaadin-on-kotlin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework)
[![Build Status](https://github.com/mvysny/vaadin-on-kotlin/actions/workflows/gradle.yml/badge.svg)](https://github.com/mvysny/vaadin-on-kotlin/actions/workflows/gradle.yml)

# Welcome to Vaadin-On-Kotlin

Vaadin-on-Kotlin is a web-application framework that includes everything needed to create database-backed web applications.
Please see the official documentation at [www.vaadinonkotlin.eu](https://www.vaadinonkotlin.eu).

Vaadin-on-Kotlin does not enforce you to use [Model-View-Controller (MVC)](https://en.wikipedia.org/wiki/Model-view-controller),
Dependency Injection (DI) nor [Service-Oriented Architecture (SOA)](https://en.wikipedia.org/wiki/Service_(systems_architecture)).
It by default does not use Spring nor JavaEE. Instead, Vaadin-on-Kotlin focuses on simplicity.
 
The View layer leverages component-oriented
programming as offered by the [Vaadin](https://vaadin.com) framework. Vaadin offers powerful components which are built on AJAX;
programming in Vaadin resembles programming in a traditional client-side framework such as JavaFX or Swing.

The database access layer is covered by the [vok-orm](https://github.com/mvysny/vok-orm) library.
`vok-orm` allows you to present the data from database rows as objects and embellish these data objects with business logic methods.
Using `vok-orm` is the recommended approach to access SQL databases.
Of course, you may decide not to use vok-orm and integrate with NoSQL instead, or use [JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) and/or [Hibernate](https://hibernate.org/orm/).

Everything is combined with the conciseness of the [Kotlin](https://kotlinlang.org/)
programming language, which makes Vaadin-on-Kotlin a perfect starting point for beginner programmers.
And Kotlin is statically-typed, so you can always Ctrl+Click on a code and learn how it works under the hood! 

For a Getting Started guide please see the official documentation at [www.vaadinonkotlin.eu/](https://www.vaadinonkotlin.eu/).

## Getting Started

1. Please install Java 8 JDK and git client if you haven't yet.

2. Then, at the command prompt, just type in:

    ```bash
    git clone https://github.com/mvysny/vok-helloworld-app
    cd vok-helloworld-app
    ./gradlew clean build web:appRun
    ```

3. Using a browser, go to [http://localhost:8080](http://localhost:8080) and you'll see: "Yay! You're on Vaadin-on-Kotlin!"

4. Follow the guidelines to start developing your application. You may find the following resources handy:

    * [Getting Started](https://www.vaadinonkotlin.eu/gettingstarted.html)

5. For easy development, we encourage you to edit the project sources in [Intellij IDEA](https://www.jetbrains.com/idea/);
  the Community Edition is enough.

## Example project

A more polished example application which you can inspire from. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud-vokdb:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080).

You can find the [VoK-CRUD Live Demo](https://vok-crud.herokuapp.com/) running on Heroku.

For more information check out the [vok-example-crud-vokdb](vok-example-crud-vokdb) module.

## Vaadin 14 Flow Example project

Head to [Beverage Buddy VoK](https://github.com/mvysny/beverage-buddy-vok) for the standalone example project.

## Run the example application from Intellij IDEA Community

1. In Intellij IDEA, open the project simply by opening the `build.gradle` file, and then selecting "Open as Project".
2. To run the application from IDEA, just open *Gradle* tab, select `vok-example-crud-vokdb / Tasks / gretty / appRun`,
   right-click and select Debug. 
   The web app will be running at [http://localhost:8080](http://localhost:8080).

If you have the Intellij IDEA Ultimate version, we recommend you to use Tomcat for development, since it offers
better code hot-redeployment:

1. Open the project in IDEA
2. Launch the `vok-example-crud-vokdb` WAR in Tomcat as described here: https://kotlinlang.org/docs/tutorials/httpservlets.html

## Contributing

We encourage you to contribute to Vaadin-on-Kotlin! Join us and discuss at [Vaadin Forums: Miscellaneous](https://vaadin.com/forum#!/category/11562).

Trying to report a possible security vulnerability in Vaadin-on-Kotlin? Please use [Vaadin Bug Tracker](https://github.com/vaadin/framework/issues).

For general Vaadin-on-Kotlin bugs, please use the [Vaadin-on-Kotlin Github Issue Tracker](https://github.com/mvysny/vaadin-on-kotlin/issues).

## Modules

Vaadin-on-Kotlin consists of several modules which provides you with handy functionality. To include the modules
into your project, you simply add appropriate Gradle jar dependencies to your `build.gradle`.

Every module contains a description of what exactly the module does, when you should use it and when
it might be better to use something else.

The list of modules:

* [vok-framework](vok-framework) - the very core of Vaadin-on-Kotlin which contains machinery
  for developing VoK plugins, and also the means to bootstrap/teardown the VoK runtime.
  Always included in your project when you build your app with VoK.
* [vok-util-vaadin](vok-util-vaadin) - when you want to have additional support for Vaadin 14.
  You typically include this module when you build your Vaadin10-based app with VoK.
* [vok-framework-vokdb](vok-framework-v10-vokdb) - when you want to have additional support for Vaadin 14 and
  the support for the database using the recommended approach.
  Includes [vok-util-vaadin](vok-util-vaadin10) and [vok-db](vok-db).
* [vok-rest](vok-rest) - when you want to expose data from your VoK app to other REST-consuming clients.
* [vok-rest-client](vok-rest-client) - when you want to consume data in your VoK app from other REST servers.
* [vok-db](vok-db) - Provides access to the database; uses [VoK-ORM](https://github.com/mvysny/vok-orm)
* [vok-security](vok-security) - provides basic security support. The documentation there explains the basics and provides links to sample projects.

## Code Examples

### Easy database transactions:

vok-orm:
```kotlin
button("Save", { db { person.save() } })
```

See [vok-orm](https://github.com/mvysny/vok-orm) for an explanation on how this works.

### Prepare your database

Simply use [Flyway](https://flywaydb.org): write Flyway scripts, add a Gradle dependency:
```groovy
compile 'org.flywaydb:flyway-core:7.1.1'
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

### vok-orm-based grid is a breeze

Support for sorting and filtering out-of-the-box:

```kotlin
grid<User>(dataProvider = Person.dataProvider) {
  isExpand = true
  
  val filterBar = appendHeaderRow().asFilterBar(this)

  columnFor(User::id) {
      filterBar.forField(NumberRangePopup(), this).inRange()
  }
  columnFor(User::username) {
      filterBar.forField(TextField(), this).ilike()
  }
  columnFor(User::roles) {
      filterBar.forField(TextField(), this).ilike()
  }
  columnFor(User::hashedPassword)
  addButtonColumn(VaadinIcon.EDIT, "edit", { createOrEditUser(it) }) {}
  addButtonColumn(VaadinIcon.TRASH, "delete", { it.delete(); refresh() }) {}
}
```

### Advanced syntax

#### Keyboard shortcuts via operator overloading

```kotlin
import com.github.mvysny.karibudsl.v8.ModifierKey.Alt
import com.github.mvysny.karibudsl.v8.ModifierKey.Ctrl
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

# Further Links

* Visit [Vaadin Forums](https://vaadin.com/forum/category/203413/07-add-ons) to discuss any issues
  or questions regarding Vaadin-on-Kotlin
* Please file bug reports at the [VoK Bug Tracker](https://github.com/mvysny/vaadin-on-kotlin/issues)
* Chat freely at [VoK Gitter](https://gitter.im/vaadin/vaadin-on-kotlin)
* For troubleshooting, please check the [Vaadin Troubleshooting](https://mvysny.github.io/Vaadin-troubleshooting/) article.

# License

Licensed under the [MIT License](https://opensource.org/licenses/MIT).

Copyright (c) 2017-2018 Martin Vysny

All rights reserved.

Permission is hereby granted, free  of charge, to any person obtaining
a  copy  of this  software  and  associated  documentation files  (the
"Software"), to  deal in  the Software without  restriction, including
without limitation  the rights to  use, copy, modify,  merge, publish,
distribute,  sublicense, and/or sell  copies of  the Software,  and to
permit persons to whom the Software  is furnished to do so, subject to
the following conditions:

The  above  copyright  notice  and  this permission  notice  shall  be
included in all copies or substantial portions of the Software.
THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
