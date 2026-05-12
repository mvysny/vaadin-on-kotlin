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
