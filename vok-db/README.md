[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-db/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-db)

# VoK database support

The recommended way to use the SQL databases. It includes the [VoK-ORM](https://github.com/mvysny/vok-orm)
library to perform data fetching; see the VoK-ORM home page for examples and documentation on how
exactly the data fetching works. It also uses [Hikari-CP](https://brettwooldridge.github.io/HikariCP/)
as a production-grade JDBC connection pooling.

To use this module, simply add the following to your `build.gradle` file:

```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-db:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

## When to use this module

Use this module if you wish to use the recommended way to access the SQL database.

If you want to use JPA, then use this module instead: [vok-framework-jpa](../vok-framework-jpa).

If you want to use NoSQL, then you will have to implement such module tailored towards your
particular NoSQL database yourself; you can consult the sources of this module for hints on how
to tie to VoK init/destroy lifecycle and how to configure the client.

## Connecting to the SQL database

First you will need to add the JDBC driver for your database as a dependency to your `build.gradle.kts` file.
Then, you'll need to add Hikari-CP as a dependency (unless you're running on Spring/JavaEE):

```groovy
compile("com.zaxxer:HikariCP:3.4.1")
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

## Database Migrations

It is recommended to run database migrations from your `ServletContextListener` as well, to make sure that
the database structure is up-to-date.

This module does not provide any support for migrations directly, however adding the support for migrations
to your app is extremely easy.

The easiest way is to use [Flyway](https://flywaydb.org/). Just add the following to your `build.gradle` file:

```groovy
dependencies {
    compile "org.flywaydb:flyway-core:6.0.8"
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
