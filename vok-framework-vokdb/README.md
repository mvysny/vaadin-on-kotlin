[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://img.shields.io/maven-central/v/eu.vaadinonkotlin/vok-framework-vokdb)](https://central.sonatype.com/artifact/eu.vaadinonkotlin/vok-framework-vokdb)

# VoK Vaadin and SQL database support

This module brings in:

* [ktorm](https://www.ktorm.org/) for SQL access — typed-SQL DSL, entity sequences, JDBC pooling friendly.
* [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) for the Vaadin integration — `Table<E>.dataProvider`,
  `EntityDataProvider` / `QueryDataProvider` backed by ktorm, filter components (`FilterTextField`,
  `DateRangePopup`, `NumberRangePopup`, `BooleanFilterField`, `EnumFilterField`), Binder helpers, and the
  `ActiveEntity` / `db { … }` runtime.

To use this module in your app:

```kotlin
dependencies {
    implementation("eu.vaadinonkotlin:vok-framework-vokdb:x.y.z")
}
```

> Note: see the [latest release tag](https://github.com/mvysny/vaadin-on-kotlin/tags) for `x.y.z`.

On top of ktorm-vaadin, this module adds:

* `VaadinOnKotlin.dataSource` extension — assign your `HikariDataSource` here and ktorm's global `ActiveKtorm.database`
  is wired automatically. This is the canonical way to bootstrap the persistence layer in a VoK app.
* `Binder.BindingBuilder.toId(idColumn)` — Vaadin Binder helper for editing a ComboBox of entities while the bound
  model field stores the foreign-key id.
* `enumFilterField<E>()` — small reified-generic factory for ktorm-vaadin's `EnumFilterField`.

## When to use this module

Use this module if you intend to build a Vaadin-based app that talks to an SQL database. There is no support for
Vaadin + JPA in VoK; if you need a NoSQL backend or some other data layer, depend only on `vok-framework` and write
your own.

## Connecting to the SQL database

Add the JDBC driver for your database and Hikari-CP to your `build.gradle.kts`:

```kotlin
implementation("com.zaxxer:HikariCP:7.0.2")
runtimeOnly("com.h2database:h2:2.2.224")
```

Then in your `ServletContextListener` (or equivalent boot hook):

```kotlin
val config = HikariConfig().apply {
    driverClassName = Driver::class.java.name  // org.h2.Driver
    jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    username = "sa"
    password = ""
}
VaadinOnKotlin.dataSource = HikariDataSource(config)  // also sets ActiveKtorm.database
```

There is no separate ktorm-destroy step on shutdown — closing the HikariDataSource (or letting the JVM exit) is
sufficient.

See [Bootstrap.kt](../vok-example-crud/src/main/kotlin/example/crudflow/Bootstrap.kt) in the demo for the full
pattern (plus Flyway migrations).

## Defining entities and tables

Define each entity as an `Entity<E>` interface (with `ActiveEntity<E>` if you want instance-level `save()` /
`create()` / `delete()`), and a `Table<E>` object alongside it:

```kotlin
interface Person : ActiveEntity<Person> {
    var id: Long?
    var name: String?
    var age: Int?
    override val table: Table<Person> get() = Persons
    companion object : Entity.Factory<Person>()
}

object Persons : Table<Person>("Person") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val age = int("age").bindTo { it.age }
}
```

Then `Persons.dataProvider` is a `EntityDataProvider<Person>` you can hand to a Vaadin Grid, `Person { name = "Foo"
}.create()` inserts a row, `db { database.sequenceOf(Persons).find { it.id eq 1L } }` reads, etc.

## Database migrations

Use [Flyway](https://flywaydb.org/). Add the dependency, drop `.sql` migrations into
`src/main/resources/db/migration`, then run after wiring `VaadinOnKotlin.dataSource`:

```kotlin
Flyway.configure().dataSource(VaadinOnKotlin.dataSource).load().migrate()
```

## More information

* [ktorm docs](https://www.ktorm.org/) — schema, queries, entity sequences.
* [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) — filter components, DataProviders, Binder helpers.
* [beverage-buddy-ktorm](https://github.com/mvysny/beverage-buddy-ktorm) — canonical end-to-end example.
