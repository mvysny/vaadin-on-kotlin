# pokusy_kotlinee

An attempt to simplify javaee-based projects which only use a subset of javaee, to a pure jetty environment.
Uses Kotlin. Currently starts its own embedded H2 database. I am trying to simplify this as much as possible, so I will not
add any injection support.

Done:
* JPA (via Hibernate) and transactions (via transaction {})
* Migrations (Flyway) - the migrations are run automatically when the WAR is started.
* Vaadin with JPAContainer

Todo:
* Async tasks & Vaadin Push
* Drop-in replacements for SFSBs
* Configure JDBC connection pooling

Ignored:
* Cluster-wide singleton
* Messaging
* Security
* Injections

## To run the WAR:
* Download Jetty Runner
* Run ./gradlew
* Locate the WAR in build/libs/
* java -jar jetty-runner*.jar *.war

## To develop in IDEA:
* Open the project
* Launch in Jetty (you will need to download and unpack Jetty Distribution zip file)
