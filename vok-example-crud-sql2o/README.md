[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Heroku](https://heroku-badge.herokuapp.com/?app=vok-crud&style=flat&svg=1)](https://vok-crud.herokuapp.com/)

# VoK-CRUD Example application

A more complete full-stack example application which you can inspire from. You can launch the app simply from your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud-sql2o:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080).

You can find the [VoK-CRUD Live Demo](https://vok-crud.herokuapp.com/) running on Heroku.

The app is a standard WAR application. Just import the whole vaadin-on-kotlin project directly into your IDE, then launch this app
as a WAR application in the servlet container of your choice.

## Behind The Scenes

The application demonstrates the following things:

* Linking to a database. VaadinOnKotlin uses [vok-orm](https://github.com/mvysny/vok-orm) for simple O/R mapping when accessing the database.
  The example project is simply using an in-memory H2 database, so that no additional setup is necessary. See 
  [build.gradle](build.gradle) the *db* section for more details.
  To link to the database, we configure Hikari database connection pooler in [Bootstrap.kt](src/main/kotlin/example/crud_sql2o/Bootstrap.kt). HikariCP provides production-grade performance.
  You can also use JPA if you so wish - see [vok-example-crud-jpa](../vok-example-crud-jpa) for details.
* Preparing the database: simply run Flyway migration every time before the app is started, to make sure that the app has newest database ready.
  The migration is safe on cluster as well as a database lock is obtained.
  Please see [Bootstrap.kt](src/main/kotlin/example/crud_sql2o/Bootstrap.kt)
  You will need to write the database migration scripts yourself: see [sample migrations](src/main/resources/db/migration) for details.
  More details in the [Flyway DB Migration Guide](https://flywaydb.org/documentation/migration/sql)
* Accessing the database: just create your pojo beans [(example Person)](src/main/kotlin/example/crud_sql2o/personeditor/Person.kt)
  and use them in any way you see fit:
  `val allPersons = db { Person.findAll() }`. The `db` is just a function defined in the [vok-orm framework](https://github.com/mvysny/vok-orm).
  You can call the `db{}` method from anywhere, be it Vaadin click listener or background thread.
  No injections/beans/EJBs/whatever necessary! See the [vok-orm documentation](https://github.com/mvysny/vok-orm) for more details.
* Serving the data via REST: add [vok-rest](../vok-rest) to your project, see [build.gradle](build.gradle). Then, declare REST Application to bind
  the REST to a particular URL endpoint, see
  [Bootstrap.kt](src/main/kotlin/example/crud_sql2o/Bootstrap.kt)
  the `@ApplicationPath("/rest")` stanza. After that, just define your REST-accessing classes, for example
  [PersonRest](src/main/kotlin/example/crud_sql2o/PersonRest.kt)
* Creating the UI: there are lots of great Vaadin tutorials, in general you declare UI and populate it with components. See
  [MyUI](src/main/kotlin/example/crud_sql2o/MyUI.kt)
* Create Update Delete (CRUD): no Scaffolding-like UI generator for now, but you can see the
  [crud example](src/main/kotlin/example/crud_sql2o/personeditor) on how to write the CRUD UI yourself very easily.
* Logging: uses SLF4j with Logback, configured as follows: [logback.xml](src/main/resources/logback.xml)
* Session-stored cache which of course can access database anytime: see [LastAddedPersonCache.kt](src/main/kotlin/example/crud_sql2o/LastAddedPersonCache.kt).
* Running: this app is a standard WAR application which you can run from your IDE directly.
* Testing: uses the [Karibu-Testing](https://github.com/mvysny/karibu-testing) framework; please find the example test at [CrudViewTest.kt](src/test/kotlin/example/crud_sql2o/personeditor/CrudViewTest.kt).
