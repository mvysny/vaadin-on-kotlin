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

* Linking to a database. VaadinOnKotlin uses [vok-orm](https://github.com/mvysny/vok-orm) for simple O/R mapping when accessing the database.
  The example project is simply using an in-memory H2 database, so that no additional setup is necessary. See 
  [build.gradle](build.gradle) the *db* section for more details.
  To link to the database, we configure Hikari database connection pooler in [Bootstrap.kt](src/main/kotlin/example/crudflow/Bootstrap.kt).
  HikariCP provides production-grade performance.
* Preparing the database: simply run Flyway migration every time before the app is started, to make sure that the app has newest database ready.
  The migration is safe on cluster as well as a database lock is obtained.
  Please see [Bootstrap.kt](src/main/kotlin/example/crudflow/Bootstrap.kt)
  You will need to write the database migration scripts yourself: see [sample migrations](src/main/resources/db/migration) for details.
  More details in the [Flyway DB Migration Guide](https://flywaydb.org/documentation/migration/sql)
* Accessing the database: just create your pojo beans [(example Person)](src/main/kotlin/example/crudflow/person/Person.kt)
  and use them in any way you see fit:
  `val allPersons = db { Person.findAll() }`. The `db` is just a function defined in the [vok-orm framework](https://github.com/mvysny/vok-orm).
  You can call the `db{}` method from anywhere, be it Vaadin click listener or background thread.
  No injections/beans/EJBs/whatever necessary! See the `vok-orm` documentation for more details.
* Serving the data via REST: add [vok-rest](../vok-rest) to your project, see [build.gradle](build.gradle). Then, declare REST Application to bind
  the REST to a particular URL endpoint, see
  [Bootstrap.kt](src/main/kotlin/example/crudflow/Bootstrap.kt)
  the `@ApplicationPath("/rest")` stanza. After that, just define your REST-accessing classes, for example
  [PersonRest](src/main/kotlin/example/crudflow/PersonRest.kt)
* Creating the UI: there are lots of great Vaadin tutorials, in general you declare your view and populate it with components. See
  [PersonListView](src/main/kotlin/example/crudflow/person/PersonListView.kt)
* Create Update Delete (CRUD): no Scaffolding-like UI generator for now, but you can see the
  [crud example](src/main/kotlin/example/crudflow/person) on how to write the CRUD UI yourself very easily.
* Logging: uses SLF4j with slf4j-simple, configured as follows: [simplelogger.properties](src/main/resources/simplelogger.properties)
* Running: this app is a standard WAR application which you can run from your IDE directly.
* Testing: uses the [Karibu-Testing](https://github.com/mvysny/karibu-testing) framework; please find the example test at [PersonListViewTest.kt](src/test/kotlin/example/crudflow/person/PersonListViewTest.kt).
