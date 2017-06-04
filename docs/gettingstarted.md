[Index](index.html) | [Getting Started](gettingstarted.html)

# Getting started with Vaadin-on-Kotlin

This guide covers getting up and running with Vaadin On Kotlin (VOK).

> **Note:** This tutorial is heavily inspired by the excellent [Ruby on Rails tutorial](http://guides.rubyonrails.org/getting_started.html). 

After reading this guide, you will know:

* How to create a new VOK application, and connect your application to a database.
* The general layout of a VOK application.
* How to quickly generate the starting pieces of a VOK application.

## 1 Guide Assumptions

This guide is designed for beginners who want to get started with a VOK application from scratch.
It does not assume that you have any prior experience with Vaadin. However, to get the most out of it,
you need to have some prerequisites installed:
   
* Java 8 JDK or newer.

VOK is an opinionated web application framework which employs Vaadin framework running on the Kotlin programming language. If you have no 
prior experience with Kotlin nor Vaadin, you will find a very steep learning curve diving straight 
into VOK. There are several curated lists of online resources for learning Kotlin:
   
* [Official Kotlin Programming Language website](https://kotlinlang.org/)
* [Kotlin Koans](https://kotlinlang.org/docs/tutorials/koans.html)

To learn Vaadin:

* [Official Vaadin website](https://www.vaadin.com)
* [Vaadin Documentation](https://vaadin.com/docs) - we recommend to download and read the Vaadin Book PDF.

> **Note:** If you have no prior experience with Kotlin nor Vaadin, you might get overwhelmed by the sheer amount of 
the new stuff we will learn. Therefore, we recommend to take slow steps and get familiar with both Vaadin and Kotlin first. 
You can also start with the [Karibu-DSL Hello World Example](https://github.com/mvysny/karibu-helloworld-application):
VOK basically uses Karibu-DSL under the hood, therefore the lessons learned in the Karibu-DSL Hello World example will
be applicable in the VOK-based apps later on.

## 2 What is Vaadin-on-Kotlin?

VOK is a glue between Vaadin, Kotlin and other frameworks which allows you to write web apps smoothly. 
It is designed to make the art of programming of web applications easier by making assumptions about what 
every developer needs to get started. It allows you to write less code while accomplishing 
more than many other languages and frameworks.

> **Note:** Traditionally either JavaEE or Spring acted as the "glue" which held various frameworks together.
But, with the advent of the Kotlin programming language,
we believe that the features of the Kotlin programming language alone are all that's necessary in the modern programming.
We believe that Kotlin can replace the traditional approach of using the Dependency Injection to glue stuff together.

VOK is opinionated software. It makes the assumption that there is a "best" way to do things,
and it's designed to encourage that way - and in some cases to discourage alternatives.

The VOK philosophy includes two major guiding principles:

* Simplicity - things are kept as simple as possible, and libraries are used only when absolutely necessary. Complex patterns such as Dependency Injection
  and MVC are deliberately left out.
* Components as basic building blocks - Vaadin is a single-page web component framework as opposed to
  the traditional multiple page frameworks. As such, it resembles the traditional fat client
  Swing/JavaFX programming and is closer to GUI software development than traditional web development with HTML and JavaScript.
  VOK promotes code/UI reuse by means of reusing components (your components will range from basic ones
  to a complex containers, even forms) instead of creating page templates.

While the Dependency Injection (DI) itself is not hard to grok, it comes with unfortunate consequences:
* The DI forces the programmer to create Services/DAOs even for tiny CRUD operations. While having Services is a desirable
practice in larger project, it is overkill for simple projects.
* The DI requires you to run on a DI container, such as a JavaEE server, or tons of Spring libraries. While that's nothing
  new for a seasoned Java developer, this is overwhelming for a newbie which is just starting with the web app development.
* It quickly tends to get very complex as the DI configuration grows.

Therefore, VOK itself is not using DI; you can of course use Spring or JavaEE in your project alongside VOK if necessary.

> **Note on MVC**: The [Model-View-Controller](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) pattern
is very popular with page-oriented frameworks such as Ruby on Rails, Groovy on Grails and Python Django. There is typically
lot of things going on in the page, and the MVC pattern helps keeping the code separated into smaller,
more easily digestable packages. 
>
> However, since Components are a much smaller unit of reuse than pages, employing MVC with Component-oriented frameworks
does not make that much sense: for example it will usually leave you with nearly empty Views. We thus believe that using MVC does 
more harm than good since it adds unnecessary complexity. Therefore this tutorial will not use MVC.

## 3 Creating a New VOK Project
The best way to read this guide is to follow it step by step. All steps are essential to run this example application and no additional code or steps are needed.

By following along with this guide, you'll create a VOK project called blog, a (very) simple weblog.
Before you can start building the application, you need to make sure that you have Java 8 JDK installed.

### 3.1 Prerequisites

Vaadin-on-Kotlin only requires Java 8 JDK do be installed. The example application has Gradle bundled in;
Gradle will then download everything else (Vaadin, Kotlin, libraries, the Jetty server which is used to run the app from the command line).
This makes VOK applications really portable
since they work flawlessly on any OS and CPU which supports Java 8 - be it Windows, Linux or Mac, on x86, ARM or others.

The example application also uses an embedded Java database called [H2](http://www.h2database.com/html/main.html), so there
is no need for you to set up any database.

While it is possible to edit the project files using any text editor, we recommend to install Intellij IDEA which provides awesome
Kotlin support including auto-completion. You can use IDEA Community edition, which is free and allows you to run
gradle tasks to run the app, or you can purchase the Ultimate edition which also supports debugging/hot-redeployment of the web app on Tomcat and other
servers, and also offers awesome database integration tools.

### 3.2 Creating the Blog Application

Getting the example application is really easy. If you have Git installed, just open a command line and run the
following command:

```bash
$ git clone https://github.com/mvysny/vok-helloword-app
```
If you don't, you can simply download the application as a zip file from GitHub https://github.com/mvysny/vok-helloword-app/archive/master.zip

After you create the blog application, switch to its folder:

```bash
$ cd vok-helloworld-app
$ ./gradlew
```

This will download everything necessary and will compile the example application's WAR file.

> **Note:** WAR (Web ARchive) is Java standard for packaging web applications. WAR file is a zip file which
can be deployed to all Java Servlet Servers, including Tomcat, JBoss etc.

The `vok-helloworld-app` directory has a number of files and folders that make up the
structure of a VOK application. Most of the work in this tutorial will happen in the
`src/main/kotlin` folder, but here's a basic rundown on the function of each of the files and 
folders:

| File/Folder       | Purpose                                 |
|-------------------|-----------------------------------------|
| `src/main/kotlin` | Contains the source files of all of your Views, Servlets, REST endpoints, async jobs for your application. You'll focus on this folder for the remainder of this guide. |
| `src/main/webapp` | Contains the Vaadin Theme (a SCSS-based theme which gets compiled to CSS). All Themes inherit from the [Valo Theme](https://vaadin.com/valo). JavaScript files, additional CSS files and images are also placed here.  |
| `src/main/resources` | Contains the logger configuration file (`logback.xml`) and the database configuration file (`persistence.xml`) |
| `build.gradle`    | This file defines tasks that can be run from the command line. You should add your own tasks by adding code to this file. There is much you can do with Gradle - you can for example use the ssh plugin to deploy the WAR to your production environment. |
| `README.md`       | This is a brief instruction manual for your application. You should edit this file to tell others what your application does, how to set it up, and so on. |
| `src/test/kotlin` | Unit tests, fixtures, and other test apparatus. These are covered in @todo |
| .gitignore        | This file tells git which files (or patterns) it should ignore. See [Github - Ignoring files](https://help.github.com/articles/ignoring-files/) for more info about ignoring files. |

## 4 Hello, Vaadin-on-Kotlin!

To begin with, let's get some text up on screen quickly. To do this, you need to get an application server running.

### 4.1 Starting up the Web Server

You actually have a functional Rails application already. To see it, you need to start a web server on your development machine. You can do this by running the following in the blog directory:

```bash
$ ./gradlew clean web:appRun
```

This will fire up Jetty, an embeddable Java web server. To see your application in action, open a browser window and navigate
 to http://localhost:8080. You should see the Vaadin-on-Kotlin default information page:

![Welcome VOK](images/welcome_vok.png)

> **Note:** To stop the web server, hit Ctrl+C in the terminal window where it's running. To verify the server has stopped 
you should see your command prompt cursor again. For most UNIX-like systems including macOS this will be a dollar sign $. 

> **Note:** changes in theme files will only be propagated when you are running `./gradlew clean web:appRun` and there is no 
`styles.css` file. If there is, your changes will be ignored until you compile the theme again, by running
`./gradlew vaadinThemeCompile`.
>
> Changes made in your Kotlin files will be propagated to the running server only after you compile them, by
 running `./gradlew build`.
 
The "Welcome aboard" page is the smoke test for a new VOK application: it makes sure that you
have your software configured correctly enough to serve a page.

### 4.2 Say "Hello", Vaadin-on-Kotlin

To get VOK saying "Hello", you need to create a View.

A View's purpose is to provide a Vaadin Component (usually a Layout containing other components), which then interacts with the user.
The Navigator decides which View receives which requests. Usually there is exactly one route to a View. You can collect the data
to be displayed right in the View itself (for small applications), or you can define so-called Service layer
(a group of regular Kotlin classes which define a clear API and are responsible for fetching of the data).
VOK however does not enforce this, and we will not use this pattern in the tutorial.

All Vaadin Components have three parts:

* Their JavaScript-based client side which you usually can not use directly;
* A Connector which connects server-side and client-side; also not accessed directly by your code
* And a server side which you access from your code.

For example, a Button client-side (`VButton`) contains the logic to send the notification about the mouse click
server-side; server-side `Button` allows you to register listeners which listen for button clicks.

Another example: `VGrid` shows a list of data in tabular fashion; it performs scrolling and fetching of the data as the user scrolls, via the Connector.
Server-side `Grid` allows you to set the `DataSource` which will actually fetch the data, from the database or from anywhere, depending on how you implement it.

To create a new View, all that's needed is to create a Kotlin class which implements the `View` interface and extends
some Vaadin Component. 

Create the `web/src/main/kotlin/com/example/vok/MyWelcomeView.kt` file and make sure it looks like follows:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.*
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.themes.ValoTheme

@AutoView("")
class MyWelcomeView: VerticalLayout(), View {
    init {
        verticalLayout {
            label("Hello, Vaadin-on-Kotlin!") {
                styleName = ValoTheme.LABEL_H1
            }
        }
    }
    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}
```

### 4.3 Setting the Application Home Page
Now that we have made the view, we need to tell VOK when we want "Hello, Vaadin-on-Kotlin!" 
to show up. In our case, we want it to show up when we navigate to the root URL of our site, 
[http://localhost:8080](http://localhost:8080). At the moment, "Welcome aboard" is occupying that spot.

Open up the `WelcomeView.kt` file and change the `@AutoView("")` annotation to the following:
`@AutoView("old-welcome")`. This will map the original "Welcome aboard" page to
[http://localhost:8080#!old-welcome](http://localhost:8080#!old-welcome) , making space for our new Hello page.

Having the `@AutoView("")` on a View will tell the VOK Navigator to map requests to the root of the application to the `MyWelcomeView` view.

Launch the web server again and navigate to [http://localhost:8080](http://localhost:8080) in your browser. You'll see the "Hello, Vaadin-on-Kotlin!"
message you put into the `web/src/main/kotlin/com/example/vok/MyWelcomeView.kt`, indicating
that this new Navigator route is indeed going to `MyWelcomeView` and is rendering the view correctly.

## 5 Getting Up and Running

Now that you've seen how to create a view, let's create something with a bit more substance.

In the Blog application, you will now create a new database table, or in REST terms, a resource. A resource is the term used 
for a collection of similar objects, such as articles, people or animals. You can create, 
read, update and destroy items for a resource and these operations are referred to as 
CRUD operations.

VOK provides a resources method which can be used to declare a standard REST resource. But first, let us define the article.
Create the `web/src/main/kotlin/com/example/vok/Article.kt` file with the following contents:

```kotlin
package com.example.vok

import java.io.Serializable
import javax.persistence.*

@Entity
data class Article(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        var title: String? = null,

        var text: String? = null
): Serializable
```

This will define a so-called JPA entity, which basically represents a row in the "Article" database table.

You need to add a REST endpoint for the article resource, just create a file `web/src/main/kotlin/com/example/vok/ArticleRest` which will look as follows:

```kotlin
package com.example.vok

import com.github.vok.framework.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/articles")
class ArticleRest {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: Long): Article? = db { em.find(Article::class.java, id) }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): List<Article> = db { em.findAll<Article>() }
}
```

This will add the possibility to retrieve the articles via a REST call. Just try

```bash
$ wget localhost:8080/rest/articles
```

You will get 500 internal server error; the server log will show a long stacktrace, with the most interesting
part being
```
Caused by: org.h2.jdbc.JdbcSQLException: Table "ARTICLE" not found; SQL statement:
select article0_.id as id1_0_0_, article0_.text as text2_0_0_, article0_.title as title3_0_0_ from Article article0_ where article0_.id=? [42102-193]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:345)
	at org.h2.message.DbException.get(DbException.java:179)
	at org.h2.message.DbException.get(DbException.java:155)
```

That is to be expected since we haven't yet created the table for Articles. We'll do that in a minute.
In the next section, you will add the ability to create new articles in your application and be able to view them. This is the "C" and the "R" from CRUD: create and read. The form for doing this will look like this:

![Create Article Screenshot](images/create_article.png)

It will look a little basic for now, but that's ok. We'll look at improving the styling for it afterwards.

### 5.1 Laying down the groundwork

Firstly, you need a place within the application to create a new article. A great place for that 
would be at `create-article`. Navigate to [http://localhost:8080#!create-article](http://localhost:8080#!create-article) and you'll see a general error:

![Navigator Error](images/navigator_error.png)

This happens because there is no View yet, mapped to the `create-article` route. 

### 5.2 The first form

The solution to this particular problem is simple:
create a Kotlin file named `web/src/main/kotlin/com/example/vok/CreateArticleView` as follows:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.*
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.themes.ValoTheme

@AutoView
class CreateArticleView: VerticalLayout(), View {
    private val binder = beanValidationBinder<Article>()
    init {
        label("New Article") {
            styleName = ValoTheme.LABEL_H1
        }
        textField("Title") {
            bind(binder).bind(Article::title)
        }
        textArea("Text") {
            bind(binder).bind(Article::text)
        }
        button("Save Article", {
        })
    }
    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}
```
If you refresh the page now, you'll see the exact same form from our example above.
Building forms in VOK is really just that easy!

There is a problem with the form though - when you click the "Save Article" button, nothing will happen.
Currently, the click listener is empty, we will need to add the database code to save the article.

### 5.3 Creating articles

To make the "Save Article" button do something, just change the class as follows:
```kotlin
package com.example.vok

import com.github.vok.framework.db
import com.github.vok.karibudsl.*
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.themes.ValoTheme

/**
 * @author mavi
 */
@AutoView
class CreateArticleView: VerticalLayout(), View {
    private val binder = beanValidationBinder<Article>()
    init {
        label("New Article") {
            styleName = ValoTheme.LABEL_H1
        }
        textField("Title") {
            bind(binder).bind(Article::title)
        }
        textArea("Text") {
            bind(binder).bind(Article::text)
        }
        button("Save Article", {
            val article = Article()
            if (binder.writeBeanIfValid(article)) {
                db { em.persist(article) }
            }
        })
    }
    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}
```

Now when you click the "Save Article" button, you'll see the good old Oops error - it's because we haven't
created the database table for Article yet.

### 5.4 Creating the Article model

