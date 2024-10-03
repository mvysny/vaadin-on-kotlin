[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-v10-vokdb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-v10-vokdb)

# VoK Vaadin and SQL database support

This module includes:
 
* The [vok-orm](https://github.com/mvysny/vok-orm) project which includes SQL database support,
* The [jdbi-orm-vaadin](https://gitlab.com/mvysny/jdbi-orm-vaadin) project which provides several
  utility methods to provide nice integration of Vaadin with the SQL database, namely the
  Grid filters and Grid DataProvider.

To use this module in your app just add the following dependency into your `build.gradle` file:

```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-framework-vokdb:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

This module provides:

* `Dao.dataProvider` which returns `EntityDataProvider` for an entity. You usually
   make KEntity's companion object extend the Dao; if you do, this extension property will
   allow you to obtain the `DataProvider` for an KEntity simply by calling `Person.dataProvider`.
* `EntityToIdConverter` which is useful when binding a `ComboBox<Person>` to a `Long` field which only contains the person ID.

## When to use this module

Use this module if you intend to build a Vaadin-based app which accesses your SQL database.

> Note: There is no support for Vaadin + JPA in VoK.

If you plan to use NoSQL database or some other form of data fetching, then only use the
[vok-util-vaadin](../vok-util-vaadin) module. You will then have to write your own data fetching
layer; however you may find inspiration on how to do so, by checking the sources of this module.

## Connecting to the SQL database

First you will need to add the JDBC driver for your database as a dependency to your `build.gradle.kts` file.
Then, you'll need to add Hikari-CP as a dependency (unless you're running on Spring/JavaEE):

```groovy
implementation("com.zaxxer:HikariCP:5.0.1")
```

After that's done, you need to configure the `vok-db` database JDBC provider.
The following example configures an in-memory H2 database:

```kotlin
val config = HikariConfig().apply {
    driverClassName = Driver::class.java.name  // the org.h2.Driver class
    jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    username = "sa"
    password = ""
}
VaadinOnKotlin.dataSource = HikariDataSource(config)
```

It is a good practice to run the configuration from your `ServletContextListener`. You can find a proper
example in the Beverage Buddy's [Bootstrap](https://github.com/mvysny/beverage-buddy-vok/blob/master/src/main/kotlin/com/vaadin/starter/beveragebuddy/Bootstrap.kt) class.

Don't forget to close the data source and clean up resources properly, in your `ServletContextListener.contextDestroyed()` function:

```kotlin
JdbiOrm.destroy()
```

## Database Migrations

It is recommended to run database migrations from your `ServletContextListener` as well, to make sure that
the database structure is up-to-date.

This module does not provide any support for migrations directly, however adding the support for migrations
to your app is extremely easy.

The easiest way is to use [Flyway](https://flywaydb.org/). Just add the following to your `build.gradle` file:

```groovy
dependencies {
    implementation("org.flywaydb:flyway-core:9.22.3")
}
```

Now we can add SQL scripts to your `src/main/resources/db/migration` folder; simply create a file named
`V01__CreateCategory.sql` in there, with the following contents:

```sql92
create TABLE CATEGORY (
  id bigint auto_increment PRIMARY KEY,
  name varchar(200) NOT NULL,
);
create UNIQUE INDEX idx_category_name ON CATEGORY(name);
```

Please find more documentation in the [Flyway Migration Guide](https://flywaydb.org/documentation/migrations#naming).

To actually run the migration, you will need to call the `Flyway` class, ideally from your `ServletContextListener`
after VoK is initialized:

```kotlin
val flyway = Flyway.configure()
    .dataSource(VaadinOnKotlin.dataSource)
    .load()
flyway.migrate()
```

Please see the Beverage Buddy's [Bootstrap](https://github.com/mvysny/beverage-buddy-vok/blob/master/src/main/kotlin/com/vaadin/starter/beveragebuddy/Bootstrap.kt)
for more details.

## More Information

* The [Accessing Database Guide](https://www.vaadinonkotlin.eu/databases.html)
* The [Using Grids Guide](https://www.vaadinonkotlin.eu/grids.html)
