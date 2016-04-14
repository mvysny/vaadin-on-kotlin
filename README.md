# pokusy_kotlinee

Lot of projects do not use all capabilities of JavaEE, but only a subset: the Async, the database, and that's it.
This project is an attempt to simplify such projects to a pure servlet environment, with no JavaEE stuff, no injections, etc.
Uses Kotlin. Currently starts its own embedded H2 database. I am trying to simplify this as much as possible, so I will not
add any injection support etc. Basically, what I'm trying to do is a very simple Vaadin-based project with async/push support
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

Ignored:
* Cluster-wide singleton
* Messaging
* Security
* Injections

## To run the WAR outside of any IDE:
* Download Jetty Runner
* Run ./gradlew
* Locate the WAR in build/libs/
* java -jar jetty-runner*.jar *.war

## To develop in IDEA:
* Open the project
* Launch in Jetty (you will need to download and unpack Jetty Distribution zip file)
