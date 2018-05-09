[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Getting started with Vaadin-on-Kotlin Vaadin 10

This guide covers getting up and running with Vaadin On Kotlin (VoK).

> **Note:** This tutorial is heavily inspired by the excellent [Ruby on Rails tutorial](http://guides.rubyonrails.org/getting_started.html). 

After reading this guide, you will know:

* How to create a new VoK application, and connect your application to a database.
* The general layout of a VoK application.
* How to quickly write the starting pieces of a VoK application.

> **Note:** To skip the introduction and dive straight into the code, just skip to [Chapter 3.2](#3_2)

## 1 Guide Assumptions

This guide is designed for beginners who want to get started with a VoK application from scratch.
It does not assume that you have any prior experience with Vaadin. However, to get the most out of it,
you need to have some prerequisites installed:

* Java 8 JDK or newer.

VoK is an opinionated web application framework which employs Vaadin framework running on the Kotlin programming language. If you have no 
prior experience with Kotlin, you will find a very steep learning curve diving straight 
into VoK. There are several curated lists of online resources for learning Kotlin:

* [Official Kotlin Programming Language website](https://kotlinlang.org/)
* [Kotlin Koans](https://kotlinlang.org/docs/tutorials/koans.html)

Vaadin lets you forget the web and program user interfaces much like you would program a desktop application with conventional Java toolkits such as AWT, Swing, or SWT. But easier.
To learn Vaadin:

* [Official Vaadin website](https://www.vaadin.com)
* [Vaadin 10 Documentation](https://vaadin.com/docs/v10/flow/Overview.html) - we recommend to download and read the Vaadin Book PDF.

> **Note:** If you have no prior experience with Kotlin nor Vaadin, you might get overwhelmed by the sheer amount of 
the new stuff we will learn. Therefore, we recommend to take slow steps and get familiar with both Vaadin and Kotlin first. 
Feel free to experiment on the [Karibu-DSL Hello World Example](https://github.com/mvysny/karibu10-helloworld-application) at any time:
VoK basically uses Karibu-DSL under the hood, therefore the lessons learned in the Karibu-DSL Hello World example will
be applicable in the VoK-based apps later on.

## 2 What is Vaadin-on-Kotlin?

VoK is a glue between Vaadin, Kotlin and other frameworks which allows you to write web apps smoothly. 
It is designed to make the art of programming of web applications easier by making assumptions about what 
every developer needs to get started. It allows you to write less code while accomplishing 
more than many other languages and frameworks.

> **Note:** Traditionally both JavaEE and Spring acted as this "glue" which held various frameworks together.
But, with the advent of the Kotlin programming language,
we believe that the features of the Kotlin programming language alone are all that's necessary in the modern programming.
We believe that Kotlin can replace the traditional approach of using the Dependency Injection to glue stuff together.

VoK is opinionated software. It makes the assumption that there is a "best" way to do things,
and it's designed to encourage that way - and in some cases to discourage alternatives.

The VoK philosophy includes three major guiding principles:

* Simplicity - things are kept as simple as possible, and libraries are used only when absolutely necessary. Complex patterns such as Dependency Injection
  and MVC are deliberately left out.
* Components as basic building blocks - Vaadin is a single-page web component framework as opposed to
  the traditional multiple page frameworks. As such, it resembles the traditional fat client
  Swing/JavaFX programming and is closer to GUI software development than traditional web development with HTML and JavaScript.
  VoK promotes code/UI reuse by means of reusing components (your components will range from basic ones
  to a complex containers, even forms) instead of creating page templates.
* No magic - No proxies, interceptors, reflection. VoK introduces explicit functions which you can easily
  browse for sources in your Intellij IDEA.

While the Dependency Injection (DI) itself is not hard to grok, it comes with unfortunate consequences:
* The DI forces the programmer to create Services/DAOs even for tiny CRUD operations. While having Services may be a desirable
practice in larger project, it is overkill for simple projects.
* The DI requires you to run on a DI container, such as a JavaEE server, or tons of Spring libraries. While that's nothing
  new for a seasoned Java developer, this is overwhelming for a newbie which is just starting with the web app development.
* It quickly tends to get very complex as the DI configuration grows.

Therefore, VoK itself is not using DI; you can of course use Spring or JavaEE in your project alongside VoK if necessary.

> **Note on MVC**: The [Model-View-Controller](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) pattern
is very popular with page-oriented frameworks such as Ruby on Rails, Groovy on Grails and Python Django. There is typically
lot of things going on in the page, and the MVC pattern helps keeping the code separated into smaller,
more easily digestable packages. 
>
> However, since Components are a much smaller unit of reuse than pages, employing MVC with Component-oriented frameworks
does not make that much sense: for example it will usually leave you with nearly empty Views. We thus believe that using MVC does 
more harm than good since it adds unnecessary complexity. Therefore this tutorial will not use MVC.

## 3 Creating a New VoK Project
The best way to read this guide is to follow it step by step. All steps are essential to run this example application and no additional code or steps are needed.

By following along with this guide, you'll create a VoK project called blog, a (very) simple weblog.
Before you can start building the application, you need to make sure that you have Java 8 JDK installed.

### 3.1 Prerequisites

Vaadin-on-Kotlin only requires Java 8 JDK to be installed. The example application has Gradle bundled in;
Gradle will then download everything else (Vaadin, Kotlin, libraries, the Jetty server which is used to run the app from the command line).
This makes VoK applications really portable
since they work flawlessly on any OS and CPU which supports Java 8 - be it Windows, Linux or Mac, on x86, ARM or others.

The example application also uses an embedded Java database called [H2](http://www.h2database.com/html/main.html), so there
is no need for you to set up any database.

While it is possible to edit the project files using any text editor, we recommend to install Intellij IDEA which provides awesome
Kotlin support including auto-completion. You can use IDEA Community edition, which is free and allows you to run
gradle tasks to run the app, or you can purchase the Ultimate edition which also supports debugging/hot-redeployment of the web app on Tomcat and other
servers, and also offers awesome database integration tools.

### 3.2 Creating the Blog Application<a name="3_2"></a>

Getting the example application is really easy. If you have Git installed, just open a command line and run the
following command:

```bash
$ git clone https://github.com/mvysny/vok-helloworld-app-v10
```
If you don't have Git, you can simply download the application as a zip file from GitHub: [https://github.com/mvysny/vok-helloworld-app-v10/archive/master.zip](https://github.com/mvysny/vok-helloworld-app-v10/archive/master.zip).

After you create the blog application, switch to its folder:

```bash
$ cd vok-helloworld-app-v10
$ ./gradlew
```

This will download everything necessary and will compile the example application's WAR file.

> **Note:** WAR (Web ARchive) is Java standard for packaging web applications. WAR file is a zip file which
can be deployed to all Java Servlet Servers, including Tomcat, JBoss etc.

The `vok-helloworld-app-v10` directory has a number of files and folders that make up the
structure of a VoK application. Most of the work in this tutorial will happen in the
`src/main/kotlin` folder, but here's a basic rundown on the function of each of the files and 
folders:

| File/Folder       | Purpose                                 |
|-------------------|-----------------------------------------|
| `web/src/main/kotlin` | Contains the source files of all of your views, Servlets, REST endpoints, async jobs for your application. You'll focus on this folder for the remainder of this guide. |
| `web/src/main/webapp` | Contains the 'Chuck Norris' image file
| `web/src/main/resources` | Contains the logger configuration file (`logback.xml`) |
| `build.gradle`    | This file defines tasks that can be run from the command line. You should add your own tasks by adding code to this file. There is much you can do with Gradle - you can for example use the ssh plugin to deploy the WAR to your production environment. |
| `README.md`       | This is a brief instruction manual for your application. You should edit this file to tell others what your application does, how to set it up, and so on. |
| .gitignore        | This file tells git which files (or patterns) it should ignore. See [Github - Ignoring files](https://help.github.com/articles/ignoring-files/) for more info about ignoring files. |

## 4 Hello, Vaadin-on-Kotlin!<a name="4"></a>

To begin with, let's get some text up on screen quickly. To do this, you need to get a web server running.

### 4.1 Starting up the Web Server

You actually have a functional VoK application already. To see it, you need to start a web server on your development machine.
You can do this by running the following in the `vok-helloworld-app-v10` directory:

```bash
$ ./gradlew clean web:appRun
```

This will fire up Jetty, an embeddable Java web server. To see your application in action, open a browser window and navigate
 to [http://localhost:8080](http://localhost:8080). You should see the Vaadin-on-Kotlin default information page:

![Welcome VoK](images/welcome_vok_v10.png)

> **Note:** To stop the web server, hit Ctrl+C in the terminal window where it's running. To verify the server has stopped
you should see your command prompt cursor again. For most UNIX-like systems including macOS this will be a dollar sign $.

> **Note:** Changes made in your Kotlin files will be propagated to the running server only after you compile them, by
 running `./gradlew build`.

The "Welcome aboard" page is the smoke test for a new VoK application: it makes sure that you
have your software configured correctly enough to serve a page.

### 4.2 Say "Hello", Vaadin

To get VoK saying "Hello", you need to create a route.

A route's purpose is to provide a Vaadin Component (usually a layout containing other components), which then interacts with the user.
The Route Registry decides which view receives which requests. Usually there is exactly one route to a view. You can collect the data
to be displayed right in the view itself (for small applications), or you can define so-called Service layer
(a group of regular Kotlin classes which define a clear API and are responsible for fetching of the data).
VoK however does not enforce this, and we will not use this pattern in the tutorial.

All Vaadin Components have two parts:

* Their JavaScript-based client side Web Component which you usually can not use directly;
* A server side class which you access from your code.

For example, a Button Web Component ([`vaadin-button`](https://vaadin.com/components/vaadin-button)) contains the logic to send the notification about the mouse click
to all JavaScript listeners; server-side `Button` registers the JavaScript listener, transfers the event server-side and allows you to register server-side listeners which listen for button clicks.

Another example: [`vaadin-grid`](https://vaadin.com/components/vaadin-grid) shows a list of data in tabular fashion; it performs scrolling and fetching of the data as the user scrolls, via a data provider.
Server-side `Grid` allows you to set the `DataProvider` which will actually fetch the data, from the database or from anywhere, depending on how you implement it.

To create a new view, all that's needed is to create a Kotlin class which is annotated with the `@Route` annotation and extends
some Vaadin Component.

Create the `web/src/main/kotlin/com/example/vok/MyWelcomeView.kt` file and make sure it looks like follows:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("")
class MyWelcomeView: VerticalLayout() {
    init {
        h1("Hello, Vaadin-on-Kotlin!")
    }
}
```

### 4.3 Setting the Application Home Page
Now that we have made the view, we need to tell VoK when we want "Hello, Vaadin-on-Kotlin!"
to show up. In our case, we want it to show up when we navigate to the root URL of our site,
[http://localhost:8080](http://localhost:8080). At the moment, "Welcome aboard" is occupying that spot.

Open up the `WelcomeView.kt` file and change the `@Route("")` annotation to the following:
`@Route("old-welcome")`. This will map the original "Welcome aboard" page to
[http://localhost:8080/old-welcome](http://localhost:8080/old-welcome) , making space for our new Hello page.

Having the `@Route("")` on `MyWelcomeView` will tell the Vaadin Route Registry to map requests to the root of the application to the `MyWelcomeView` view.

Launch the web server again and navigate to [http://localhost:8080](http://localhost:8080) in your browser. You'll see the "Hello, Vaadin-on-Kotlin!"
message you put into the `web/src/main/kotlin/com/example/vok/MyWelcomeView.kt`, indicating
that this new route is indeed going to `MyWelcomeView` and is rendering the view correctly.

## 5 Getting Up and Running

Now that you've seen how to create a view, let's create something with a bit more substance.

In the Blog application, you will now create a new database table, or in REST terms, a resource. A resource is the term used
for a collection of similar objects, such as articles, people or animals. You can create,
read, update and destroy items for a resource and these operations are referred to as
CRUD operations.

VoK provides a resources method which can be used to declare a standard REST resource. But first, let us define the article.
Create the `web/src/main/kotlin/com/example/vok/Article.kt` file with the following contents:

```kotlin
package com.example.vok

import com.github.vokorm.*

data class Article(
        override var id: Long? = null,

        var title: String? = null,

        var text: String? = null
) : Entity<Long> {
    companion object : Dao<Article>
}
```

This will define a so-called entity class, which basically represents a row in the "Article" database table.

We can now implement the REST endpoint for REST clients to access the article resource.

> **Note:** This step is completely optional and is actually not used by Vaadin, since
Vaadin Flow uses its own internal JSON protocol to communicate with components.
Having REST may come handy though, since we can use it to examine the state of the database
(using the `curl` or `wget` tools).

Just create a file `web/src/main/kotlin/com/example/vok/ArticleRest.kt` which will look as follows:

```kotlin
package com.example.vok

import com.github.vokorm.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/articles")
class ArticleRest {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: Long): Article = Article.findById(id) ?: throw NotFoundException("No article with id $id")

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): List<Article> = Article.findAll()
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
select * from Article [42102-196]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:345)
	at org.h2.message.DbException.get(DbException.java:179)
	at org.h2.message.DbException.get(DbException.java:155)
```

That is to be expected since we haven't yet created the table for Articles. We'll do that in a minute.
In the next section, you will add the ability to create new articles in your application and be able to view them. This is the "C" and the "R" from CRUD: create and read. The form for doing this will look like this:

