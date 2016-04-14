# pokusy_kotlinee

Lots of projects actually do not use all capabilities of JavaEE, just a subset of JavaEE features: mostly the database access of course,
the Async, and that's it.

This project is an attempt to simplify such projects:

* Allow them to run in a pure servlet environment (such as Jetty, Tomcat)
* Remove complex stuff such as injections, SLSBs, SFSBs
* Allow any object to be bound to a session (e.g. caches) in a simple manner

Uses Kotlin. Currently starts its own embedded H2 database. Basically, what I'm trying to do is a very simple Vaadin-based project with async/push support
and database support - a very simple but powerful quickstart project.

## Motivation

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
transactions), manually store it into the session and then run into abovementioned issues with websocket xhr. What the fuck? I want to focus on coding,
not @configuring the world until JavaEE is satisfied.

## Status

This is just a prototype project. A real-world app needs to be built on top of this, to see how well this quasi-framework will fare.

Done:

* JPA (via Hibernate) and transactions (via transaction {})
* Migrations (Flyway) - the migrations are run automatically when the WAR is started.
* Vaadin with JPAContainer
* Async tasks & Vaadin Push
* Drop-in replacements for SFSBs bound to session: see LastAddedPersonCache.kt for details.
* REST+JSON (via RESTEasy); see `PersonRest.kt` for details.
* Vaadin UI builder - see `MyUI.kt` for details.

Todo:

* Configure JDBC connection pooling
* Decrease logging verbosity ;)

Ignored:

* Cluster-wide singleton
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
* In IDEA, add Jetty Server Local launcher, specify the path to the Jetty Distribution directory and attach the WAR-exploded artefact to the runner
* Run or Debug the runner

### Tomcat

* Open the project in IDEA
* https://kotlinlang.org/docs/tutorials/httpservlets.html
