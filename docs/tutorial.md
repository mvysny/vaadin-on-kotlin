---
layout: default
title: Tutorial
permalink: /tutorial/
nav_order: 2
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

# Getting started with Vaadin-on-Kotlin

Vaadin-on-Kotlin, or VoK for short, is an opinionated web application framework which
uses Vaadin for its front-end and Kotlin for its back-end. Vaadin lets you forget the
web and program user interfaces much like when building a desktop application in
Swing or JavaFX. Kotlin lets you write clean, understandable and maintainable code.

This guide teaches you how to get VoK up and running by building a small, realistic
app one feature at a time. After reading it, you will be able to:

- Create a VoK application and connect it to a SQL database.
- Understand the general layout of a VoK application.
- Build interactive UI in Kotlin using Karibu-DSL.
- Persist and query data with Flyway migrations and ktorm.
- Expose a small REST API on top of your data.

## Guide assumptions

This guide assumes you have no prior knowledge of VoK and that your goal is to build
a VoK application from scratch. You don't need to be a seasoned programmer, but a
basic working knowledge of Kotlin and Vaadin will help. If you're new to either:

- For Kotlin: [kotlinlang.org](https://kotlinlang.org/) and the
  [Kotlin Koans](https://kotlinlang.org/docs/koans.html).
- For Vaadin: the [official Vaadin documentation](https://vaadin.com/docs).

If you'd like a minimal sandbox to experiment with Karibu-DSL itself,
[karibu10-helloworld-application](https://github.com/mvysny/karibu10-helloworld-application)
is a tiny standalone example.

## Completed app

If you get stuck, or simply want to skip ahead and see the finished app in action,
check out the [`complete`](https://github.com/mvysny/vok-helloworld-app/tree/complete)
branch of the [vok-helloworld-app](https://github.com/mvysny/vok-helloworld-app)
repository. Each chapter in this tutorial corresponds to a single commit on that
branch, so you can `git checkout` any chapter's tip to see exactly what your code
should look like at that point.

## What is Vaadin-on-Kotlin?

VoK is glue between Vaadin, Kotlin and a small set of supporting libraries that lets
you write web apps without ceremony. It makes the assumption that there is a "best"
way to do things and is designed to encourage that way.

The VoK philosophy has three guiding principles:

- **Simplicity** — things are kept as simple as possible. Libraries are added only
  when necessary. Complex patterns like Dependency Injection and MVC are deliberately
  left out.
- **Components as basic building blocks** — Vaadin is a single-page web-component
  framework as opposed to a traditional multi-page request/response framework. It
  resembles fat-client GUI development (Swing, JavaFX) more than typical web
  development. VoK promotes code and UI reuse by composing components — from
  individual fields up to entire forms — rather than by templating pages.
- **No magic** — no proxies, no interceptors, no classpath scanning. VoK is explicit
  functions and classes that your IDE can navigate to without surprise.

### Notes for Java programmers

Traditionally JavaEE and Spring acted as the "glue" holding frameworks together. With
Kotlin, we believe that the language itself provides all the glue you need.

Dependency Injection (DI) in particular comes with costs:

- DI pushes you to create Services/DAOs even for trivial CRUD. That's fine on large
  teams; it's overkill for the kind of app this tutorial builds.
- DI requires a container (JavaEE server, Spring) — heavy machinery for a newcomer.
- DI scales poorly: configuration grows alongside the project and rarely shrinks.
- DI fights Vaadin: injecting services into Vaadin components forces those components
  to become managed beans, which is not how Vaadin is designed to be used.

VoK does not use DI. You can still bring Spring or JavaEE if your project needs them,
but the tutorial app does not.

> **Note on MVC.** MVC works well in page-oriented frameworks like Rails or Django,
> where each page is a unit of presentation. In component-oriented frameworks the
> unit of reuse is much smaller — a single `Grid` or `Dialog` — and the result of
> applying MVC tends to be near-empty views with logic split across files for no real
> gain. This tutorial does not use MVC.

## The example app: BoltShop

We'll build **BoltShop** — a small neighbourhood hardware store. The shop has an
online product catalog and a Click & Collect workflow: customers browse the catalog,
pick items, and pick them up in the shop. We'll focus on the **back-office screen**
the shopkeeper uses to maintain the catalog.

That screen is a single Vaadin SPA view that looks like this when it's done:

- a `Grid` of products on the left, with live search, category filter, and a
  "low stock only" toggle;
- a details panel on the right with a `Binder`-driven form to edit the selected row;
- a `+ Add product` button that opens a `Dialog` reusing the same form.

This master-detail shape — list on one side, editable detail on the other, reactive
filtering above — is **the** canonical shape Vaadin is good at, and a multi-page
request/response framework genuinely cannot pull it off without fighting the
platform. By the end of the tutorial you'll have built it from scratch.

## Tutorial structure

The tutorial is split into short chapters. Each one introduces a single new concept
and ends with a runnable, demo-able app:

| # | Chapter | Concept |
|---|---|---|
| 0 | Starting point | Cloning, running, and what's pre-wired |
| 1 | Hello, Karibu-DSL | Building UI in Kotlin; event handlers |
| 2 | A Grid of products | `Grid`, in-memory data |
| 3 | Persisting to the database | Flyway, ktorm entities, finders |
| 4 | Live filtering | `DataProvider`, reactive filters |
| 5 | Category filter | `ComboBox`, enums in the UI |
| 6 | Editing the selected product | Side panel + `Binder` |
| 7 | Adding new products | `Dialog`, extracting a reusable form |
| 8 | Validation | `Binder` validators, inline errors |
| 9 | Custom cell rendering | `ComponentRenderer`, badges |
| 10 | Browserless tests | Karibu-Testing |
| 11 | Exposing a REST API | `vok-rest` + Javalin |

Tests are deliberately deferred to a single dedicated chapter at the end. The goal in
early chapters is *learning momentum*, not test discipline; once you can see the app
do something, adding tests becomes much easier.

# Chapter 0 — Setting up

## Prerequisites

VoK requires **Java 21 JDK** to be installed. The example application has the Gradle
wrapper bundled in; Gradle will download everything else (Vaadin, Kotlin, libraries,
the embedded Jetty server). This makes VoK applications portable — they work on any
OS and CPU that supports Java 21: Windows, Linux, macOS, on x86 or ARM.

The example also uses an embedded Java database called
[H2](https://www.h2database.com/), so there is no separate database to set up.

While you can edit the project with any text editor, we recommend
[IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/) for its
excellent Kotlin support.

## Clone and run

Clone the starter repo:

```bash
$ git clone https://github.com/mvysny/vok-helloworld-app
$ cd vok-helloworld-app
```

If you don't have Git, you can download the starter as a zip from
[github.com/mvysny/vok-helloworld-app](https://github.com/mvysny/vok-helloworld-app).

Run the app:

```bash
$ ./gradlew run
```

On the first run Gradle will download Vaadin, Kotlin, and the rest of the
dependencies, which takes a minute or two. When the build is done you'll see
something like:

```
=================================================
Embedded Jetty started successfully on http://localhost:8080
=================================================
```

Open <http://localhost:8080> in your browser. You should see a single heading
reading **Welcome to BoltShop**. That's it — that's all the starter does. Stop the
server with `Ctrl+C` when you're done looking.

## What's pre-wired

The starter is intentionally minimal. The parts you'll build on in the chapters
ahead are already on the classpath and bootstrapped, but no domain code exists yet.
Here's what's there:

- **Vaadin Boot.** Embedded Jetty launched from a plain `main()` in
  `com.example.vok.Main`. No Spring, no servlet container to install.
- **Karibu-DSL** for writing UI in Kotlin.
- **H2 + Flyway + ktorm** (via `vok-db`). `Bootstrap.kt` wires a Hikari datasource
  into `VaadinOnKotlin.dataSource` and runs Flyway on startup. There are no
  migrations yet, so the database boots empty — we'll add the first migration in
  Chapter 3.
- **REST scaffolding.** `JavalinRestServlet` is in place but registers no
  endpoints. We'll wire it up in Chapter 11.
- **`WelcomeView`** — a single `KComposite` showing the *"Welcome to BoltShop"*
  heading. You'll replace it almost immediately in the next chapter.

The whole layout is small enough to skim in a few minutes:

```
src/main/kotlin/com/example/vok/
├── Bootstrap.kt    — startup: datasource, VoK init, Flyway, REST scaffold
├── Main.kt         — the main() that boots Vaadin Boot
├── Utils.kt        — small helpers
└── WelcomeView.kt  — the @Route("") landing page
```

Have a look at `Bootstrap.kt` if you're curious — there's nothing magical in it.

# Chapter 1 — Hello, Karibu-DSL

In this chapter we'll replace the static welcome heading with a small interactive
form: a text field where the visitor types their name, and a button that pops up a
greeting notification. Nothing earth-shattering, but it introduces two things you'll
use in every chapter after this one:

- **Karibu-DSL builders** — type-safe Kotlin functions for composing Vaadin
  components inside a `KComposite`.
- **Event handlers** — wiring a server-side block of Kotlin to a click on the
  client.

## The starting view

Open `src/main/kotlin/com/example/vok/WelcomeView.kt`. It looks like this:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); content { align(center, middle) }; isMargin = false; isSpacing = true

            h1("Welcome to BoltShop")
        }
    }
}
```

A few things to notice:

- `@Route("")` makes this view the application's landing page (the empty string =
  the URL root).
- `KComposite` is the Karibu-DSL base class for views composed of other components.
  Every view in BoltShop will extend it.
- `ui { ... }` is the *one* DSL block where you build the view's component tree.
  Inside it, `verticalLayout`, `h1`, etc. are extension functions that create the
  corresponding Vaadin component and add it to its parent.
- The chained calls inside `verticalLayout` — `setSizeFull()`,
  `content { align(center, middle) }`, `isMargin = false`, `isSpacing = true` —
  center the contents of the layout on the page.

That's the *entire* welcome page.

## Add a name field and a button

Change `WelcomeView.kt` to read:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); content { align(center, middle) }; isMargin = false; isSpacing = true

            h1("Welcome to BoltShop")
            val nameField = textField("Your name") {
                placeholder = "e.g. Alice"
            }
            button("Say hello") {
                onClick {
                    val name = nameField.value.ifBlank { "stranger" }
                    Notification.show("Hello, $name!")
                }
            }
        }
    }
}
```

Restart the app (`./gradlew run`) and reload <http://localhost:8080>. Type a name,
click **Say hello**, and a notification slides up from the bottom of the page.

## What just happened

Three Karibu-DSL features made that work:

**Builders create and attach components in one step.**
`textField("Your name") { ... }` constructs a Vaadin `TextField` with the label
*"Your name"*, runs the trailing lambda against it (so you can tweak its properties —
here we set `placeholder`), **and** adds it to the surrounding `verticalLayout`.
There's no separate `add(...)` call. The same pattern applies to `button`, `h1`,
`verticalLayout`, and every component in Karibu-DSL.

**You can capture a component into a local `val`.**
The expression `val nameField = textField(...)` lets the click listener later refer
back to the text field. Karibu-DSL builders return the component they create, so
this is just a plain Kotlin local-variable assignment — no framework magic.

**`onClick { ... }` is the click handler.**
On any clickable component, `onClick { event -> ... }` registers a server-side
listener. When the user clicks the button, Vaadin transparently sends a request to
the server and runs your lambda. Inside, `nameField.value` is the current text in
the field (Vaadin pushed it to the server with the click), and `Notification.show`
schedules a notification toast in the response back to the browser.

> **Stateful by default.** Notice that the button click reads `nameField.value`
> directly. There's no controller, no form binding, no JSON over the wire — the
> server holds the live component tree and the client is a thin renderer of it.
> This is the Vaadin model, and it's what makes the rest of the tutorial possible.

In the next chapter we'll throw away this greeting form and put something more
useful in its place: a `Grid` of products.

# Chapter 2 — A Grid of products

Vaadin's `Grid` is a data table component: virtual scrolling, sortable columns,
selection, and lazy loading all out of the box. It's the workhorse of nearly every
back-office screen, including ours. In this chapter we'll define what a *product*
is and put a list of ten of them on screen.

We're not touching the database yet. The list of products is hardcoded in Kotlin.
That's not a long-term design — it's a deliberate choice to let you see a Grid
working on its own before we layer Flyway and ktorm on top in Chapter 3.

## Define the Product type

Create a new file `src/main/kotlin/com/example/vok/Product.kt`:

```kotlin
package com.example.vok

import java.math.BigDecimal

enum class Category { Tools, Fasteners, Plumbing, Electrical, Paint, Garden }

enum class UnitOfMeasure { Each, Box, Meter, Kilogram }

data class Product(
    val sku: String,
    val name: String,
    val category: Category,
    val price: BigDecimal,
    val stock: Int,
    val unit: UnitOfMeasure
)
```

A few notes:

- `Product` is a plain Kotlin `data class`, not a database entity. Chapter 3 will
  promote it; for now think of it as the simplest possible record.
- `BigDecimal` (not `Double`) for `price`. Floating-point and money do not mix —
  `0.1 + 0.2 = 0.30000000000000004` is the kind of bug that ships to production.
- `Category` and `UnitOfMeasure` are enums. They give us a closed set of values
  the UI can iterate over (handy for filters in Chapter 5).
- The enum is `UnitOfMeasure` rather than `Unit` because `kotlin.Unit` would
  shadow it under wildcard imports. The *field* is still called `unit`.

## Replace the welcome view with a catalog view

Rename `WelcomeView.kt` to `CatalogView.kt` — in IntelliJ that's *Refactor →
Rename*, on the command line `git mv` does it. Then replace its contents with:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.router.Route

@Route("")
class CatalogView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")
            grid<Product>(Product::class) {
                setSizeFull()
                setItems(sampleProducts)
                columnFor(Product::sku) { setHeader("SKU") }
                columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                columnFor(Product::category) { setHeader("Category") }
                columnFor(Product::price) { setHeader("Price") }
                columnFor(Product::stock) { setHeader("Stock") }
                columnFor(Product::unit) { setHeader("Unit") }
            }
        }
    }
}

private val sampleProducts: List<Product> = listOf(
    Product("HX-M6-40",      "Hex bolt M6×40mm, zinc-plated",    Category.Fasteners,  "0.35".toBigDecimal(), 120, UnitOfMeasure.Each),
    Product("HX-M8-50",      "Hex bolt M8×50mm, stainless",      Category.Fasteners,  "0.85".toBigDecimal(),   0, UnitOfMeasure.Each),
    Product("NT-M6-BOX100",  "Hex nut M6, zinc-plated, 100/box", Category.Fasteners,  "4.50".toBigDecimal(),  18, UnitOfMeasure.Box),
    Product("WD40-400",      "WD-40 lubricant spray 400ml",      Category.Tools,      "7.90".toBigDecimal(),  32, UnitOfMeasure.Each),
    Product("SD-FLAT-6",     "Screwdriver, flat blade 6mm",      Category.Tools,      "6.50".toBigDecimal(),  12, UnitOfMeasure.Each),
    Product("PIPE-CU-22",    "Copper pipe Ø22mm",                Category.Plumbing,  "12.40".toBigDecimal(),  45, UnitOfMeasure.Meter),
    Product("CABLE-3G15",    "Power cable 3G1.5mm²",             Category.Electrical, "1.80".toBigDecimal(), 200, UnitOfMeasure.Meter),
    Product("PAINT-WHT-1L",  "Interior paint, white matte 1L",   Category.Paint,     "14.90".toBigDecimal(),   8, UnitOfMeasure.Each),
    Product("PAINT-WHT-10L", "Interior paint, white matte 10L",  Category.Paint,     "89.00".toBigDecimal(),   3, UnitOfMeasure.Each),
    Product("SAND-CONCRETE", "Concrete sand",                    Category.Garden,     "0.45".toBigDecimal(), 800, UnitOfMeasure.Kilogram),
)
```

Restart the app (`./gradlew run`) and reload <http://localhost:8080>. You'll see a
ten-row table with columns for SKU, Name, Category, Price, Stock and Unit. Click a
column header to sort by it. The whole grid fills the viewport because we set
`setSizeFull()` on both the outer layout and the Grid itself.

## What just happened

**`grid<Product>(Product::class) { ... }`** is the Karibu-DSL builder for
`com.vaadin.flow.component.grid.Grid`. The type parameter and the `KClass`
argument tell the Grid what row type it's bound to — Karibu-DSL needs the class
itself (not just the generic type) so it can resolve the property references we
pass to `columnFor` below.

**`setItems(sampleProducts)`** hands the Grid a fixed `List<Product>` to render.
For ten rows this is fine; Grid will happily handle this size in memory. (For a
larger or paged source, you'd pass a `DataProvider` instead — that's Chapter 4.)

**`columnFor(Product::sku) { setHeader("SKU") }`** adds a column. The first
argument is a Kotlin property reference — `Product::sku` — so the Grid knows
which field to render and what to sort on without any string-typed
property-name shenanigans. Pass `Product::sku` and you get type-safe column
definitions; rename `sku` in the data class and the compiler tells you which
columns need to follow.

**`flexGrow = 1`** on the *Name* column tells the Grid to give it any leftover
horizontal space. The other columns size to their content; Name expands to fill.

**No more `Notification`.** Compare this view to Chapter 1: there's no event
handler anywhere. The Grid wires itself to the data on construction; clicks,
hovers and sorts are handled by Vaadin without you writing a single listener.
You'll add a click listener in Chapter 6 when we start editing rows — for now,
this is a read-only catalog.

## A note on the rename

We renamed `WelcomeView` → `CatalogView` because the file no longer welcomes
anyone; it shows the catalog. The `@Route("")` annotation still binds the view
to the URL root, so the address bar doesn't change.

This is the only chapter that renames a file. From here on, `CatalogView` is the
single screen we keep enriching.

In the next chapter we'll move `sampleProducts` from a Kotlin `val` into a real
database — a Flyway migration plus a ktorm entity, accessed through a finder
method. **Chapter 3 is the heaviest one in the tutorial; budget more time for it
than the others.** After it, the rest of the chapters are short feature additions
on top of a stable foundation.

# Chapter 3 — Persisting to the database

> **Heads up.** This is the longest chapter in the tutorial. It introduces three
> things at once — Flyway migrations, the ktorm `Entity` model, and the difference
> between a Kotlin `data class` and a database-bound entity — because they only
> really make sense together. Take it in two passes if you need to.

In Chapter 2 we hardcoded a list of ten products in Kotlin. The starter project
already has H2, Flyway, and ktorm on the classpath; we just haven't used any of
them. In this chapter we will:

1. Write a **Flyway migration** that creates a `Product` table and inserts the
   same ten rows we had hardcoded.
2. Promote `Product` from a Kotlin `data class` to a **ktorm entity**.
3. Replace the hardcoded list in `CatalogView` with a call to
   `Products.findAll()`.

When you're done, the app boots, Flyway brings the database up to date, ktorm
reads the rows, and the Grid renders exactly the same screen as before — except
the data now lives in a database and can be modified at runtime. The rest of the
tutorial is built on this foundation.

## What's already wired

Open `Bootstrap.kt`. The constructor of every web app has to do *something*
before serving the first request, and ours does three relevant things:

```kotlin
VaadinOnKotlin.dataSource = HikariDataSource(cfg)
VaadinOnKotlin.init()

val flyway = Flyway.configure()
    .dataSource(VaadinOnKotlin.dataSource)
    .load()
flyway.migrate()
```

- **`VaadinOnKotlin.dataSource = ...`** is an extension property from the
  `vok-framework-vokdb` module. Setting it makes a Hikari-pooled JDBC data
  source available to ktorm via the `db { ... }` helper — that's the same data
  source ktorm queries will use.
- **`VaadinOnKotlin.init()`** initialises the VoK framework around it.
- **`flyway.migrate()`** scans `src/main/resources/db/migration/` for SQL files
  named `V<n>__<description>.sql` and applies any that the database hasn't yet
  seen.

The starter contains no migration files, so today Flyway runs on an empty
database and does nothing. Let's give it one.

## Step 1 — write the migration

Create `src/main/resources/db/migration/V1__create_product.sql` with:

```sql
CREATE TABLE Product(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(20) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  stock INTEGER NOT NULL,
  unit VARCHAR(20) NOT NULL
);

INSERT INTO Product (sku, name, category, price, stock, unit) VALUES
  ('HX-M6-40',      'Hex bolt M6×40mm, zinc-plated',    'Fasteners',   0.35, 120, 'Each'),
  ('HX-M8-50',      'Hex bolt M8×50mm, stainless',      'Fasteners',   0.85,   0, 'Each'),
  ('NT-M6-BOX100',  'Hex nut M6, zinc-plated, 100/box', 'Fasteners',   4.50,  18, 'Box'),
  ('WD40-400',      'WD-40 lubricant spray 400ml',      'Tools',       7.90,  32, 'Each'),
  ('SD-FLAT-6',     'Screwdriver, flat blade 6mm',      'Tools',       6.50,  12, 'Each'),
  ('PIPE-CU-22',    'Copper pipe Ø22mm',                'Plumbing',   12.40,  45, 'Meter'),
  ('CABLE-3G15',    'Power cable 3G1.5mm²',             'Electrical',  1.80, 200, 'Meter'),
  ('PAINT-WHT-1L',  'Interior paint, white matte 1L',   'Paint',      14.90,   8, 'Each'),
  ('PAINT-WHT-10L', 'Interior paint, white matte 10L',  'Paint',      89.00,   3, 'Each'),
  ('SAND-CONCRETE', 'Concrete sand',                    'Garden',      0.45, 800, 'Kilogram');
```

A few notes about the schema:

- The **file name `V1__create_product.sql`** matters. Flyway parses the version
  (`1`), a separator (`__`, two underscores), and a description (`create_product`).
  When you add the next migration, name it `V2__...sql`. Don't rename or edit a
  migration that has already been applied — Flyway records a checksum and will
  refuse to run the suite again if a previous file changed.
- `DECIMAL(10, 2)` matches the `BigDecimal` type we used in Kotlin. Ten digits
  total, two after the decimal point — enough for any retail price you'll ever
  meet in a hardware shop.
- The `category` and `unit` columns are `VARCHAR`s storing the enum's `name`
  (`'Fasteners'`, `'Box'`, etc.). ktorm's `enum<E>("col")` binding reads and
  writes them as strings.
- `id BIGINT AUTO_INCREMENT PRIMARY KEY` gives every row a surrogate key. The
  `sku` is a natural unique identifier as well, so it's `UNIQUE` — both will be
  useful later (for example, the REST API in Chapter 11 will look products up
  by SKU).
- The migration writes the table name as unquoted `Product`. H2 would normally
  uppercase unquoted identifiers at parse time, so the actual table would land
  as `PRODUCT` — meanwhile ktorm-vaadin uses the PostgreSQL dialect and quotes
  identifiers in every query (`SELECT … FROM "Product"`), and quoting is
  case-sensitive. The `;DATABASE_TO_UPPER=FALSE` already on the H2 URL in
  `Bootstrap.kt` is what keeps the two sides aligned; on a real PostgreSQL or
  MySQL backend the concern doesn't arise.

For a tutorial, mixing DDL and seed data in a single migration is convenient.
In a real project you'd typically separate the two — schema in `V1__...`,
reference data in `V2__...` — so that schema changes can be replayed against a
production database that already has rows.

## Step 2 — promote Product to a ktorm entity

Replace `Product.kt` with:

```kotlin
package com.example.vok

import com.github.mvysny.ktormvaadin.ActiveEntity
import org.ktorm.entity.Entity
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import org.ktorm.schema.decimal
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import java.math.BigDecimal

enum class Category { Tools, Fasteners, Plumbing, Electrical, Paint, Garden }

enum class UnitOfMeasure { Each, Box, Meter, Kilogram }

interface Product : ActiveEntity<Product> {
    var id: Long?
    var sku: String?
    var name: String?
    var category: Category?
    var price: BigDecimal?
    var stock: Int?
    var unit: UnitOfMeasure?

    override val table: Table<Product> get() = Products

    companion object : Entity.Factory<Product>()
}

object Products : Table<Product>("Product") {
    val id: Column<Long> = long("id").primaryKey().bindTo { it.id }
    val sku: Column<String> = varchar("sku").bindTo { it.sku }
    val name: Column<String> = varchar("name").bindTo { it.name }
    val category: Column<Category> = enum<Category>("category").bindTo { it.category }
    val price: Column<BigDecimal> = decimal("price").bindTo { it.price }
    val stock: Column<Int> = int("stock").bindTo { it.stock }
    val unit: Column<UnitOfMeasure> = enum<UnitOfMeasure>("unit").bindTo { it.unit }
}
```

The shape changed a lot. Here's why each piece looks the way it does.

### `interface Product`, not `class Product`

ktorm entities are **interfaces**, not classes. The runtime implementation is
generated for you when you call `Entity.create<Product>()` (or use the
`Entity.Factory` companion). The interface only declares the property *shape*;
ktorm tracks loaded values and dirty fields behind the scenes.

This is unfamiliar if you've used JPA or other "POJO + annotations" ORMs, but
it's deliberate: ktorm keeps the entity definition declarative, and gives you a
clean change-tracking model for free (the `flushChanges()` call we'll meet in
Chapter 6 uses it).

### Why every property is nullable

In Chapter 2 the `data class` had non-nullable fields. The ktorm interface flips
that: every property is `Type?`.

This is a consequence of how ktorm models partial entities. When you create a
fresh `Product()` with `Entity.create<Product>()`, none of its fields are set
yet — you fill them in one at a time before calling `save()`. The Kotlin
compiler can't tell which of those steps has happened, so the property type has
to be nullable for the model to work at all.

The database **still** enforces `NOT NULL` on every column. At save time, if
you forget to set `name`, the JDBC driver throws a constraint violation —
loudly, and at the right moment.

In Chapter 8 we'll layer JSR-303 validation annotations (`@get:NotNull`,
`@get:Size`, ...) on top of the entity, which moves these checks from the
database round-trip to a Kotlin validator running inside `Binder` — so the user
sees inline form errors instead of a stack trace.

### `ActiveEntity<Product>` vs `Entity<Product>`

ktorm gives you the bare `Entity<E>` superinterface. The `vok-db` module — via
`ktorm-vaadin` — wraps it as `ActiveEntity<E>` and adds `save()`, `create()`,
`delete()`, `validate()`, and a few more conveniences. Using `ActiveEntity`
costs nothing if you don't need the extras and pays off the moment you do; the
tutorial uses it throughout.

### The `Products` Table object

`Products` is the schema half of the picture. Each `varchar("...")` /
`decimal("...")` / `enum<E>("...")` call declares a column with a SQL name, and
`bindTo { it.sku }` (etc.) wires it to the corresponding entity property. The
property reference is type-checked — if you rename `sku` in the interface, the
binding stops compiling.

The object is named in the **plural** (`Products`), and the SQL table name is
passed to the `Table` constructor as `"Product"` (singular). This is a ktorm
convention — the table is named after a single row of data; the object that
represents the table is named after the collection.

## Step 3 — use the finder in the view

Update `CatalogView.kt` to read rows from the database instead of the hardcoded
list:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.ktormvaadin.findAll
import com.vaadin.flow.router.Route

@Route("")
class CatalogView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")
            grid<Product>(Product::class) {
                setSizeFull()
                setItems(Products.findAll())
                columnFor(Product::sku) { setHeader("SKU") }
                columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                columnFor(Product::category) { setHeader("Category") }
                columnFor(Product::price) { setHeader("Price") }
                columnFor(Product::stock) { setHeader("Stock") }
                columnFor(Product::unit) { setHeader("Unit") }
            }
        }
    }
}
```

The Chapter-2 `private val sampleProducts = listOf(...)` block is **deleted
entirely**. The only line that does the work of producing rows is
`Products.findAll()`, an extension function from `com.github.mvysny.ktormvaadin`
that returns `List<Product>`.

`findAll()` is convenient on a tutorial-scale dataset of ten rows. On a real
table with millions of products you would not call it — instead, you'd hand the
Grid a *streaming* `DataProvider` that fetches a page at a time. That's the
shift we'll make in Chapter 4, where we also add a search field.

## Run it

Stop the server if it's still running and restart it:

```bash
$ ./gradlew run
```

Watch the startup log. You should see, in order:

```
Hikari ... Start completed.
Vaadin On Kotlin initialized
Running DB migrations
Flyway ... Migrating schema "PUBLIC" to version "1 - create product"
Flyway ... Successfully applied 1 migration to schema "PUBLIC", now at version v1
Initialization complete
Embedded Jetty started successfully on http://localhost:8080
```

Reload <http://localhost:8080>. The Grid looks identical to Chapter 2 — same
ten rows, same columns — but it's now reading from H2 via ktorm. To prove it,
open `Product.kt`, change `enum class Category { Tools, ... }` to add `Misc` at
the end (a new value the database knows nothing about), restart, and the app
still loads — because no row uses `Misc`. Now change one of the seed rows in
`V1__create_product.sql`. Restart. The Grid **does not** show your change:
Flyway has already recorded version `1` as applied and refuses to run the
migration again.

That refusal is the whole point. Production migrations are run once, in order,
and never edited. To make a change to seeded data after V1 is in production
you'd add a V2:

```sql
-- V2__rename_paint.sql
UPDATE Product SET name = 'Interior paint, eggshell 1L' WHERE sku = 'PAINT-WHT-1L';
```

For now you can drop the H2 database by restarting — it's `mem:test`, so the
schema lives only in the JVM process. Stop the server, restart, and Flyway
re-applies V1 from scratch.

## Where we are

You now have:

- a real schema, in a real migration file, applied by Flyway on startup;
- a ktorm entity bound to that schema, with `save()`, `delete()`, and
  `validate()` already available (we'll start using them in Chapter 6);
- a Grid backed by a database query.

Compared to Chapter 2, exactly one line changed in the view —
`setItems(sampleProducts)` became `setItems(Products.findAll())`. The
interesting work moved to `Product.kt` and `V1__create_product.sql`. That
separation is the point: **the view doesn't know or care where its rows come
from.** In the next chapter we'll exploit that by replacing `findAll()` with a
reactive `DataProvider` and adding a live search field above the Grid.

# Chapter 4 — Live filtering

`Products.findAll()` loaded every row into memory and handed the list to the
Grid. That's fine for ten products and bad for ten thousand: the entire table
gets shipped to the browser regardless of what the user is actually looking at.
Vaadin's `DataProvider` abstraction fixes that. The Grid asks the provider for
the rows it needs *right now* (which page, what filter, what sort), and the
provider issues a matching SQL query.

In this chapter we'll switch the Grid to ktorm-vaadin's `EntityDataProvider`
and put a search field above it. Each keystroke (after a short debounce) sends
a new filter to the provider, which re-runs the query and the Grid repopulates.

## Step 1 — swap `findAll()` for a `DataProvider`

Edit `CatalogView.kt` so it reads:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.ktormvaadin.dataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import org.ktorm.dsl.or
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike

@Route("")
class CatalogView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")

            val dp = Products.dataProvider

            textField {
                placeholder = "Search by name or SKU"
                valueChangeMode = ValueChangeMode.LAZY
                setWidth("20em")
                addValueChangeListener { e ->
                    dp.setFilter(productFilter(e.value))
                }
            }

            grid<Product>(Product::class, dp) {
                setSizeFull()
                columnFor(Product::sku) { setHeader("SKU") }
                columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                columnFor(Product::category) { setHeader("Category") }
                columnFor(Product::price) { setHeader("Price") }
                columnFor(Product::stock) { setHeader("Stock") }
                columnFor(Product::unit) { setHeader("Unit") }
            }
        }
    }
}

private fun productFilter(query: String): ColumnDeclaring<Boolean>? {
    val q = query.trim()
    if (q.isEmpty()) return null
    val pattern = "%$q%"
    return Products.name.ilike(pattern) or Products.sku.ilike(pattern)
}
```

Restart the app and reload <http://localhost:8080>. The Grid still shows ten
rows. Type `paint` into the search field; after a heartbeat, the Grid drops to
the two paint products. Clear the field; ten rows reappear. Try `HX` — two hex
bolts. Try `Garden` — that's a category, not a name or SKU, so you get zero
results (Chapter 5 adds a category filter).

## What changed, piece by piece

**`Products.dataProvider`** is an extension property from
`com.github.mvysny.ktormvaadin` that returns an `EntityDataProvider<Product>`.
That class implements Vaadin's `DataProvider` interface using ktorm under the
hood: when the Grid asks for rows, it runs `SELECT * FROM Product WHERE ...
LIMIT ? OFFSET ?` and converts each row to a `Product` entity. There is no
in-memory cache — each user interaction that needs fresh data triggers a query.

**`grid<Product>(Product::class, dp) { ... }`** is a different Karibu-DSL
overload than the one we've used so far. The second argument is the data
provider; the Grid wires itself to it on construction. We no longer call
`setItems(...)` inside the builder — the rows arrive lazily from the provider.

**`ValueChangeMode.LAZY`** controls when the `TextField` fires its value-change
event. The default fires on blur, which would make a search field feel
sluggish — you'd have to tab out before anything happens. `LAZY` debounces
keystrokes (default 400 ms) and fires once the user pauses typing. That's the
right feel for live search: responsive but not frantic.

**`dp.setFilter(productFilter(e.value))`** is what makes the search reactive.
`EntityDataProvider.setFilter` takes a ktorm boolean expression (or `null` to
clear the filter) and calls `refreshAll()` internally, which makes the Grid
re-query through the provider with the new filter applied. So:

1. User types a keystroke.
2. `LAZY` debounce expires, `TextField` fires `ValueChangeEvent`.
3. Our listener calls `dp.setFilter(...)` with a fresh ktorm WHERE expression.
4. `EntityDataProvider` calls `refreshAll()` → Grid asks for rows again.
5. ktorm builds and runs `SELECT * FROM Product WHERE (name ILIKE ?) OR (sku
   ILIKE ?) ORDER BY ... LIMIT ? OFFSET ?` against H2.

**`productFilter(query)`** is the only domain logic. Trim, exit early on empty,
build a `LIKE` pattern with leading and trailing `%` so the match is
substring-anywhere, then return `Products.name.ilike(pattern) or
Products.sku.ilike(pattern)`. That return type is `ColumnDeclaring<Boolean>?` —
ktorm's algebra of WHERE-clause expressions, type-checked at compile time.

> **Why `ilike` and not `like`?** ILIKE is the case-insensitive variant; we want
> searching for `paint` to match `Paint`. It's a PostgreSQL extension, but
> ktorm-vaadin configures the PostgreSQL dialect over H2 by default and H2's
> SQL grammar accepts `ILIKE` natively, so the same code runs against either
> database. The import path is
> `org.ktorm.support.postgresql.ilike` — a small reminder of where it came
> from.

## Why this approach scales

The view does no in-memory filtering. There is no `list.filter { ... }` call,
no client-side data slicing, no `List<Product>` held in the session. Every
visible row is the result of a fresh query whose `WHERE` is built from the
current state of the search field. Scaling from ten products to ten thousand
to ten million changes nothing in this file — the SQL just runs against more
rows, with a `LIMIT 50` keeping each page's payload small.

In the next chapter we'll add a second filter — a `ComboBox<Category>` — and
combine it with the search field. The trick we'll need is to build a
`productFilter()` that ANDs both inputs together; the underlying mechanism is
exactly what you just wrote.

# Chapter 5 — Category filter

Searching by SKU and name is useful when the user knows what they're looking
for. Just as often, they want to *browse* — "show me everything in Plumbing".
We'll add a `ComboBox<Category>` next to the search field and combine both
filters so they narrow the result together.

This chapter is short. The plumbing (`DataProvider`, `setFilter`,
`productFilter`) is already in place from Chapter 4; we're adding one new
field, expanding `productFilter` to take two arguments, and wrapping the
toolbar in a `horizontalLayout` so the controls sit side by side.

## The whole change

Update `CatalogView.kt` to:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.ktormvaadin.dataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.or
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike

@Route("")
class CatalogView : KComposite() {
    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")

            val dp = Products.dataProvider

            horizontalLayout {
                val searchField = textField {
                    placeholder = "Search by name or SKU"
                    valueChangeMode = ValueChangeMode.LAZY
                    setWidth("20em")
                }
                val categoryField = comboBox<Category> {
                    placeholder = "Category"
                    setItems(Category.entries)
                    setWidth("12em")
                    isClearButtonVisible = true
                }

                fun applyFilters() {
                    dp.setFilter(productFilter(searchField.value, categoryField.value))
                }
                searchField.addValueChangeListener { applyFilters() }
                categoryField.addValueChangeListener { applyFilters() }
            }

            grid<Product>(Product::class, dp) {
                setSizeFull()
                columnFor(Product::sku) { setHeader("SKU") }
                columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                columnFor(Product::category) { setHeader("Category") }
                columnFor(Product::price) { setHeader("Price") }
                columnFor(Product::stock) { setHeader("Stock") }
                columnFor(Product::unit) { setHeader("Unit") }
            }
        }
    }
}

private fun productFilter(search: String, category: Category?): ColumnDeclaring<Boolean>? {
    val s = search.trim()
    val parts = listOfNotNull(
        if (s.isEmpty()) null else {
            val pattern = "%$s%"
            Products.name.ilike(pattern) or Products.sku.ilike(pattern)
        },
        category?.let { Products.category eq it },
    )
    return parts.reduceOrNull { a, b -> a and b }
}
```

Restart and reload. Pick **Plumbing** from the combo box: the Grid shows only
the copper pipe. Type `M6` into the search field on top of that: the Grid
empties (no plumbing item has `M6` in its name or SKU). Clear the search:
plumbing returns. Clear the combo box: all ten rows come back.

## What's new

**`comboBox<Category>(...)`** is the Karibu-DSL builder for Vaadin's
`ComboBox`. The type parameter and `setItems(Category.entries)` together give
us a dropdown whose items are exactly the six enum constants
(`Category.entries` is the Kotlin 1.9+ replacement for `values()` — a real
`List<Category>` rather than an array).

**`isClearButtonVisible = true`** puts a small × on the right edge of the
combo box so the user can clear the selection with one click. Without it, the
only way to drop a category filter is to scroll back to "nothing", which most
UI patterns don't even provide. Always set this on filters.

**`horizontalLayout { ... }`** wraps the two filter fields. Both controls live
inside a single horizontal row above the Grid. Children of `horizontalLayout`
are spaced and laid out left-to-right by default; for the kind of toolbar we
need here that's already the right look.

**`fun applyFilters()`** is a *local* function defined inside the layout
builder. It reads each field's current value and calls
`dp.setFilter(productFilter(...))`. Both fields' value-change listeners point
at the same `applyFilters()`, so whichever field the user touches, the same
combined WHERE clause gets rebuilt and pushed to ktorm.

Local functions like this are a small but real Kotlin lever — they let us
capture both `searchField` and `categoryField` (and `dp`) by reference,
without lifting them to class fields, and without resorting to `lateinit`.

## The expanded `productFilter()`

`productFilter` now takes two arguments and returns either `null` (no filter,
show everything) or a ktorm boolean expression:

```kotlin
private fun productFilter(search: String, category: Category?): ColumnDeclaring<Boolean>? {
    val s = search.trim()
    val parts = listOfNotNull(
        if (s.isEmpty()) null else {
            val pattern = "%$s%"
            Products.name.ilike(pattern) or Products.sku.ilike(pattern)
        },
        category?.let { Products.category eq it },
    )
    return parts.reduceOrNull { a, b -> a and b }
}
```

`listOfNotNull(...)` drops any `null` clauses — that's how each individual
filter "turns itself off" when the user has nothing in its field.
`reduceOrNull { a, b -> a and b }` ANDs the surviving clauses together, or
returns `null` if the list is empty (i.e. no filters at all). The combination
generates SQL like:

```sql
SELECT * FROM Product
WHERE ((name ILIKE ?) OR (sku ILIKE ?)) AND (category = ?)
LIMIT ? OFFSET ?
```

If we needed a third filter — say, a low-stock checkbox — adding it would be
two lines: one new field, one new entry in the `listOfNotNull`. The closure of
each filter is independent of the others, and `productFilter` is the single
place that assembles them. That's the structural pattern, and we'll reuse it
in Chapter 9 when we add the "Low stock only" toggle.

## Note on `addValueChangeListener` vs the inline form

Compare this chapter's listeners:

```kotlin
val searchField = textField { ... }
val categoryField = comboBox<Category> { ... }
searchField.addValueChangeListener { applyFilters() }
categoryField.addValueChangeListener { applyFilters() }
```

to what we did in Chapter 4:

```kotlin
textField {
    ...
    addValueChangeListener { e -> dp.setFilter(productFilter(e.value)) }
}
```

Both are valid. The Chapter-4 form attaches the listener inline inside the
builder lambda, which is fine when the listener only reads its own field's
value. As soon as the listener needs to read *another* field's value too — as
ours does now — we have to first capture both fields into vals (because the
builder lambda has no access to a sibling that hasn't been declared yet) and
attach the listeners afterwards.

In the next chapter we'll add a side panel that edits the row the user
selects in the Grid — and for that we'll meet Vaadin's `Binder`, which is the
component that ties form fields to the properties of an entity.

# Chapter 6 — Editing the selected product

So far the catalog is read-only. In this chapter we'll turn it into a
master-detail editor: clicking a row in the Grid pulls that product into a
form on the right; the user changes a field, hits **Save**, and the row
updates. **Delete** removes the row. The form is built once and reused for
every row the user picks.

Two new ideas show up here:

- **Vaadin's `Binder`** — the bridge between an entity and a form. It reads
  values out of a bean into matching fields, validates input on its way back,
  and writes changes back to the bean.
- **Master-detail layout** — Grid on the left, form on the right, both
  inside a `horizontalLayout` that spans the available width.

## The whole new `CatalogView.kt`

Replace `CatalogView.kt` with:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.github.mvysny.ktormvaadin.dataProvider
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.or
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike

@Route("")
class CatalogView : KComposite() {

    private val dp = Products.dataProvider
    private val binder = Binder<Product>(Product::class.java)
    private var selected: Product? = null
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")

            horizontalLayout {
                val searchField = textField {
                    placeholder = "Search by name or SKU"
                    valueChangeMode = ValueChangeMode.LAZY
                    setWidth("20em")
                }
                val categoryField = comboBox<Category> {
                    placeholder = "Category"
                    setItems(Category.entries)
                    setWidth("12em")
                    isClearButtonVisible = true
                }

                fun applyFilters() {
                    dp.setFilter(productFilter(searchField.value, categoryField.value))
                }
                searchField.addValueChangeListener { applyFilters() }
                categoryField.addValueChangeListener { applyFilters() }
            }

            horizontalLayout {
                setSizeFull(); isSpacing = true

                grid<Product>(Product::class, dp) {
                    flexGrow = 1.0
                    setSizeFull()
                    columnFor(Product::sku) { setHeader("SKU") }
                    columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                    columnFor(Product::category) { setHeader("Category") }
                    columnFor(Product::price) { setHeader("Price") }
                    columnFor(Product::stock) { setHeader("Stock") }
                    columnFor(Product::unit) { setHeader("Unit") }

                    asSingleSelect().addValueChangeListener { e -> showSelection(e.value) }
                }

                verticalLayout {
                    setWidth("28em"); isPadding = true; isSpacing = true

                    h3("Product details")

                    textField("SKU") { bind(binder).bind(Product::sku) }
                    textField("Name") { bind(binder).bind(Product::name) }
                    comboBox<Category>("Category") {
                        setItems(Category.entries)
                        bind(binder).bind(Product::category)
                    }
                    bigDecimalField("Price") { bind(binder).bind(Product::price) }
                    integerField("Stock") { bind(binder).bind(Product::stock) }
                    comboBox<UnitOfMeasure>("Unit") {
                        setItems(UnitOfMeasure.entries)
                        bind(binder).bind(Product::unit)
                    }

                    horizontalLayout {
                        saveButton = button("Save") {
                            setPrimary()
                            onClick { onSave() }
                        }
                        deleteButton = button("Delete") {
                            onClick { onDelete() }
                        }
                    }
                }
            }
        }
    }

    init {
        showSelection(null)
    }

    private fun showSelection(product: Product?) {
        selected = product
        binder.readBean(product)
        saveButton.isEnabled = product != null
        deleteButton.isEnabled = product != null
    }

    private fun onSave() {
        val product = selected ?: return
        if (!binder.writeBeanIfValid(product)) return
        product.save()
        dp.refreshAll()
        Notification.show("Saved ${product.name}")
    }

    private fun onDelete() {
        val product = selected ?: return
        product.delete()
        dp.refreshAll()
        showSelection(null)
        Notification.show("Deleted ${product.name}")
    }
}

private fun productFilter(search: String, category: Category?): ColumnDeclaring<Boolean>? {
    val s = search.trim()
    val parts = listOfNotNull(
        if (s.isEmpty()) null else {
            val pattern = "%$s%"
            Products.name.ilike(pattern) or Products.sku.ilike(pattern)
        },
        category?.let { Products.category eq it },
    )
    return parts.reduceOrNull { a, b -> a and b }
}
```

Restart the app. The view now has the Grid on the left and an empty form on
the right with **Save** and **Delete** disabled. Click any row; the form
populates with that product's values, the buttons enable. Change the price,
click **Save** — the Grid cell updates and a "Saved …" toast appears. Click
**Delete** — the row vanishes, the form clears, the buttons disable again.

## What changed structurally

This chapter is the first to introduce **class-level fields** on `CatalogView`.
Up to now everything lived inside the `ui { ... }` builder; now we have:

```kotlin
private val dp = Products.dataProvider
private val binder = Binder<Product>(Product::class.java)
private var selected: Product? = null
private lateinit var saveButton: Button
private lateinit var deleteButton: Button
```

These exist because the save/delete handlers and the `showSelection` helper
need to reach across the view from outside the builder lambda. Pulling them
out as fields makes the dependencies obvious: the binder owns form ↔ entity
translation, `selected` remembers which row is being edited, the buttons need
to be toggled, and `dp` needs to be refreshed after each persistence call.

`lateinit` is used only for the two `Button` references because they are
assigned **during** the `root = ui { ... }` initialiser — the buttons don't
exist until the builder runs. Everything else can be initialised at field
declaration time.

The `init { showSelection(null) }` block runs after the builder finishes
constructing the UI, with `saveButton` and `deleteButton` already assigned —
so the very first thing the user sees is a properly cleared form with the
buttons disabled.

## The split layout

The body is wrapped in a second `horizontalLayout`:

```kotlin
horizontalLayout {
    setSizeFull(); isSpacing = true

    grid<Product>(Product::class, dp) {
        flexGrow = 1.0
        ...
    }

    verticalLayout {
        setWidth("28em"); ...
    }
}
```

- **`setSizeFull()`** makes the body fill the remaining vertical space
  inside `CatalogView`.
- **`grid { flexGrow = 1.0 }`** tells the flexbox layout to give the Grid all
  the spare horizontal space.
- **`verticalLayout { setWidth("28em") }`** fixes the side panel's width to
  28 em — wide enough for the labels and inputs, narrow enough not to crowd
  the Grid.

## Selecting a row

```kotlin
asSingleSelect().addValueChangeListener { e -> showSelection(e.value) }
```

A Grid can be configured for single-row, multi-row or no selection. The
`asSingleSelect()` view exposes a `HasValue<..., T?>` whose value is the
currently-selected row (`null` if none). Subscribing to its value-change event
gives us a single hook for "the selection changed". Inside the listener we
call `showSelection`, which both **remembers** the row (so we can save or
delete it later) and **populates the form** via the binder.

## Meet `Binder`

`Binder<Product>` is the centrepiece of Vaadin's form story. It does three
things:

1. **Reads** values out of a bean into form fields.
2. **Writes** values back into a bean from form fields.
3. **Validates** the input on its way back.

We create one instance for the whole side panel:

```kotlin
private val binder = Binder<Product>(Product::class.java)
```

…and bind each form field to a property:

```kotlin
textField("SKU") { bind(binder).bind(Product::sku) }
textField("Name") { bind(binder).bind(Product::name) }
comboBox<Category>("Category") {
    setItems(Category.entries)
    bind(binder).bind(Product::category)
}
bigDecimalField("Price") { bind(binder).bind(Product::price) }
integerField("Stock") { bind(binder).bind(Product::stock) }
comboBox<UnitOfMeasure>("Unit") {
    setItems(UnitOfMeasure.entries)
    bind(binder).bind(Product::unit)
}
```

A few details worth unpacking:

- **`bind(binder)`** is a Karibu-DSL extension on any field (`HasValue`). It
  starts a fluent binding: *"this field will be bound to a property of the
  bean type, via this binder"*. Calling `.bind(Product::sku)` finalises the
  binding by naming the property — and because it takes a `KMutableProperty1`
  rather than a string, the compiler enforces that the field's type
  (`String?` for `TextField`) matches the property's type. Rename `sku` in
  the `Product` interface and this line stops compiling.

- **Typed numeric fields.** `bigDecimalField` and `integerField` are Vaadin's
  strongly-typed numeric inputs. `bigDecimalField` reads/writes `BigDecimal?`
  directly — no `String → BigDecimal` converter needed. Likewise
  `integerField` returns `Int?`. We could have used a plain `textField` with
  `.withConverter(...)` but the typed variants are clearer and tighter to
  read.

## Read, then write back

```kotlin
private fun showSelection(product: Product?) {
    selected = product
    binder.readBean(product)
    saveButton.isEnabled = product != null
    deleteButton.isEnabled = product != null
}
```

`binder.readBean(product)` **copies** each bound property from the bean into
its form field. Passing `null` clears all fields. After this call, the user
can edit fields freely — those edits **do not** propagate back to `product`
yet.

```kotlin
private fun onSave() {
    val product = selected ?: return
    if (!binder.writeBeanIfValid(product)) return
    product.save()
    dp.refreshAll()
    Notification.show("Saved ${product.name}")
}
```

`binder.writeBeanIfValid(product)` validates every binding, and only if all
of them pass does it write the field values back into the bean. We're using
no validators yet (Chapter 8 adds JSR-303 annotations), so this is currently
"always-true" — but it's still the right call to wire up now.

`product.save()` comes from `ActiveEntity` (Chapter 3): it asks ktorm to
flush the entity's dirty properties to the database. `dp.refreshAll()` makes
the Grid re-read its data so the visible row reflects the change.

> **Why `readBean` + `writeBean` and not `setBean`?**
> `Binder` has a third mode — `setBean(bean)` — that wires the bean to the
> form bidirectionally: edits go straight into the bean as the user types.
> It's convenient when you have no Save button (auto-save). For an
> explicit-save form like ours, `readBean` + `writeBean` is better: it lets
> the user change rows without confirming, and discards in-flight edits.

## Delete

```kotlin
private fun onDelete() {
    val product = selected ?: return
    product.delete()
    dp.refreshAll()
    showSelection(null)
    Notification.show("Deleted ${product.name}")
}
```

`Entity.delete()` is built into ktorm — it issues `DELETE FROM Product WHERE
id = ?`. After deleting we refresh the provider and clear the selection so
the form returns to its empty state.

> **A small UX caveat.** There is no confirmation dialog yet, so a misclick
> on **Delete** loses the row. A polished app would pop a "Delete *Hex bolt
> M6×40mm*? This cannot be undone." confirmation. Chapter 7 introduces
> `Dialog`; you'll have everything you need to add a confirm after that.

## What we haven't done (yet)

- **No validation.** The form happily accepts a blank `name`, a negative
  `stock`, a `price` of `0`. Chapter 8 fixes this with JSR-303 annotations.
- **No add flow.** The Save button only updates existing rows. Chapter 7
  adds a **+ Add product** button that opens the same form inside a
  `Dialog`, reusing the binder we just built.
- **No low-stock cue.** The Grid shows the raw stock number even when it
  reaches zero. Chapter 9 introduces a custom `ComponentRenderer` and adds a
  red **"Low stock"** badge.

So far so good — we have a working master-detail catalog. In the next
chapter we'll factor the form fields into a reusable `KComposite` and add
the add-product flow on top of it.

# Chapter 7 — Adding new products

The shopkeeper can edit and delete products, but every row in the catalog is
seeded by Flyway — there is no way to add a new one through the UI. In this
chapter we'll fix that. The new pieces are:

- A reusable **`ProductForm`** — a `KComposite` containing the six fields
  we built in Chapter 6, with its own `Binder<Product>`. Both the side panel
  and the new dialog will use it; no field declarations are duplicated.
- A **`+ Add product`** button in the toolbar that opens a Vaadin **`Dialog`**
  containing a fresh `ProductForm`. Submitting the dialog inserts a row.

## Step 1 — extract `ProductForm`

Create `src/main/kotlin/com/example/vok/ProductForm.kt`:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.data.binder.Binder

class ProductForm : KComposite() {

    val binder: Binder<Product> = Binder(Product::class.java)

    private val root = ui {
        formLayout {
            textField("SKU") { bind(binder).bind(Product::sku) }
            textField("Name") { bind(binder).bind(Product::name) }
            comboBox<Category>("Category") {
                setItems(Category.entries)
                bind(binder).bind(Product::category)
            }
            bigDecimalField("Price") { bind(binder).bind(Product::price) }
            integerField("Stock") { bind(binder).bind(Product::stock) }
            comboBox<UnitOfMeasure>("Unit") {
                setItems(UnitOfMeasure.entries)
                bind(binder).bind(Product::unit)
            }
        }
    }
}

fun (@VaadinDsl HasComponents).productForm(block: (@VaadinDsl ProductForm).() -> Unit = {}): ProductForm =
    init(ProductForm(), block)
```

The class itself is small: it wraps a `FormLayout` (Vaadin's two-column
label-on-the-left form layout) and exposes its `Binder<Product>` as a public
property so the parent screen can call `readBean` / `writeBeanIfValid`. The
six binding lines are unchanged from Chapter 6 — they just moved.

The `productForm(block)` function at the bottom is the **Karibu-DSL builder**.
Following the same convention as `textField`, `button`, `grid` and friends,
it's an extension on `HasComponents` that constructs the composite, runs the
caller's configuration lambda against it, and adds it to the surrounding
layout — all in one call.

> **About that warning.** If you keep the `@VaadinDsl` annotation on the
> top-level function declaration (you'll see it in some older VoK code) the
> Kotlin compiler emits a warning: DSL marker annotations only have effect
> on types, not on functions. The annotations *inside* the function
> signature — `(@VaadinDsl HasComponents)` and `(@VaadinDsl ProductForm)` —
> do real work, so keep those.

## Step 2 — use the form in two places

Rewrite `CatalogView.kt`:

```kotlin
package com.example.vok

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.github.mvysny.ktormvaadin.dataProvider
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.or
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.support.postgresql.ilike

@Route("")
class CatalogView : KComposite() {

    private val dp = Products.dataProvider
    private lateinit var sidePanelForm: ProductForm
    private var selected: Product? = null
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private val root = ui {
        verticalLayout {
            setSizeFull(); isPadding = true; isSpacing = true

            h2("BoltShop catalog")

            horizontalLayout {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.END

                val searchField = textField {
                    placeholder = "Search by name or SKU"
                    valueChangeMode = ValueChangeMode.LAZY
                    setWidth("20em")
                }
                val categoryField = comboBox<Category> {
                    placeholder = "Category"
                    setItems(Category.entries)
                    setWidth("12em")
                    isClearButtonVisible = true
                }
                button("+ Add product") {
                    setPrimary()
                    onClick { openAddDialog() }
                }

                fun applyFilters() {
                    dp.setFilter(productFilter(searchField.value, categoryField.value))
                }
                searchField.addValueChangeListener { applyFilters() }
                categoryField.addValueChangeListener { applyFilters() }
            }

            horizontalLayout {
                setSizeFull(); isSpacing = true

                grid<Product>(Product::class, dp) {
                    flexGrow = 1.0
                    setSizeFull()
                    columnFor(Product::sku) { setHeader("SKU") }
                    columnFor(Product::name) { setHeader("Name"); flexGrow = 1 }
                    columnFor(Product::category) { setHeader("Category") }
                    columnFor(Product::price) { setHeader("Price") }
                    columnFor(Product::stock) { setHeader("Stock") }
                    columnFor(Product::unit) { setHeader("Unit") }

                    asSingleSelect().addValueChangeListener { e -> showSelection(e.value) }
                }

                verticalLayout {
                    setWidth("28em"); isPadding = true; isSpacing = true

                    h3("Product details")

                    sidePanelForm = productForm()

                    horizontalLayout {
                        saveButton = button("Save") {
                            setPrimary()
                            onClick { onSave() }
                        }
                        deleteButton = button("Delete") {
                            onClick { onDelete() }
                        }
                    }
                }
            }
        }
    }

    init {
        showSelection(null)
    }

    private fun showSelection(product: Product?) {
        selected = product
        sidePanelForm.binder.readBean(product)
        saveButton.isEnabled = product != null
        deleteButton.isEnabled = product != null
    }

    private fun onSave() {
        val product = selected ?: return
        if (!sidePanelForm.binder.writeBeanIfValid(product)) return
        product.save()
        dp.refreshAll()
        Notification.show("Saved ${product.name}")
    }

    private fun onDelete() {
        val product = selected ?: return
        product.delete()
        dp.refreshAll()
        showSelection(null)
        Notification.show("Deleted ${product.name}")
    }

    private fun openAddDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "Add product"

        val form = ProductForm()
        val draft = Product()
        form.binder.readBean(draft)
        dialog.add(form)

        val createButton = Button("Create") {
            if (!form.binder.writeBeanIfValid(draft)) return@Button
            draft.save()
            dp.refreshAll()
            Notification.show("Created ${draft.name}")
            dialog.close()
        }.apply { setPrimary() }
        val cancelButton = Button("Cancel") { dialog.close() }
        dialog.footer.add(cancelButton, createButton)

        dialog.open()
    }
}

private fun productFilter(search: String, category: Category?): ColumnDeclaring<Boolean>? {
    val s = search.trim()
    val parts = listOfNotNull(
        if (s.isEmpty()) null else {
            val pattern = "%$s%"
            Products.name.ilike(pattern) or Products.sku.ilike(pattern)
        },
        category?.let { Products.category eq it },
    )
    return parts.reduceOrNull { a, b -> a and b }
}
```

Restart. The toolbar now has a prominent **+ Add product** button on the
right. Click it: a dialog opens with an empty form. Fill in the fields,
hit **Create**, and the new row appears in the Grid the moment the dialog
closes. Click **Cancel** to discard.

## What changed in `CatalogView`

The class-level `binder` field is **gone**. The side panel now owns its
binder through `ProductForm`, and `CatalogView` reaches it via
`sidePanelForm.binder.readBean(...)` / `.writeBeanIfValid(...)`.

The form fields themselves are gone from `CatalogView` too — they're now a
single call:

```kotlin
sidePanelForm = productForm()
```

Six lines of binding boilerplate became one. Whenever a tutorial reader
later needs a `Product` form somewhere new — a multi-step wizard, a
side-by-side compare view, a bulk-edit popup — they instantiate
`ProductForm()` and they're done.

## The Dialog plumbing

`openAddDialog()` is short but introduces three Vaadin APIs at once:

```kotlin
val dialog = Dialog()
dialog.headerTitle = "Add product"
```

Vaadin's `Dialog` is a modal overlay component. The optional `headerTitle`
puts a heading in the dialog's reserved header slot — no separate `h2`
needed inside the content.

```kotlin
val form = ProductForm()
val draft = Product()
form.binder.readBean(draft)
dialog.add(form)
```

Two things happen here that differ from the side panel. First, we
instantiate `ProductForm` directly (not via the builder) because we don't
have a parent `HasComponents` to add it to yet — we want to add it to the
dialog. Second, the draft entity is created with `Product()`. This invokes
the `Entity.Factory<Product>` `invoke()` operator that ktorm's
`companion object : Entity.Factory<Product>()` declaration gave us in
Chapter 3 — a tiny but important detail. The draft has no `id`; when we
later call `save()` on it, the `ActiveEntity` save path inserts a new row
and the database fills in the `id` via `AUTO_INCREMENT`.

```kotlin
val createButton = Button("Create") {
    if (!form.binder.writeBeanIfValid(draft)) return@Button
    draft.save()
    dp.refreshAll()
    Notification.show("Created ${draft.name}")
    dialog.close()
}.apply { setPrimary() }
val cancelButton = Button("Cancel") { dialog.close() }
dialog.footer.add(cancelButton, createButton)
```

`Dialog.footer.add(...)` puts buttons in the reserved footer slot —
right-aligned, with the small bit of padding the design system expects for
modal actions. Vaadin's `Button(String, ClickListener)` constructor pairs
nicely with Kotlin SAM conversion, so `Button("Create") { ... }` reads
naturally.

`Create` is marked **primary** (the same blue style we use on Save) so the
user has a clear default action. `Cancel` is plain — secondary by default.

> **Both buttons close the dialog**, but for different reasons. **Cancel**
> just closes — the draft entity is unreferenced afterwards and gets
> garbage-collected. **Create** writes the form into the draft, persists
> it, and *then* closes. If validation fails (in Chapter 8 it will, on
> empty required fields), `writeBeanIfValid` returns false and we
> short-circuit with `return@Button` — the dialog stays open so the user
> can fix the input.

## Why two `ProductForm` instances?

Each `ProductForm` has its **own** `Binder<Product>`. The side panel binder
is mid-edit on whichever row the user selected; the dialog binder is
filling in a brand-new draft. Sharing a single binder between them would
mean opening the Add dialog clears the side panel form, and saving the
side panel would write the dialog's draft fields back too. Two instances
isolate the two flows cleanly.

This is the practical payoff of the extraction: the form is a *value*, not
a singleton. You can have as many of them as you want, each with its own
state.

## What this chapter doesn't fix

- **No validation, still.** Submitting an empty form silently creates a
  row with `name = ""` and crashes on the database NOT NULL constraint
  for `price`. Chapter 8 plugs in JSR-303 annotations and the form will
  finally start defending itself.
- **No SKU uniqueness check at the UI layer.** The `sku` column is
  `UNIQUE` in SQL, so a duplicate raises a JDBC exception on save.
  Chapter 8 will show the user a friendly inline message instead.

Next up: validation. We're going to add `@NotNull`, `@Size`, `@Min`, and
a regex check for SKU directly on the `Product` interface — and the side
panel and dialog will both pick them up for free, because both share the
same `ProductForm`.

# Chapter 8 — Validation

Right now the form accepts whatever the user types. Blank name? Sure. Price
of zero? Fine. SKU with a space in it? No problem until the database
complains. We'll fix all of that with two small changes:

1. Add **JSR-303 constraint annotations** directly to the `Product`
   interface — declarations like `@get:NotNull`, `@get:Size`,
   `@get:Positive`, `@get:Pattern`.
2. Swap the `Binder<Product>` in `ProductForm` for
   `beanValidationBinder<Product>()`. That subclass reads the annotations
   off the bean type and refuses to write invalid values back.

Two lines change in `ProductForm.kt`. The rest is annotations on the
entity. Both the side panel and the Add dialog pick up the rules for free.

## Step 1 — annotate `Product`

Update `Product.kt` so it imports the validation constraints and tags each
property:

```kotlin
package com.example.vok

import com.github.mvysny.ktormvaadin.ActiveEntity
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.ktorm.entity.Entity
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import org.ktorm.schema.decimal
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import java.math.BigDecimal

enum class Category { Tools, Fasteners, Plumbing, Electrical, Paint, Garden }

enum class UnitOfMeasure { Each, Box, Meter, Kilogram }

interface Product : ActiveEntity<Product> {
    var id: Long?

    @get:NotNull
    @get:Size(min = 1, max = 40)
    @get:Pattern(
        regexp = "[A-Z0-9-]+",
        message = "SKU may only contain uppercase letters, digits and hyphens"
    )
    var sku: String?

    @get:NotNull
    @get:Size(min = 1, max = 200)
    var name: String?

    @get:NotNull
    var category: Category?

    @get:NotNull
    @get:Positive(message = "Price must be greater than zero")
    var price: BigDecimal?

    @get:NotNull
    @get:PositiveOrZero(message = "Stock cannot be negative")
    var stock: Int?

    @get:NotNull
    var unit: UnitOfMeasure?

    override val table: Table<Product> get() = Products

    companion object : Entity.Factory<Product>()
}

// Products table object unchanged from Chapter 3
```

Five different constraints earn their keep here:

- **`@NotNull`** — every property must be non-null. The Kotlin type is
  `Type?` (because ktorm needs that for partial entities, Chapter 3), so
  this is doing real work: it rules out `name = null`, `price = null`,
  etc. at validation time.
- **`@Size(min = 1, max = 40)`** on `sku` — at least one character, at
  most 40. The upper bound matches the SQL column length.
- **`@Pattern(regexp = "[A-Z0-9-]+", message = ...)`** — `sku` must
  match the regex. JSR-303's `@Pattern` anchors the whole string by
  default, so this rejects `m6-bolt` (lowercase), `M6 BOLT` (space) and
  `M6_BOLT` (underscore) but accepts `HX-M6-40`.
- **`@Positive`** on `price` — must be > 0. `@PositiveOrZero` would
  allow zero; we don't sell anything for free.
- **`@PositiveOrZero`** on `stock` — must be ≥ 0. We do sell things
  whose stock has dropped to zero, so this is the right choice (and
  `HX-M8-50` in the seed data has `stock = 0`).

### Why `@get:NotNull` and not just `@NotNull`?

In Kotlin, you can attach an annotation to several different parts of a
property declaration — the field, the constructor parameter, the
getter, the setter. By default, a "plain" `@NotNull var foo: ...` on a
Kotlin interface property doesn't land where Hibernate Validator looks
(which is the JavaBean getter). The `@get:` site target makes it
explicit: this annotation goes on the generated getter method, where
JSR-303 reflection will pick it up.

If you forget the `@get:` prefix, the annotations compile fine and the
app runs, but validation silently does nothing — there's no error, just
no enforcement. It's the kind of bug that takes a frustrating afternoon
to find. Always use the site target for entity validation annotations.

## Step 2 — swap the binder

Open `ProductForm.kt`. Change the binder line from:

```kotlin
val binder: Binder<Product> = Binder(Product::class.java)
```

to:

```kotlin
val binder: Binder<Product> = beanValidationBinder()
```

That's it. `beanValidationBinder<Product>()` is a Karibu-DSL helper that
returns a `BeanValidationBinder<Product>` — a subclass of `Binder<Product>`
that auto-wires JSR-303 constraint checks on every binding it creates.
Because we kept the property type as `Binder<Product>`, every existing
call site (`readBean`, `writeBeanIfValid`) keeps working unchanged.

The Kotlin inline `reified` magic on the helper means we don't have to
pass `Product::class.java` — the type parameter on the call site
(`beanValidationBinder<Product>()`) is enough. Here we don't even need
the type parameter because Kotlin infers it from the return type
annotation `Binder<Product>`. Hence the very short final form.

## Try it

Restart the app. Open the **+ Add product** dialog and click **Create**
without filling anything in. The dialog stays open and each required
field grows a red error message beneath it:

- *SKU* — "must not be null"
- *Name* — "must not be null"
- *Category* — "must not be null"
- *Price* — "must not be null"
- *Stock* — "must not be null"
- *Unit* — "must not be null"

Now type `m6-bolt` (lowercase) in **SKU**. The red message changes to
*"SKU may only contain uppercase letters, digits and hyphens"* — our
`@Pattern` `message=` is what shows. Type `HX-M6-99` instead, fill the
rest of the form sensibly, and **Create** succeeds.

In the side panel, pick an existing product. Clear the **Name** field.
The error appears immediately — `BeanValidationBinder` validates fields
on every change, not just on the way out via `writeBeanIfValid`. Click
**Save** — nothing happens; the row in the Grid is unchanged. Restore
the name, click **Save** — it saves.

## How `writeBeanIfValid` short-circuits

The save handler hasn't changed since Chapter 6:

```kotlin
private fun onSave() {
    val product = selected ?: return
    if (!sidePanelForm.binder.writeBeanIfValid(product)) return
    product.save()
    dp.refreshAll()
    Notification.show("Saved ${product.name}")
}
```

What changed is `writeBeanIfValid` itself. With the plain `Binder`, it
ran the (empty) per-binding validator list and always succeeded. With
`BeanValidationBinder`, each binding implicitly has the JSR-303
constraints attached to it. The call:

1. Walks every binding and runs its constraint checks on the **field's
   current value**.
2. If any constraint fails, the binding records the message on its
   field (the red text under the input) and the method returns `false`.
3. Only if every constraint passes does it write the values back into
   the bean and return `true`.

Our `if (!writeBeanIfValid(product)) return` was already prepared for
this — we just didn't have anything for it to defend against until now.

## The shape of the rest

You now have a six-field validated form with database-backed persistence,
reusable in a side panel and a dialog, behind a Grid with live search,
category filtering, and selection-driven editing. From here, the
remaining chapters are short feature additions:

- **Chapter 9** adds a custom cell renderer — a red **"Low stock"** badge
  on rows where `stock < threshold` — and a toolbar checkbox that hides
  everything else.
- **Chapter 10** writes browserless tests for what you've built so far
  with Karibu-Testing. The catalog stops being "I clicked around and it
  worked" and becomes "the test suite says it works".
- **Chapter 11** wires `vok-rest` to expose `/api/products` so a barcode
  scanner at the shop counter (or any other client) can read and update
  the catalog over HTTP.

# Chapter 9 — Custom cell rendering

Right now the **Stock** column shows a number and nothing else. The
shopkeeper has to read every row in turn to spot which products are
running out. We'll fix that with two small features:

1. A red **"Low stock"** badge next to the stock value when it dips
   below a threshold.
2. A **"Low stock only"** toolbar checkbox that hides every other row.

The mechanism behind the badge is Vaadin's `ComponentRenderer` — a
column renderer that produces a real Component instance per row
instead of a plain string. The checkbox plumbs into the same
`productFilter()` we built in Chapter 5; the new clause is
`Products.stock lt LOW_STOCK_THRESHOLD`.

## Step 1 — define the threshold

Open `CatalogView.kt` and add a file-level constant near the top:

```kotlin
private const val LOW_STOCK_THRESHOLD: Int = 10
```

A "threshold" is a *presentation* concern — it controls when the UI
draws attention to a row — not a domain rule. We keep it next to the
renderer and filter clause that use it, file-private so nothing else
in the project accidentally picks it up.

## Step 2 — the `stockCell` renderer

Add the renderer function at the bottom of `CatalogView.kt`,
alongside `productFilter`:

```kotlin
private fun stockCell(product: Product): Component {
    val layout = HorizontalLayout()
    layout.isPadding = false
    layout.isSpacing = true
    layout.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
    layout.add(Span(product.stock?.toString().orEmpty()))
    val stock = product.stock
    if (stock != null && stock < LOW_STOCK_THRESHOLD) {
        val badge = Span("Low stock")
        badge.element.setAttribute("theme", "badge error small")
        layout.add(badge)
    }
    return layout
}
```

A few things to call out:

- The function takes a whole `Product` and returns a `Component`. The
  Grid will call it once per visible row.
- It produces a `HorizontalLayout` containing the stock number as a
  `Span`, then conditionally appends a second `Span` with the badge
  text. `isPadding = false` keeps the cell tight against the rest of
  the column.
- The badge's appearance is controlled by Vaadin's Lumo
  `theme="badge error small"` attribute. Lumo ships a set of
  pre-styled badge variants — `badge` (neutral), `badge success`,
  `badge error`, `badge contrast`, `badge primary` — and an optional
  `small` modifier. Together they render as a small red pill. No CSS
  needed.

Wire it into the Grid by upgrading the stock column from a plain
`columnFor` to one that takes a renderer:

```kotlin
columnFor(Product::stock, ComponentRenderer(::stockCell)) { setHeader("Stock") }
```

The two-argument form of `columnFor` accepts a `Renderer<Product>` as
its second argument; `ComponentRenderer(::stockCell)` wraps our
function in the Vaadin renderer SAM. The first argument
(`Product::stock`) is still there so the column knows what property
it represents — that keeps sorting by stock value working, even
though the cell contents are now a fully custom Component tree.

Restart and reload. The Grid now shows `0 [Low stock]`,
`8 [Low stock]`, `3 [Low stock]` on the three rows below the
threshold, and bare numbers on the rest. Sort the Stock column —
ascending puts the badged rows at the top.

## Step 3 — the "Low stock only" checkbox

Add the checkbox to the toolbar (just before the `+ Add product`
button), expand `applyFilters` to read its value, and route it
through `productFilter`:

```kotlin
val lowStockField = checkBox("Low stock only")
button("+ Add product") {
    setPrimary()
    onClick { openAddDialog() }
}

fun applyFilters() {
    dp.setFilter(productFilter(searchField.value, categoryField.value, lowStockField.value))
}
searchField.addValueChangeListener { applyFilters() }
categoryField.addValueChangeListener { applyFilters() }
lowStockField.addValueChangeListener { applyFilters() }
```

Then extend `productFilter` to accept and apply the new flag:

```kotlin
private fun productFilter(
    search: String,
    category: Category?,
    lowStockOnly: Boolean,
): ColumnDeclaring<Boolean>? {
    val s = search.trim()
    val parts = listOfNotNull(
        if (s.isEmpty()) null else {
            val pattern = "%$s%"
            Products.name.ilike(pattern) or Products.sku.ilike(pattern)
        },
        category?.let { Products.category eq it },
        if (lowStockOnly) Products.stock lt LOW_STOCK_THRESHOLD else null,
    )
    return parts.reduceOrNull { a, b -> a and b }
}
```

Restart. Tick the **Low stock only** box: the Grid drops to three
rows. Untick it: ten rows return. Combine with the category filter:
pick **Paint** with the checkbox on — two rows. Pick **Tools** with
the checkbox on — zero rows (`WD40-400`'s stock is 32, well above
threshold). Everything composes through the same WHERE-clause
algebra you wrote three chapters ago.

The structural payoff: adding a *third* filter took two real lines —
a new field declaration, a new entry in the `listOfNotNull`. The
listener wiring is mechanical. The new ktorm clause
(`Products.stock lt LOW_STOCK_THRESHOLD`) participates in the same
AND combinator as the existing search and category. If the
shopkeeper later wants a "by price band" filter, or a "by supplier"
filter, the same shape works.

## Why `ComponentRenderer` instead of, say, a converter?

We could have written a converter that turned the `stock` value into a
formatted `String` like `"0 (LOW)"`. That would change the *text*
rendered into the cell — but only the text. To get colour, shape, or
icon involvement, you have to drop down to a real Component tree.
`ComponentRenderer` is the door from "values" to "any UI you want per
cell" — sparklines, progress bars, action menus, image thumbnails,
nested grids. It's expensive (a full Component instance per visible
row), but for a back-office app where the visible row count is small,
that cost doesn't matter.

For a tutorial-scale app, you'd reach for `ComponentRenderer` for any
column where you need more than a single formatted string. For
production apps with thousands of visible rows you'd consider
`LitRenderer` instead — Vaadin's lighter-weight template-based
renderer — but that's a different topic.

## The full picture

The catalog now does everything the BoltShop spec promised at the
start of the tutorial:

- A filterable Grid (search, category, low-stock toggle)
- Selection-driven side-panel editing with `Binder` and validation
- A Dialog-based add flow reusing the same form
- A custom cell renderer that draws attention to inventory needing
  attention
- All of it backed by ktorm + Flyway + H2

Two short chapters remain. **Chapter 10** writes browserless tests
covering the filters, the edit flow, the add flow, and the
validation. **Chapter 11** exposes the catalog as a small REST API
that lives next to the SPA — so a barcode scanner or any other
client can read and write products over HTTP.

# Chapter 10 — Browserless tests with Karibu-Testing

Until now every check has been "click around in a browser and squint
at the screen". This chapter replaces that with a proper test suite.
You'll be able to run `./gradlew test` and know in three seconds
whether the catalog still works.

The tool is **Karibu-Testing**: an in-process test harness for Vaadin
Flow. It mocks `UI`, `VaadinSession`, `VaadinRequest`, and the
servlet container, scans your classpath for `@Route` views, and
gives you a small DSL for locating components, setting values,
clicking buttons, and asserting Grid contents. No Jetty, no browser,
no Selenium — the entire test runs server-side in the same JVM.

## Step 1 — dependencies

Wire JUnit Jupiter and Karibu-Testing into `build.gradle.kts`:

```kotlin
dependencies {
    // ... existing implementation deps unchanged ...

    // tests
    testImplementation(libs.junit)
    testImplementation(libs.kaributesting)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

`libs.junit` resolves to `org.junit.jupiter:junit-jupiter:6.0.3`,
JUnit's latest stable line. `libs.kaributesting` resolves to
`com.github.mvysny.kaributesting:karibu-testing-v24:2.7.0` — and
yes, that's the right artifact for Vaadin 25 too. There is no
`karibu-testing-v25` variant; the `-v24` artifact targets both
Vaadin 24 and 25. The `-launcher` runtime dependency is what Gradle
9 needs to discover JUnit Platform; without it the test executor
fails before running any test.

## Step 2 — the test base

Create `src/test/kotlin/com/example/vok/AbstractAppTest.kt`:

```kotlin
package com.example.vok

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import eu.vaadinonkotlin.VaadinOnKotlin
import eu.vaadinonkotlin.vaadin.vokdb.dataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

private object AppBootstrap {
    init {
        Bootstrap().contextInitialized(null)
    }
}

abstract class AbstractAppTest {

    companion object {
        private lateinit var routes: Routes

        @BeforeAll
        @JvmStatic
        fun initSuite() {
            AppBootstrap
            routes = Routes().autoDiscoverViews("com.example.vok")
        }
    }

    @BeforeEach
    fun resetDbAndMockVaadin() {
        Flyway.configure()
            .dataSource(VaadinOnKotlin.dataSource)
            .cleanDisabled(false)
            .load()
            .apply { clean(); migrate() }
        MockVaadin.setup(routes)
    }

    @AfterEach
    fun teardownMockVaadin() {
        MockVaadin.tearDown()
    }
}
```

Three pieces worth pointing at:

- **`AppBootstrap` is a Kotlin `object`.** Its `init` block runs
  exactly once per JVM — the moment any line of code first references
  the object, the Kotlin runtime initialises it. We bootstrap the
  app by calling our existing `Bootstrap().contextInitialized(null)`
  — the production servlet listener tolerates a null event because
  it never reads it. We deliberately do **not** refactor
  `Bootstrap.kt` to expose a separate test-only init function; the
  servlet listener *is* the test entry point.
- **`Routes().autoDiscoverViews("com.example.vok")`** scans the
  classpath for `@Route`-annotated classes. It's expensive (≈ 1 sec
  cold). The skill guide for Karibu-Testing flags this as the
  single biggest perf trap: cache it in a `@BeforeAll` so it runs
  once per test class, not per test.
- **`@BeforeEach` wipes the DB and re-runs Flyway.** Each test sees
  exactly the seed data from `V1__create_product.sql` — no leakage
  from a previous mutation. The `cleanDisabled(false)` opt-in is
  scoped to this Flyway instance only; the production `Bootstrap`
  Flyway keeps the safe default.

## Step 3 — the tests

Create `src/test/kotlin/com/example/vok/CatalogViewTest.kt`. The
file is long enough that I'll show a few representative tests, then
list the rest.

### Read-only tests

```kotlin
@Test
fun `view shows seed products`() {
    _get<Grid<Product>>().expectRows(10)
}

@Test
fun `search by SKU filters Grid`() {
    _get<TextField> { placeholder = "Search by name or SKU" }._value = "PAINT"
    _get<Grid<Product>>().expectRows(2)
}

@Test
fun `category filter narrows results`() {
    _get<ComboBox<Category>> { placeholder = "Category" }._value = Category.Fasteners
    _get<Grid<Product>>().expectRows(3)
}

@Test
fun `low stock checkbox narrows results`() {
    _get<Checkbox> { label = "Low stock only" }._value = true
    _get<Grid<Product>>().expectRows(3)
}
```

The shape is consistent: locate a field by its label or placeholder,
set its value, then check the Grid's row count. `_get<T> { ... }` is
the Karibu locator — it walks the visible component tree, applies
the search criteria, and returns exactly one match or fails the test
with a tree dump. `_value =` simulates a user typing into the field,
which fires the same `ValueChangeEvent` the production listeners
react to.

### A mutating test

```kotlin
@Test
fun `save persists changes`() {
    val grid = _get<Grid<Product>>()
    grid._selectRow(0)
    val original = grid._get(0)

    _get<BigDecimalField> { label = "Price" }._value = "99.99".toBigDecimal()
    _get<Button> { text = "Save" }._click()

    expectNotifications("Saved ${original.name}")
    val updated = Products.findAll().first { it.sku == original.sku }
    assertEquals("99.99".toBigDecimal(), updated.price)
}
```

`grid._selectRow(0)` fires the same selection event the side-panel
form listens to — it populates with row 0's values. We change the
price, click **Save**, and check two things: the notification fired
("Saved …"), and the row in the database actually has the new
price. The DB round-trip via `Products.findAll()` is the proof that
the form is wired through `Binder.writeBeanIfValid` to
`product.save()` correctly.

### The validation test

```kotlin
@Test
fun `empty add dialog is rejected by validation`() {
    _get<Button> { text = "+ Add product" }._click()
    val dialog = _get<Dialog>()
    dialog._get<Button> { text = "Create" }._click()

    _expectOne<Dialog>()
    _get<Grid<Product>>().expectRows(10)
}
```

Click **+ Add product** → dialog opens. Click **Create** without
filling anything in. The dialog should still be there
(`_expectOne<Dialog>()` — exactly one match), and the Grid should
still have ten rows. If the `@get:NotNull` annotations from Chapter
8 stopped working, this test would catch it — the empty form would
fail at the database NOT NULL constraints instead, the dialog would
close on the exception, and `_expectOne<Dialog>()` would fail.

### The full file

Ten tests cover every chapter so far:

```kotlin
package com.example.vok

import com.github.mvysny.kaributesting.v10.*
import com.github.mvysny.ktormvaadin.findAll
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.BigDecimalField
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CatalogViewTest : AbstractAppTest() {

    @Test
    fun `view shows seed products`() {
        _get<Grid<Product>>().expectRows(10)
    }

    @Test
    fun `search by SKU filters Grid`() {
        _get<TextField> { placeholder = "Search by name or SKU" }._value = "PAINT"
        _get<Grid<Product>>().expectRows(2)
    }

    @Test
    fun `search by name filters Grid`() {
        _get<TextField> { placeholder = "Search by name or SKU" }._value = "bolt"
        _get<Grid<Product>>().expectRows(2)
    }

    @Test
    fun `category filter narrows results`() {
        _get<ComboBox<Category>> { placeholder = "Category" }._value = Category.Fasteners
        _get<Grid<Product>>().expectRows(3)
    }

    @Test
    fun `low stock checkbox narrows results`() {
        _get<Checkbox> { label = "Low stock only" }._value = true
        _get<Grid<Product>>().expectRows(3)
    }

    @Test
    fun `selecting a row populates the side panel form`() {
        val grid = _get<Grid<Product>>()
        val product = grid._get(0)
        grid._selectRow(0)
        assertEquals(product.sku, _get<TextField> { label = "SKU" }._value)
        assertEquals(product.name, _get<TextField> { label = "Name" }._value)
    }

    @Test
    fun `save persists changes`() {
        val grid = _get<Grid<Product>>()
        grid._selectRow(0)
        val original = grid._get(0)

        _get<BigDecimalField> { label = "Price" }._value = "99.99".toBigDecimal()
        _get<Button> { text = "Save" }._click()

        expectNotifications("Saved ${original.name}")
        val updated = Products.findAll().first { it.sku == original.sku }
        assertEquals("99.99".toBigDecimal(), updated.price)
    }

    @Test
    fun `delete removes the row`() {
        val grid = _get<Grid<Product>>()
        grid._selectRow(0)
        val original = grid._get(0)

        _get<Button> { text = "Delete" }._click()

        expectNotifications("Deleted ${original.name}")
        grid.expectRows(9)
    }

    @Test
    fun `add dialog creates a new product`() {
        _get<Button> { text = "+ Add product" }._click()
        val dialog = _get<Dialog>()

        dialog._get<TextField> { label = "SKU" }._value = "NEW-PRODUCT-1"
        dialog._get<TextField> { label = "Name" }._value = "New product"
        dialog._get<ComboBox<Category>> { label = "Category" }._value = Category.Tools
        dialog._get<BigDecimalField> { label = "Price" }._value = "5.50".toBigDecimal()
        dialog._get<IntegerField> { label = "Stock" }._value = 100
        dialog._get<ComboBox<UnitOfMeasure>> { label = "Unit" }._value = UnitOfMeasure.Each
        dialog._get<Button> { text = "Create" }._click()

        expectNotifications("Created New product")
        _get<Grid<Product>>().expectRows(11)
    }

    @Test
    fun `empty add dialog is rejected by validation`() {
        _get<Button> { text = "+ Add product" }._click()
        val dialog = _get<Dialog>()
        dialog._get<Button> { text = "Create" }._click()

        _expectOne<Dialog>()
        _get<Grid<Product>>().expectRows(10)
    }
}
```

Two patterns to call out:

- **Scoped lookups.** When the **+ Add product** dialog is open
  there are two `TextField`s with label `"SKU"` — one in the side
  panel, one in the dialog. `_get<TextField> { label = "SKU" }`
  would find both and fail. `dialog._get<TextField> { label = "SKU" }`
  scopes the search to descendants of the dialog only. Use it
  whenever the same field appears in multiple places.
- **DB assertions go through ktorm directly.** The `save persists
  changes` test reads the modified row back via `Products.findAll()`,
  not through the Grid. This is on purpose — the Grid going stale
  after a save would only fail the test for a UI reason; reading
  the database proves the *persistence* worked too.

## Run it

```bash
$ ./gradlew test
```

The first run downloads classpath-scanner artifacts and the JUnit
platform launcher; subsequent runs are fast. On a development laptop
the whole suite finishes in three seconds. The HTML report appears at
`build/reports/tests/test/index.html`.

## What's gained

Every chapter from 1 to 9 now has at least one test covering it:

- Chapter 2 → "view shows seed products" (Grid renders)
- Chapter 3 → all read tests (Flyway + ktorm round-trip)
- Chapter 4 → "search by SKU / by name filters Grid"
- Chapter 5 → "category filter narrows results"
- Chapter 6 → "selecting a row populates the side panel", "save
  persists changes", "delete removes the row"
- Chapter 7 → "add dialog creates a new product"
- Chapter 8 → "empty add dialog is rejected by validation"
- Chapter 9 → "low stock checkbox narrows results"

When you change something — say, rename `Product::name` to
`Product::displayName` — the compiler catches the field rename in
the binding, *and* the test for "search by name" catches the
behavioural regression. The catalog stops being something only the
shopkeeper's manual testing can vouch for.

One short chapter left: **Chapter 11** stands up a REST endpoint at
`/api/products` so a barcode scanner — or any other HTTP client —
can read and update the catalog over the wire.

# Chapter 11 — A REST API for the catalog

The catalog is a SPA today. That's right for the shopkeeper, but a
barcode scanner at the counter — or a future mobile app, or anything
that isn't a browser — wants to read and write products over plain
HTTP. In this chapter we'll add three endpoints under `/api/`:

| Verb | Path                       | Purpose |
|------|----------------------------|---------|
| GET  | `/api/products`            | List every product as JSON |
| GET  | `/api/products/{sku}`      | One product by its natural key |
| POST | `/api/products`            | Create a new product |

The REST stack — `vok-rest` (Javalin under the hood) — has been on
the classpath since Chapter 0, with a scaffold servlet wired into
`Bootstrap.kt` but no routes registered. We're filling that in.

## Step 1 — a DTO for the wire shape

ktorm entities are interfaces, not classes, so Gson can't directly
deserialize a JSON body into a `Product` — it has no way to
instantiate one. We translate at the boundary with a plain data
class:

Create `src/main/kotlin/com/example/vok/ProductDto.kt`:

```kotlin
package com.example.vok

import java.math.BigDecimal

data class ProductDto(
    val id: Long? = null,
    val sku: String? = null,
    val name: String? = null,
    val category: Category? = null,
    val price: BigDecimal? = null,
    val stock: Int? = null,
    val unit: UnitOfMeasure? = null,
)

fun Product.toDto(): ProductDto = ProductDto(id, sku, name, category, price, stock, unit)

fun ProductDto.toEntity(): Product = Product {
    sku = this@toEntity.sku
    name = this@toEntity.name
    category = this@toEntity.category
    price = this@toEntity.price
    stock = this@toEntity.stock
    unit = this@toEntity.unit
}
```

`ProductDto` is the *wire format*. `Product.toDto()` projects an
entity into one for serialization. `ProductDto.toEntity()` flips the
direction for incoming POST bodies — note the `Product { ... }` ktorm
`Entity.Factory` invocation we first saw in Chapter 7.

In a real app you'd often want the wire shape and the entity to
diverge — exclude `id` from POST bodies, expose computed fields, etc.
For BoltShop we keep the two in lockstep, but the conversion
functions are the seam where you'd add that asymmetry.

## Step 2 — implement the endpoints

Open `Bootstrap.kt`. The bottom half has a placeholder Javalin
servlet already mapped at `/rest/*` from Chapter 0:

```kotlin
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", ...)
class JavalinRestServlet : HttpServlet() { ... }

fun Javalin.configureRest(): Javalin {
    return this
}
```

Change the URL pattern to `/api/*` and fill in the route registration:

```kotlin
@WebServlet(urlPatterns = ["/api/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin: JavalinServlet = Javalin.createStandalone { it.gsonMapper(VokRest.gson) } .configureRest().javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    get("/api/products") { ctx ->
        ctx.json(Products.findAll().map { it.toDto() })
    }
    get("/api/products/{sku}") { ctx ->
        val sku = ctx.pathParam("sku")
        val product = db { database.sequenceOf(Products).find { it.sku eq sku } }
            ?: throw NotFoundResponse("No product with SKU '$sku'")
        ctx.json(product.toDto())
    }
    post("/api/products") { ctx ->
        val dto = ctx.bodyAsClass(ProductDto::class.java)
        val product = dto.toEntity()
        product.save()
        ctx.status(201).json(product.toDto())
    }
    exception(ConstraintViolationException::class.java) { e, ctx ->
        ctx.status(400).json(mapOf("errors" to e.constraintViolations.map { it.message }))
    }
    return this
}
```

You'll also need these imports at the top of `Bootstrap.kt`:

```kotlin
import com.github.mvysny.ktormvaadin.db
import com.github.mvysny.ktormvaadin.findAll
import io.javalin.http.NotFoundResponse
import jakarta.validation.ConstraintViolationException
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
```

Three handlers and one exception mapper. Let's unpack the pieces.

### `GET /api/products`

```kotlin
get("/api/products") { ctx ->
    ctx.json(Products.findAll().map { it.toDto() })
}
```

`Products.findAll()` is the same ktorm-vaadin extension we used
through Chapters 3-9. It runs `SELECT * FROM Product`, returns a
`List<Product>`. We project each entity into a `ProductDto` and let
Javalin's Gson mapper turn the result into a JSON array.

### `GET /api/products/{sku}`

```kotlin
get("/api/products/{sku}") { ctx ->
    val sku = ctx.pathParam("sku")
    val product = db { database.sequenceOf(Products).find { it.sku eq sku } }
        ?: throw NotFoundResponse("No product with SKU '$sku'")
    ctx.json(product.toDto())
}
```

The path template `{sku}` matches a single segment and exposes it as
a `pathParam`. We use ktorm's `sequenceOf(Products).find { ... }` to
query for the row — this is the right SQL for "fetch one by natural
key", producing `SELECT * FROM Product WHERE sku = ? LIMIT 1`. The
explicit `db { ... }` block opens the ktorm session because
`sequenceOf` requires one (unlike `findAll`, which wraps `db { }`
internally).

If no row matches, we throw `NotFoundResponse` — Javalin's stock
exception type that maps to HTTP 404 with the supplied message.

### `POST /api/products`

```kotlin
post("/api/products") { ctx ->
    val dto = ctx.bodyAsClass(ProductDto::class.java)
    val product = dto.toEntity()
    product.save()
    ctx.status(201).json(product.toDto())
}
```

`ctx.bodyAsClass(ProductDto::class.java)` deserializes the JSON
request body via Gson. The fresh `Product` entity gets its values
from the DTO, then `save()` — the `ActiveEntity` method from
Chapter 3 — runs the **JSR-303 validations from Chapter 8** before
issuing the `INSERT`. On success we respond with `201 Created` and
the created entity (including its newly assigned `id` from
`AUTO_INCREMENT`).

### The validation-to-400 bridge

```kotlin
exception(ConstraintViolationException::class.java) { e, ctx ->
    ctx.status(400).json(mapOf("errors" to e.constraintViolations.map { it.message }))
}
```

This is the small but real payoff of having JSR-303 wired across the
whole app. When the POST handler's `product.save()` throws
`ConstraintViolationException` (e.g. an empty `name` or a `price` of
zero), this handler catches it and returns HTTP 400 with the
specific constraint messages. **The same `@get:Positive` /
`@get:Pattern` messages that show inline under the form fields in
the SPA show up in the JSON error response.** No duplicate
validation rules between web UI and REST.

## Step 3 — try it

Restart the app (`./gradlew run`), then in another terminal:

### List every product

```bash
$ curl -s http://localhost:8080/api/products | python3 -m json.tool
[
    {
        "id": 1,
        "sku": "HX-M6-40",
        "name": "Hex bolt M6×40mm, zinc-plated",
        "category": "Fasteners",
        "price": 0.35,
        "stock": 120,
        "unit": "Each"
    },
    {
        "id": 2,
        "sku": "HX-M8-50",
        ...
    },
    ...
]
```

Ten objects, in seed-data order. `python3 -m json.tool` is just for
pretty-printing — `curl` alone returns the same JSON on one line.

### Look one up by SKU

```bash
$ curl -s http://localhost:8080/api/products/HX-M6-40
{"id":1,"sku":"HX-M6-40","name":"Hex bolt M6×40mm, zinc-plated","category":"Fasteners","price":0.35,"stock":120,"unit":"Each"}

$ curl -i http://localhost:8080/api/products/NOPE
HTTP/1.1 404 Not Found
Content-Type: application/json
...

No product with SKU 'NOPE'
```

### Create a new product

```bash
$ curl -i -X POST http://localhost:8080/api/products \
    -H 'Content-Type: application/json' \
    -d '{
      "sku": "HX-M10-60",
      "name": "Hex bolt M10x60mm",
      "category": "Fasteners",
      "price": 1.20,
      "stock": 50,
      "unit": "Each"
    }'
HTTP/1.1 201 Created
Content-Type: application/json
...

{"id":11,"sku":"HX-M10-60","name":"Hex bolt M10x60mm","category":"Fasteners","price":1.20,"stock":50,"unit":"Each"}
```

Reload the SPA at <http://localhost:8080> — the new bolt appears in
the Grid. The same database backs both.

### Try to break it

```bash
$ curl -i -X POST http://localhost:8080/api/products \
    -H 'Content-Type: application/json' \
    -d '{
      "sku": "bad-sku",
      "name": "",
      "category": "Tools",
      "price": 0,
      "stock": -5,
      "unit": "Each"
    }'
HTTP/1.1 400 Bad Request
Content-Type: application/json
...

{"errors":["Price must be greater than zero","must not be empty","Stock cannot be negative","SKU may only contain uppercase letters, digits and hyphens"]}
```

Every constraint message is the exact text from a `@get:Pattern`,
`@get:Positive`, `@get:PositiveOrZero`, or `@get:Size` annotation on
the `Product` interface back in Chapter 8. One validation source of
truth, three surfaces using it (Binder side panel, Binder dialog,
REST endpoint).

## Where to take this next

The skeleton above is deliberately minimal — three endpoints, one
DTO, one exception bridge. A few directions to explore from here:

- **`PUT /api/products/{sku}`** for updates. The shape is `POST`
  plus a `db { database.sequenceOf(Products).find { it.sku eq sku } }`
  lookup, then copying the DTO fields onto the loaded entity and
  calling `save()`.
- **`DELETE /api/products/{sku}`** — load the entity, call
  `entity.delete()` (the same `delete()` we wired into the SPA's
  side-panel button), respond `204 No Content`.
- **Pagination on `GET /api/products`** — read `offset` / `limit`
  query params and use `database.sequenceOf(Products).drop(offset).take(limit)`.
  vok-rest's `KtormCrudHandler` does this generically — drop into
  that helper if you want a full CRUD surface without hand-writing
  each route.
- **Authentication** — the
  [vok-security-demo](https://github.com/mvysny/vok-security-demo)
  repo has the pattern for protecting both Vaadin views and REST
  endpoints with the same credentials store. Worth a read once you
  outgrow "the LAN is the auth boundary".

## Where we landed

The BoltShop catalog is done. From a single `@Route("")` view
showing the words *"Welcome to BoltShop"* you've built:

- a single-page master-detail screen with live search, category
  filter, and a low-stock toggle;
- a `Binder`-backed side-panel editor with JSR-303 validation;
- a `Dialog`-based add flow reusing the same form;
- a `ComponentRenderer` for inline visual cues;
- a Karibu-Testing suite that runs the whole UI in 3 seconds with
  no browser;
- and a small REST API that any HTTP client can talk to.

All on Vaadin Boot — no Spring, no app server, just a `main()` and a
Hikari pool. Around 460 lines of Kotlin under `src/main/` across
seven small files. That's the BoltShop tutorial.

The `complete` branch of
[vok-helloworld-app](https://github.com/mvysny/vok-helloworld-app/tree/complete)
holds the finished code, one chapter per commit. If anything in this
tutorial doesn't reproduce, check the matching commit on `complete`
and compare. And if you spot something out of date — or if there's a
follow-up chapter you'd like to see (multi-entity? routing?
`UI.access`?) — open an issue.
