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
