[![Powered By Vaadin on Kotlin](https://www.vaadinonkotlin.eu/iconography/vok_badge.svg)](https://www.vaadinonkotlin.eu)
[![Join the chat at https://gitter.im/vaadin/vaadin-on-kotlin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/vaadin-on-kotlin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework)
[![Build Status](https://github.com/mvysny/vaadin-on-kotlin/actions/workflows/gradle.yml/badge.svg)](https://github.com/mvysny/vaadin-on-kotlin/actions/workflows/gradle.yml)

# Welcome to Vaadin-on-Kotlin

> **0.19 is a breaking release.** The persistence layer moved from `vok-orm` (jdbi-orm) to
> [ktorm](https://www.ktorm.org/) + [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin). The published REST
> wire format also simplified to eq-only filters with `?offset` / `?limit` / `?sort=col:asc,col2:desc`. If you're
> upgrading from 0.18.x, check the per-module READMEs for the new shapes and see [vok-example-crud](vok-example-crud)
> for an end-to-end example.

Vaadin-on-Kotlin is a web-application framework for database-backed apps in Kotlin. Documentation lives at
[www.vaadinonkotlin.eu](https://www.vaadinonkotlin.eu) (some pages still describe pre-0.19 idioms — refer to module
READMEs and the demo for current code).

VoK does not enforce MVC, dependency injection, or service-oriented architecture. It uses neither Spring nor JavaEE
by default. The view layer leverages [Vaadin](https://vaadin.com)'s component-oriented programming model. The
persistence layer uses [ktorm](https://www.ktorm.org/) — typed-SQL DSL, entity sequences, no XML — wrapped by
[ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) for `EntityDataProvider`, filter components, and the
`ActiveEntity` runtime. Of course you can swap in JPA/Hibernate or a NoSQL store instead by depending only on
`vok-framework`.

## Getting started

1. Install JDK 21 (required by Vaadin 25) and a git client if you don't already have them.

2. Clone the example app and run it:

    ```bash
    git clone https://github.com/mvysny/vaadin-on-kotlin
    cd vaadin-on-kotlin
    ./gradlew vok-example-crud:run
    ```

3. Visit [http://localhost:8080](http://localhost:8080). The CRUD demo backs a Vaadin Grid by a ktorm-vaadin
   `EntityDataProvider`, with filter components attached to the grid header.

4. See [vok-example-crud](vok-example-crud) for the demo source. For the canonical end-to-end ktorm-vaadin sample,
   see [beverage-buddy-ktorm](https://github.com/mvysny/beverage-buddy-ktorm).

## Modules

* [vok-framework](vok-framework) — VoK runtime core: bootstrap, `Session`, `Cookies`, async executor, i18n. No DB
  dependency. Always pulled in transitively.
* [vok-framework-vokdb](vok-framework-vokdb) — Vaadin + SQL via ktorm + ktorm-vaadin. Provides
  `VaadinOnKotlin.dataSource` (which also wires `ActiveKtorm.database`), the `toId(idColumn)` Binder helper, and a
  reified-generic `enumFilterField<E>()` factory.
* [vok-rest](vok-rest) — REST **server** support. Javalin 5 + Gson. Exposes ktorm tables via
  `Table<E>.getCrudHandler()`. Read endpoints fully implemented; create/update/delete return 501 pending a
  Gson↔ktorm-Entity adapter.
* [vok-rest-client](vok-rest-client) — REST **client** helpers built on the JDK `HttpClient`. ORM-agnostic.
* [vok-example-crud](vok-example-crud) — runnable demo and integration-test harness.

## Code examples

### Define a ktorm entity + table

```kotlin
interface Person : ActiveEntity<Person> {
    var id: Long?
    @get:NotNull @get:Size(min = 1, max = 200) var name: String?
    @get:NotNull @get:Min(15) @get:Max(100)   var age: Int?
    var dateOfBirth: LocalDate?
    override val table: Table<Person> get() = Persons
    companion object : Entity.Factory<Person>()
}

object Persons : Table<Person>("Person") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val age = int("age").bindTo { it.age }
    val dateOfBirth = date("dateOfBirth").bindTo { it.dateOfBirth }
}
```

### Bootstrap

```kotlin
@WebListener
class Bootstrap : ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        VaadinOnKotlin.dataSource = HikariDataSource(hikariConfig)  // wires ActiveKtorm.database too
        VaadinOnKotlin.init()
        Flyway.configure().dataSource(VaadinOnKotlin.dataSource).load().migrate()
    }
    override fun contextDestroyed(sce: ServletContextEvent?) {
        VaadinOnKotlin.destroy()
    }
}
```

### Save / query

```kotlin
val p = Person { name = "Leto"; age = 45 }.create()           // insert
val one = db { database.sequenceOf(Persons).find { it.id eq p.id!! } }
val all = db { database.sequenceOf(Persons).toList() }
```

### Grid with ktorm-vaadin DataProvider + filters

```kotlin
private val nameFilter = FilterTextField()
private val ageFilter  = NumberRangePopup()
private val dataProvider = Persons.dataProvider

personGrid = grid<Person>(dataProvider) {
    appendHeaderRow()
    val filterBar = appendHeaderRow()
    columnFor(Person::name, key = Persons.name.e.key) {
        filterBar.getCell(this).component = nameFilter
        nameFilter.addValueChangeListener { updateFilter() }
    }
    columnFor(Person::age, key = Persons.age.e.key) {
        filterBar.getCell(this).component = ageFilter
        ageFilter.addValueChangeListener { updateFilter() }
    }
}

private fun updateFilter() {
    val conditions = mutableListOf<ColumnDeclaring<Boolean>?>()
    if (nameFilter.value.isNotBlank()) conditions += Persons.name.ilike("${nameFilter.value.trim()}%")
    conditions += Persons.age.between(ageFilter.value.asIntegerInterval())
    dataProvider.setFilter(conditions.and())
}
```

### UI DSL

```kotlin
verticalLayout {
    formLayout {
        textField("Name:") { focus() }
        textField("Age:")
    }
    horizontalLayout {
        button("Save") {
            onClick { okPressed() }
            setPrimary()
        }
    }
}
```

### REST server (vok-rest)

```kotlin
@WebServlet(urlPatterns = ["/rest/*"])
class JavalinRestServlet : HttpServlet() {
    private val javalin = Javalin.createStandalone { it.gsonMapper(VokRest.gson) }.apply {
        crud2("/rest/person", Persons.getCrudHandler(allowModification = true))
    }.javalinServlet()
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) { javalin.service(req, resp) }
}
```

Exposes `GET /rest/person?name=Leto&offset=0&limit=20&sort=age:desc&count=true` and so on. See
[vok-rest](vok-rest) for the full wire format.

## Contributing

Bug reports: [VoK issue tracker](https://github.com/mvysny/vaadin-on-kotlin/issues).

## Further links

* [Vaadin troubleshooting](https://mvysny.github.io/Vaadin-troubleshooting/)
* [ktorm docs](https://www.ktorm.org/)
* [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin)
* [beverage-buddy-ktorm](https://github.com/mvysny/beverage-buddy-ktorm) — canonical end-to-end example

# License

Licensed under the [MIT License](https://opensource.org/licenses/MIT).

Copyright (c) 2017-2026 Martin Vysny.
