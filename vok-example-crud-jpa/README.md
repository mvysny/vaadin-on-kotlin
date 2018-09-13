# Sample application which uses JPA

If you prefer the evil you are accustomed to, then you can start off this example. The application demonstrates the following things:

* Linking to a database. Vaadin-on-Kotlin encourages you to use the [vok-orm](../vok-example-crud-sql2o)
  approach but you can also use the alternative approach of Hibernate for JPA O/R mapping when accessing the database.
  The example project is simply using an in-memory H2 database, so that no additional setup is necessary. See 
  [build.gradle](build.gradle) the db section for more details.
  To link to the database, we use the traditional JPA
  [persistence.xml](src/main/resources/META-INF/persistence.xml). Please note that HikariCP is used for DB
  connection pooling, which provides production-grade performance.
* Preparing the database: simply run Flyway migration every time before the app is started, to make sure
  that the app has newest database ready.
  The migration is safe on cluster as well as a database lock is obtained.
  Please see [Bootstrap.kt](src/main/kotlin/com/github/vok/example/crud/Bootstrap.kt)
  You will need to write the database migration scripts yourself: see [sample migrations](src/main/resources/db/migration) for details. More details
  in the [Flyway Migration Guide](https://flywaydb.org/documentation/migration/sql)
* Accessing the database: just create your JPA beans
  [(example Person)](src/main/kotlin/com/github/vok/example/crud/personeditor/Person.kt) and use them in any way you see fit:
  `val allPersons = db { em.findAll<Person>() }`. The `db` is just a function defined
  in the JPA module in [DB.kt](../vok-framework-jpa/src/main/kotlin/com/github/vok/framework/DB.kt), you can call this from anywhere, be it
  Vaadin click listener or background thread. No injections/beans/EJBs/whatever necessary!
