# pokusy_kotlinee

Lots of projects actually do not use all capabilities of JavaEE, just a subset of JavaEE features: mostly the database access of course,
the Async, and that's it.

This project is an attempt to simplify such projects:

* Allow them to run in a pure servlet environment (Jetty)
* Remove complex stuff such as injections, SLSBs, SFSBs
* Allow any object to be bound to a session (e.g. caches) in a simple manner

Uses Kotlin. Currently starts its own embedded H2 database. Basically, what I'm trying to do is a very simple Vaadin-based project with async/push support
and database support - a very simple but powerful quickstart project.

## Motivation

I implemented a Vaadin-based JavaEE project and was constantly plagued with:

* Crashes in TomEE when deploying Kotlin-based SLSBs: https://issues.apache.org/jira/browse/TOMEE-1774
* Crashes when accessing @SessionScoped beans from websocket xhr code - https://vaadin.com/forum#!/thread/11474306 ; @NormalUIScoped produces
org.apache.openejb.core.stateful.StatefulContainer$StatefulCacheListener timedOut and javax.ejb.NoSuchEJBException - you need to add
@StatefulTimeout(-1) everywhere and use @PreserveOnRefresh on your UI - stupid.
* Moronic async API: when async method is called directly from another method in that very SLSB, the method is actually silently
called synchronously. To call async, you need to inject self and call the method as `self.method()`
* You cannot inject stuff into Vaadin components, you can make Vaadin component a managed bean but that's just plain weird. How about
having a global val with a getter which produces the correct bean instance on demand?

## Status

Done:
* JPA (via Hibernate) and transactions (via transaction {})
* Migrations (Flyway) - the migrations are run automatically when the WAR is started.
* Vaadin with JPAContainer
* Async tasks & Vaadin Push
* Drop-in replacements for SFSBs bound to session: see LastAddedPersonCache.kt for details.

Todo:
* Configure JDBC connection pooling
* REST
* Vaadin ui builder

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
* Open the project
* Download the Jetty Distribution zip file from here: http://download.eclipse.org/jetty/stable-9/dist/
* Unpack the Jetty Distribution
* In IDEA, add Jetty Server Local launcher, specify the path to the Jetty Distribution directory and attach the WAR-exploded artefact to the runner
* Run or Debug the runner

