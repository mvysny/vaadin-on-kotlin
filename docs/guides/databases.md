---
layout: default
title: Accessing Databases Guide
permalink: /databases/
parent: Guides
nav_order: 6
---

<br/>
<details close markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>
<br/>

# Accessing SQL Databases With Vaadin-on-Kotlin

Vaadin-on-Kotlin provides first-class support for the following SQL databases out-of-the-box:

* [H2 Database](http://h2database.com) — a 100% Java database which can be quick-started as an in-memory
  database; perfect for writing tests for your app.
* [PostgreSQL](https://www.postgresql.org/)
* [MariaDB](https://mariadb.org/)
* [MySQL](https://www.mysql.com/)

All other SQL databases may or may not work. Care has been taken to only use the SQL92 syntax,
but only the four databases above are tested and officially supported.

> *NoSQL Note*: only SQL databases which provide appropriate JDBC drivers are currently supported.
There is no direct support for NoSQL databases, but you can easily integrate any NoSQL database with VoK.
Please read [Accessing NoSQL or REST data sources](/nosql_rest_datasources) for more information.

> *Note for experienced Java developers*: VoK is *not* using JPA nor Hibernate to access the
database. The reason is that there are inherent issues with the abstraction that JPA
mandates — see [Why Not JPA](https://mvysny.github.io/back-to-base-make-sql-great-again/).

## Why ktorm

The persistence layer is [ktorm](https://www.ktorm.org/) plus
[ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) (which adds Vaadin DataProviders, filter components,
and Binder helpers on top of ktorm). The design follows the same simplicity-first philosophy that previous
versions of VoK had:

* Plain SQL stays first-class. ktorm's type-safe DSL emits the SQL you expect; for everything else you
  can drop down to `database.useConnection { ... }` and write the SQL yourself.
* Entities are interfaces, proxied at runtime over a `LinkedHashMap`. They don't track changes, don't
  auto-flush, and don't need bytecode enhancement. You decide when to `save()`.
* No N+1 surprises: ktorm fetches what you ask for. Joins, projections, aggregates — all explicit in the DSL.
* You can map any SELECT (including joins, views, aggregates) to a Kotlin class — which means any SELECT
  can back a Vaadin Grid.

The rest of this guide focuses on the **VoK-specific bits** — wiring the data source, the `db { }` block,
the Vaadin Grid integration, and form binding. For schema definition, the column-type catalog, and the
full Entity Sequence DSL, **the canonical reference is [ktorm.org](https://www.ktorm.org/)** — we link to
the relevant chapters as we go rather than re-explain them here.

## Defining entities and tables

You write two declarations per table: an `Entity<E>` interface for the row shape, and a `Table<E>` object
for the schema mapping.

Add the dependency first:

```kotlin
dependencies {
    implementation("eu.vaadinonkotlin:vok-framework-vokdb:x.y.z")
}
```

> See the [latest release tag](https://github.com/mvysny/vaadin-on-kotlin/tags) for `x.y.z`.

Consider a `Person` table:

| Column | Type | Meaning
| ------ | ---- | -------
| id | Long | Primary key, auto-generated
| name | String | Full name
| age | Int | Age in years
| dateOfBirth | LocalDate? | Optional date of birth
| alive | Boolean | Whether the person is alive
| maritalStatus | MaritalStatus? | Enum: Single / Married / Divorced / Widowed
| created | Instant | When the row was created

Create a Flyway migration at `src/main/resources/db/migration/V01__CreatePerson.sql`. The H2 flavour:

```sql
create table Person (
    id bigint primary key auto_increment,
    name varchar not null,
    age integer not null,
    dateOfBirth date,
    alive boolean not null,
    maritalStatus varchar,
    created timestamp not null
);
```

The PostgreSQL flavour uses `bigserial`; MySQL/MariaDB uses `bigint primary key auto_increment` and
`timestamp(3)` for sub-second precision.

> *MaritalStatus Note*: we store the enum name (`"Single"`) rather than its ordinal. Ordinals are easy to
> break by reordering constants, and the data would silently load with the wrong value.

The Kotlin side — `Person.kt`:

```kotlin
package com.example.vok

import com.github.mvysny.ktormvaadin.ActiveEntity
import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.Instant
import java.time.LocalDate

enum class MaritalStatus { Single, Married, Divorced, Widowed }

interface Person : ActiveEntity<Person> {
    var id: Long?
    var name: String?
    var age: Int?
    var dateOfBirth: LocalDate?
    var alive: Boolean?
    var maritalStatus: MaritalStatus?
    var created: Instant?

    override val table: Table<Person> get() = Persons

    companion object : Entity.Factory<Person>()
}

object Persons : Table<Person>("Person") {
    val id           = long("id").primaryKey().bindTo { it.id }
    val name         = varchar("name").bindTo { it.name }
    val age          = int("age").bindTo { it.age }
    val dateOfBirth  = date("dateOfBirth").bindTo { it.dateOfBirth }
    val alive        = boolean("alive").bindTo { it.alive }
    val maritalStatus = enum<MaritalStatus>("maritalStatus").bindTo { it.maritalStatus }
    val created      = timestamp("created").bindTo { it.created }
}
```

A few notes on what's happening:

* `Entity<Person>` is the ktorm-provided interface; ktorm creates the runtime implementation for you.
  Don't add any backing state — every `var` is mapped to a column.
* `ActiveEntity<Person>` is ktorm-vaadin's extension that adds instance-level `save()` / `create()` /
  `delete()` (and `validate()`). It requires you to point back at the `Table` via the `table` property.
* `Entity.Factory<Person>()` lets you write `Person { name = "Foo" }` to construct an unsaved entity.
* `Table<Person>("Person")` names the underlying SQL table.
* The column-builder methods (`long`, `varchar`, `date`, `enum`, `timestamp`, …) come from ktorm —
  see [Schema Definition](https://www.ktorm.org/en/schema-definition.html) for the full list and how to
  define custom column types.

## Bootstrap

The expected app shape (mirrored in
[`vok-example-crud/.../Bootstrap.kt`](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud/src/main/kotlin/example/crudflow/Bootstrap.kt)):

```kotlin
val config = HikariConfig().apply {
    driverClassName = Driver::class.java.name  // org.h2.Driver
    jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
    username = "sa"
    password = ""
}
VaadinOnKotlin.dataSource = HikariDataSource(config)  // also wires ActiveKtorm.database
VaadinOnKotlin.init()

Flyway.configure().dataSource(VaadinOnKotlin.dataSource).load().migrate()
```

Setting `VaadinOnKotlin.dataSource` is the canonical hook: it stores the DataSource for the rest of VoK
and also sets ktorm-vaadin's `ActiveKtorm.database`, so ktorm queries work without an extra step.

> *H2 quirk:* `DATABASE_TO_UPPER=FALSE` keeps identifiers case-sensitive so ktorm's quoted `"Person"`,
> `"name"`, etc. match the Flyway DDL exactly. Without it, H2 folds unquoted names to upper case and your
> queries 500 with "table not found".

On shutdown, call `VaadinOnKotlin.destroy()`. There's no separate ktorm shutdown step — closing the
HikariDataSource (or letting the JVM exit) is enough.

## CRUD with `db { }`

All ktorm calls must happen inside a transaction. ktorm-vaadin provides a top-level `db { }` block whose
receiver is a `KtormContext`, which exposes `database` and `transaction`:

```kotlin
db {
    val person = Person { name = "John Doe"; age = 42; alive = true; created = Instant.now() }
    person.create()                                  // INSERT, populates person.id

    val loaded = database.sequenceOf(Persons)
        .find { it.id eq person.id!! }               // SELECT … WHERE id = ?
    loaded!!.name = "Agent Smith"
    loaded.save()                                    // UPDATE … SET name = ? WHERE id = ?

    database.sequenceOf(Persons)
        .filter { it.alive eq true }
        .sortedBy { it.age }
        .forEach { println(it.name) }

    loaded.delete()                                  // DELETE FROM … WHERE id = ?
}
```

You can also call the entity instance methods (`save()`, `create()`, `delete()`) **outside** a `db { }`
block — each opens its own short-lived transaction. Same goes for ktorm-vaadin's `Table` shortcuts:

```kotlin
Persons.findAll()      // returns List<Person>
Persons.count()
Persons.deleteAll()
Persons.single()       // exactly one or throws
```

For everything richer than these four shortcuts, **drop into a `db { }` block and use the ktorm
sequence DSL**: `find`, `filter`, `sortedBy`, `mapColumns`, `groupingBy`, `aggregateColumns`, etc.
See [Entity Sequence APIs](https://www.ktorm.org/en/entity-sequence.html) for the full menu.

## Forms

Forms let users enter or edit row data, ideally with validation. We use
[Vaadin Binder](https://vaadin.com/docs/latest/binding-data/components-binder) plus JSR-303 bean validation
(Hibernate Validator 9). Place the validation annotations on the **getter** of each interface property
using `@get:`, since the entity is a proxied interface:

```kotlin
import com.github.mvysny.ktormvaadin.ActiveEntity
import jakarta.validation.constraints.*
import org.ktorm.entity.Entity
import org.ktorm.schema.Table

interface Person : ActiveEntity<Person> {
    var id: Long?

    @get:NotNull
    @get:Size(min = 1, max = 200)
    var name: String?

    @get:NotNull
    @get:Min(0)
    @get:Max(150)
    var age: Int?

    var dateOfBirth: LocalDate?

    @get:NotNull
    var alive: Boolean?

    var maritalStatus: MaritalStatus?

    @get:NotNull
    var created: Instant?

    override val table: Table<Person> get() = Persons
    companion object : Entity.Factory<Person>()
}
```

A reusable editor component:

```kotlin
class PersonForm : FormLayout() {
    val binder = beanValidationBinder<Person>()

    init {
        textField("Name") {
            bind(binder).trimmingConverter().bind(Person::name)
        }
        textField("Age") {
            bind(binder).toInt().bind(Person::age)
        }
        datePicker("Date of Birth") {
            bind(binder).bind(Person::dateOfBirth)
        }
        checkBox("Alive") {
            bind(binder).bind(Person::alive)
        }
        comboBox<MaritalStatus>("Marital Status") {
            setItems(*MaritalStatus.entries.toTypedArray())
            bind(binder).bind(Person::maritalStatus)
        }
    }
}

fun HasComponents.personForm(block: PersonForm.()->Unit = {}) = init(PersonForm(), block)
```

Saving the bean is a separate concern; a typical "Save" button looks like:

```kotlin
button("Save") {
    onClick {
        if (binder.validate().isOk && binder.writeBeanIfValid(person)) {
            if (person.created == null) person.created = Instant.now()
            person.save()
        }
    }
}
```

> *Why `@get:` on the interface*: bean-validation traverses getter annotations. Putting `@NotNull` on
> the property directly attaches it to the field of a generated class, where Validator can't see it.

See [CreateEditPerson.kt](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud/src/main/kotlin/example/crudflow/person/CreateEditPerson.kt)
in the demo for the full edit-dialog pattern.

## Showing entities in a Grid

Vaadin Grid is the workhorse for tabular data. ktorm-vaadin's `Persons.dataProvider` extension property
hands you an `EntityDataProvider<Person>` that:

* lazy-loads pages of rows on scroll,
* translates Grid sorting clicks into SQL `ORDER BY` against the ktorm columns,
* accepts a `ColumnDeclaring<Boolean>` filter (a ktorm expression) via `setFilter()`.

A minimal Grid:

```kotlin
@Route("")
class PersonListView : VerticalLayout() {
    init {
        setSizeFull()
        grid<Person>(Persons.dataProvider) {
            setSizeFull()
            columnFor(Person::id)
            columnFor(Person::name)
            columnFor(Person::age)
            columnFor(Person::dateOfBirth, converter = { it?.toString() })
            columnFor(Person::maritalStatus)
            columnFor(Person::alive)
            columnFor(Person::created, converter = { it?.toString() })
            addColumn(NativeButtonRenderer<Person>("Delete") { person ->
                person.delete()
                this@grid.refresh()
            })
        }
    }
}
```

That's a full lazy-loading Grid with SQL-side sorting working out of the box.

See [Using Grids](/grids) for column configuration patterns (renderers, hidden columns, formatting).

### Grid filters

The recipe for filter-bar filtering is the same as before, just with ktorm types:

1. Create one filter component per column (`FilterTextField`, `NumberRangePopup`, `DateRangePopup`,
   `BooleanFilterField`, `EnumFilterField`).
2. Add a header row via `appendHeaderRow()` and place each filter component into its column's cell.
3. On any filter value change, build a single `ColumnDeclaring<Boolean>` from the currently-set filters
   and push it to the data provider via `setFilter()`.

ktorm-vaadin ships with a `Collection<ColumnDeclaring<Boolean>?>.and()` extension that ANDs a list of
nullable conditions (skipping nulls) into one, which makes step 3 a one-liner:

```kotlin
import com.github.mvysny.ktormvaadin.and
import com.github.mvysny.ktormvaadin.dataProvider
import com.github.mvysny.ktormvaadin.filter.*
import eu.vaadinonkotlin.vaadin.vokdb.enumFilterField
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike   // works on H2 too

class PersonListView : KComposite() {
    private val nameFilter = FilterTextField()
    private val ageFilter = NumberRangePopup()
    private val aliveFilter = BooleanFilterField()
    private val dateOfBirthFilter = DateRangePopup()
    private val maritalStatusFilter = enumFilterField<MaritalStatus>()
    private val dataProvider = Persons.dataProvider

    private val root = ui {
        verticalLayout {
            setSizeFull()
            grid<Person>(dataProvider) {
                setSizeFull()
                val filterBar = appendHeaderRow()

                columnFor(Person::name) {
                    nameFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = nameFilter
                }
                columnFor(Person::age) {
                    ageFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = ageFilter
                }
                columnFor(Person::alive) {
                    aliveFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = aliveFilter
                }
                columnFor(Person::dateOfBirth, converter = { it?.toString() }) {
                    dateOfBirthFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = dateOfBirthFilter
                }
                columnFor(Person::maritalStatus) {
                    maritalStatusFilter.addValueChangeListener { updateFilter() }
                    filterBar.getCell(this).component = maritalStatusFilter
                }
            }
        }
    }

    private fun updateFilter() {
        val conditions = mutableListOf<ColumnDeclaring<Boolean>?>()
        if (nameFilter.value.isNotBlank()) {
            conditions += Persons.name.ilike("${nameFilter.value.trim()}%")
        }
        conditions += Persons.age.between(ageFilter.value.asIntegerInterval())
        aliveFilter.value?.let { conditions += Persons.alive eq it }
        conditions += Persons.dateOfBirth.between(dateOfBirthFilter.value)
        val statuses = maritalStatusFilter.value
        if (statuses.isNotEmpty() && statuses.size < MaritalStatus.entries.size) {
            conditions += Persons.maritalStatus.inList(statuses.toList())
        }
        dataProvider.setFilter(conditions.and())
    }
}
```

The full working example is
[PersonListView.kt](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud/src/main/kotlin/example/crudflow/person/PersonListView.kt)
in the demo app — copy from there.

#### What ktorm-vaadin provides

The filter components shipped in `com.github.mvysny.ktormvaadin.filter`:

* **`FilterTextField`** — a Vaadin `TextField` with `ValueChangeMode.LAZY` and a clear button preconfigured.
  Combine with `Column.ilike("$value%")` for starts-with search, or `like` for case-sensitive.
* **`NumberRangePopup`** — value is `NumberInterval<Double>` (open at either end). Convert with
  `.asIntegerInterval()` / `.asLongInterval()`, then `Column<T>.between(NumberInterval<T>)` produces the
  ktorm condition.
* **`DateRangePopup`** — value is `DateInterval` (LocalDate range, open at either end). `Column<LocalDate>.between(DateInterval)`
  produces the condition. For `Instant`-typed columns you need to widen the day range to instants in the
  browser's timezone — see `PersonListView.containsInstant` in the demo for the helper.
* **`BooleanFilterField`** — three-state: `true` / `false` / `null` (no filter).
* **`EnumFilterField<E>`** — multi-select over enum values; produces a `Set<E>`. Use with
  `Column.inList(values)`. The convenience factory `enumFilterField<MaritalStatus>()` (from
  `vok-framework-vokdb`) saves you a reified-type-parameter dance.

#### What about plain Vaadin fields?

You don't have to use the ktorm-vaadin components — any field that emits value-change events works the
same way. A bare `TextField` for "starts with" search is fine, as long as your `updateFilter()` converts
its value into a `ColumnDeclaring<Boolean>` you can push.

## Custom SELECTs, joins, and projections

When you need data that isn't a 1:1 mapping of a single table — joins, projections, aggregates — you
have two options depending on whether the result needs to back a Grid or just a list.

### Read-only lists from `db { }`

For a one-shot list (a summary screen, a CSV export, etc.) just write the query inside `db { }`:

```kotlin
data class PersonDept(val personName: String, val deptName: String)

fun findAll(): List<PersonDept> = db {
    database
        .from(Persons)
        .innerJoin(Departments, on = Persons.deptId eq Departments.id)
        .select(Persons.name, Departments.name)
        .map { row ->
            PersonDept(
                personName = row[Persons.name]!!,
                deptName = row[Departments.name]!!,
            )
        }
}
```

`database.from(...).innerJoin(...).select(...)` is ktorm's Query DSL — see
[Joining](https://www.ktorm.org/en/joining.html) and [Query](https://www.ktorm.org/en/query.html) for the
full picture (left/right/cross joins, aggregates, grouping, etc.). Note that this `PersonDept` is *not*
an entity; it's just a plain `data class` we map into.

### Custom SELECTs that back a Grid

For a Grid backed by an arbitrary SELECT, use ktorm-vaadin's `QueryDataProvider<T>`. You give it two
functions: one that builds the ktorm `Query`, and one that maps each `QueryRowSet` row to your bean.
Paging, sorting (via Grid's sort clicks), and filtering all work through `setFilter()` exactly as with
`EntityDataProvider`:

```kotlin
val dataProvider = QueryDataProvider<PersonDept>(
    query = { database ->
        database
            .from(Persons)
            .innerJoin(Departments, on = Persons.deptId eq Departments.id)
            .select(Persons.name, Departments.name)
    },
    rowMapper = { row ->
        PersonDept(
            personName = row[Persons.name]!!,
            deptName = row[Departments.name]!!,
        )
    },
)

grid<PersonDept>(dataProvider) {
    setSizeFull()
    columnFor(PersonDept::personName)
    columnFor(PersonDept::deptName)
}
```

Then filter wiring is the same as for `EntityDataProvider` — collect `ColumnDeclaring<Boolean>` conditions
and call `dataProvider.setFilter(conditions.and())`.

## Combining filter sources

A common need: a Grid with column-level filter components **plus** an outer "search-everywhere"
`TextField` above the Grid, with the two ANDed together.

In ktorm-vaadin there is no separate "chained DataProvider" or "configurable filter" wrapper — there's
only one `setFilter()` per data provider. The pattern is simpler than it used to be: keep all your filter
inputs as fields on the view, and have **one** `updateFilter()` method that reads all of them and pushes
a single combined `ColumnDeclaring<Boolean>`:

```kotlin
private val searchEverywhere = TextField()
private val nameFilter = FilterTextField()
private val deptFilter = FilterTextField()
private val dataProvider = PersonDeptQuery.dataProvider   // your QueryDataProvider

init {
    searchEverywhere.addValueChangeListener { updateFilter() }
    nameFilter.addValueChangeListener { updateFilter() }
    deptFilter.addValueChangeListener { updateFilter() }
}

private fun updateFilter() {
    val conditions = mutableListOf<ColumnDeclaring<Boolean>?>()

    val q = searchEverywhere.value.trim()
    if (q.isNotEmpty()) {
        conditions += (Persons.name.ilike("$q%")) or (Departments.name.ilike("$q%"))
    }
    if (nameFilter.value.isNotBlank()) {
        conditions += Persons.name.ilike("${nameFilter.value.trim()}%")
    }
    if (deptFilter.value.isNotBlank()) {
        conditions += Departments.name.ilike("${deptFilter.value.trim()}%")
    }

    dataProvider.setFilter(conditions.and())
}
```

To add a permanently-applied filter (say: only show rows for one company), include it unconditionally:

```kotlin
private fun updateFilter() {
    val conditions = mutableListOf<ColumnDeclaring<Boolean>?>(
        Persons.companyId eq currentCompanyId          // always applied
    )
    // … user-driven conditions appended here as above …
    dataProvider.setFilter(conditions.and())
}
```

> *Why the change*: the old `withFilter { … }` / `withConfigurableFilter2()` wrappers existed to work
> around an order-of-operations problem in the previous DataProvider stack. With a single `setFilter()`
> in ktorm-vaadin's providers, you just AND your sources together in one place. Less moving parts.

## Exporting data from DataProviders

To export the rows currently visible in the Grid (i.e. honoring filters set by the bar above):

* **The simple case** — re-run the same query through the data provider's underlying source. For an
  `EntityDataProvider`, that's `db { database.sequenceOf(Persons).filter { /* same conditions */ }.toList() }`.
  This re-derives the filter from your `updateFilter()` logic and is the easiest to reason about.
* **The exhaustive case** — pull pages via Vaadin's standard `DataProvider.fetch(Query)` and concatenate.
  Be careful: this can run out of memory for very large tables, so prefer a streaming CSV/Excel writer.

For straightforward "export everything" you can call `Persons.findAll()` (no filter) or write the SELECT
yourself with the ktorm sequence DSL.

## Further reading

* [ktorm.org](https://www.ktorm.org/) — schema definition, query DSL, entity sequences, joins, aggregates.
* [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) — DataProviders, filter components, Binder helpers.
* [vok-framework-vokdb README](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-framework-vokdb)
  — VoK-side additions (DataSource wiring, Binder `toId` helper, `enumFilterField`).
* [vok-example-crud](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-example-crud) — the
  runnable demo and end-to-end integration-test harness. Best place to copy patterns from.
* [beverage-buddy-ktorm](https://github.com/mvysny/beverage-buddy-ktorm) — a fuller example app.
