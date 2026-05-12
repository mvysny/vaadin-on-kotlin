---
layout: default
title: Accessing NoSQL or REST data sources
permalink: /nosql_rest_datasources/
parent: Guides
nav_order: 7
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

# Accessing NoSQL or REST data sources

VoK ships no out-of-the-box bindings for NoSQL stores. What it does ship is a thin, predictable contract
for plugging any data source into a Vaadin Grid: extend Vaadin's `AbstractBackEndDataProvider<T, F>`
and implement two methods. The Grid then gets sorting (from Vaadin), paging (from Vaadin), and filtering
(from a filter type `F` that you define) for free.

The same contract is what backs both:

* **SQL** — `EntityDataProvider<T>` / `QueryDataProvider<T>` from
  [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin). Filter type is `ColumnDeclaring<Boolean>` (a
  ktorm expression). See [Accessing SQL Databases](/databases) for the full story.
* **REST** — `CrudClient<T>` from [vok-rest-client](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client).
  Filter type is `Map<String, String>` (eq-only). Documented below.

## REST

VoK provides server- and client-side support for CRUD endpoints over HTTP, with paging, sorting, and
eq-only filtering:

* **[vok-rest](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest)** publishes ktorm tables
  as REST resources via `Table<E>.getCrudHandler()` (or the manual `KtormCrudHandler<E>`).
* **[vok-rest-client](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client)** consumes
  those endpoints (or any REST API that matches the wire format) via `CrudClient<T>`, which *is* a
  Vaadin `DataProvider` — drop it straight into a Grid.

The wire format is intentionally minimal:

* `GET /rest/person?col=value` — eq-only filtering, one query parameter per filter column.
* `?offset=N&limit=M` — paging.
* `?sort=col:asc,col2:desc` — multi-column sort.
* `?count=true` — count of matching rows (the Grid scroller uses this to size the scrollbar).

Caller pre-formats filter values as Strings (`"2024-05-12"`, `"42"`, etc.). There is no nested
condition tree, no boolean combinators, no LIKE — keep filter complexity out of the URL.

For everything else — the JSON shape, Gson customization, error handling, partial-update semantics —
see the [vok-rest](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest) and
[vok-rest-client](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client) READMEs.

## Plugging in a custom backend (NoSQL, GraphQL, gRPC, …)

To back a Grid with any other data source, extend Vaadin's `AbstractBackEndDataProvider<T, F>`:

```kotlin
class MongoBookProvider(
    private val collection: MongoCollection<Document>,
) : AbstractBackEndDataProvider<Book, BookFilter>() {

    override fun fetchFromBackEnd(query: Query<Book, BookFilter>): Stream<Book> {
        val bson = query.filter.orElse(BookFilter()).toBson()
        return collection.find(bson)
            .skip(query.offset)
            .limit(query.limit)
            .sort(query.sortOrders.toBson())   // your conversion of QuerySortOrder -> Bson
            .map { it.toBook() }
            .asSequence().asStream()
    }

    override fun sizeInBackEnd(query: Query<Book, BookFilter>): Int {
        val bson = query.filter.orElse(BookFilter()).toBson()
        return collection.countDocuments(bson).toInt()
    }
}

grid.dataProvider = MongoBookProvider(books)
```

Two design choices to make:

1. **The bean type `T`** — a plain Kotlin class (`data class Book(...)`) is fine; it doesn't have to
   be a ktorm `Entity`.
2. **The filter type `F`** — anything you want. `Map<String, String>` for REST-ish flat filters, a
   sealed class for typed conditions, a Mongo `Bson`, an Elasticsearch query DSL. The Grid will pass
   whatever you set via `dataProvider.setFilter(...)`.

Then `Query` (Vaadin's, not ktorm's) gives you `offset`, `limit`, `sortOrders: List<QuerySortOrder>`,
and `filter: Optional<F>`. Translate these into your backend's native query language inside the two
overrides.

> *Why no `DataLoader` indirection?* Earlier VoK had a `DataLoader<T>` / `FilterFactory` / `DataLoaderAdapter`
> layer between custom backends and Vaadin's `DataProvider`. It carried a pre-defined filter taxonomy
> (`EqFilter`, `LikeFilter`, `AndFilter`, etc.) which had to be translated to both the filter UI and the
> backend query language. As of 0.19 that layer is gone — Vaadin's `DataProvider` is the direct
> extension point, you choose your own filter type, and the SQL implementation lives in ktorm-vaadin.

### Worked example: `CrudClient`

The cleanest small-scale example of the pattern is `CrudClient<T>` in
[vok-rest-client/CrudClient.kt](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-rest-client/src/main/kotlin/eu/vaadinonkotlin/restclient/CrudClient.kt)
(~120 lines). It picks `Map<String, String>` as the filter type, parses `query.sortOrders` into a
`col:asc,col2:desc` string, and uses the JDK `HttpClient` + Gson to fetch JSON. Worth reading end-to-end
if you're building your own provider — the structure transfers directly to non-REST backends.

## Filter bar wiring

Filter components in the Grid header work the same way regardless of backend. Each filter component
fires value-change events; you collect their current values into your filter type `F` in one
`updateFilter()` method and push via `dataProvider.setFilter(...)`.

The pattern is documented end-to-end in [Accessing SQL Databases — Grid filters](/databases#grid-filters);
the only thing that varies per backend is the type you produce in `updateFilter()`:

* SQL (ktorm-vaadin): combine `ColumnDeclaring<Boolean>` conditions via the provided
  `Collection<ColumnDeclaring<Boolean>?>.and()` extension.
* REST (`CrudClient`): build a `Map<String, String>` from your filter components' values, formatting
  each as a String.
* NoSQL / custom: build whatever type your `AbstractBackEndDataProvider<T, F>` declared as `F`.

ktorm-vaadin's filter components (`FilterTextField`, `NumberRangePopup`, `DateRangePopup`,
`BooleanFilterField`, `EnumFilterField`) are convenient defaults but not required — any Vaadin field
that emits value-change events works.

## More resources

* [Accessing SQL Databases](/databases) — the SQL/ktorm-vaadin side of the same DataProvider story.
* [vok-rest README](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest) — REST server,
  CRUD handler, wire format, testing.
* [vok-rest-client README](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client) —
  REST client, `CrudClient`, JSON customization, testing patterns.
* [Vaadin DataProvider docs](https://vaadin.com/docs/latest/binding-data/data-provider) — the
  underlying Vaadin contract.
