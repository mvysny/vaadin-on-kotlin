# VoK-CRUD Example application

A simple demo of a full-stack example application which you can inspire from.
You can launch the app simply from your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:run
```

The web app will be running at [http://localhost:8080](http://localhost:8080).

Please see the [Vaadin Boot](https://github.com/mvysny/vaadin-boot#preparing-environment) documentation
on how you run, develop and package this Vaadin-Boot-based app.

## Behind The Scenes

The application demonstrates the following things:

* Linking to a database. VaadinOnKotlin uses [ktorm](https://www.ktorm.org/) for O/R mapping, with a thin
  Vaadin integration layer from [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin).
  The example project is using an in-memory H2 database, so no additional setup is necessary. See
  [build.gradle.kts](build.gradle.kts) for the dependencies.
  To link to the database, we configure a Hikari connection pool in [Bootstrap.kt](src/main/kotlin/example/crudflow/Bootstrap.kt).
* Preparing the database: Flyway migrations run every time before the app starts, ensuring the schema is up to date.
  See [Bootstrap.kt](src/main/kotlin/example/crudflow/Bootstrap.kt) and the
  [sample migrations](src/main/resources/db/migration). More details in the [Flyway DB Migration Guide](https://flywaydb.org/documentation/migration/sql).
* Accessing the database: declare your entities as ktorm `Entity<E>` interfaces — see
  [(example Person)](src/main/kotlin/example/crudflow/person/Person.kt) — and run queries inside `db { … }` blocks,
  e.g. `val allPersons = db { database.sequenceOf(Persons).toList() }`. The `db { }` wrapper is from `ktorm-vaadin`
  and opens (or joins) a transaction with the singleton `ActiveKtorm.database`. No DI/EJBs required.
* Serving the data via REST: pulls in [vok-rest](../vok-rest). Wire a Javalin servlet and register a CRUD handler
  via `Table.getCrudHandler()` — see [PersonRest](src/main/kotlin/example/crudflow/PersonRest.kt).
* Creating the UI: declare your view and populate it with Karibu-DSL components. See
  [PersonListView](src/main/kotlin/example/crudflow/person/PersonListView.kt) for filter components and grid wiring.
* Logging: SLF4j + slf4j-simple, configured in [simplelogger.properties](src/main/resources/simplelogger.properties).
* Running: standard WAR-style app embedded in Jetty via Vaadin Boot; run from your IDE or `./gradlew vok-example-crud:run`.
* Testing: uses the [Karibu-Testing](https://github.com/mvysny/karibu-testing) framework; see
  [PersonListViewTest.kt](src/test/kotlin/example/crudflow/person/PersonListViewTest.kt) for the canonical pattern.
