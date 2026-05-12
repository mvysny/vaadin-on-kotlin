---
layout: default
title: Grids
permalink: /grids/
parent: Guides
nav_order: 4
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

# Using Grids

Vaadin Grid is the workhorse for tabular data: it lazy-loads rows as the user scrolls, and the user can
sort and filter per column. It's also the most feature-dense Vaadin component — take things slow and
copy from the [vok-example-crud demo](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-example-crud/src/main/kotlin/example/crudflow/person)
as you go.

Reference docs for Grid itself live at
[Vaadin Components — Grid](https://vaadin.com/docs/latest/components/grid). This page focuses on the
VoK / karibu-dsl idioms layered on top.

> *Backing a Grid with something other than SQL?* See
> [Accessing NoSQL or REST data sources](/nosql_rest_datasources). The contract is the same — extend
> Vaadin's `AbstractBackEndDataProvider<T, F>`; only the filter type changes.

## General rules

Grid scrolls itself, so its height should never be `wrapContent`. The usual setup is `setSizeFull()` on
the Grid, and a parent layout with a fixed or filled height. Inside a `VerticalLayout` the common
pattern is `setSizeFull()` on the layout and `flexGrow = 1.0` on the Grid so it fills remaining space.

## Binding to data

For SQL-backed views, every ktorm `Table<E>` gets a `dataProvider` extension from
[ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin) — an
`EntityDataProvider<E>` that lazy-loads pages, translates Grid sort clicks into SQL `ORDER BY`, and
accepts a ktorm `ColumnDeclaring<Boolean>` filter via `setFilter()`.

```kotlin
grid<Person>(Persons.dataProvider) {
    setSizeFull()
    columnFor(Person::name)
    columnFor(Person::age)
}
```

The data provider is the second argument to the `grid<T>(...)` builder. There's no `setDataLoader(...)`
call any more — that was the pre-0.19 `DataLoader<T>` indirection, replaced by Vaadin's
`DataProvider` directly. See [Accessing SQL Databases](/databases) for the full story (entity/table
definitions, the `db { }` block, custom SELECTs via `QueryDataProvider`).

### Default sorting

To pick a default sort *and* show the sort indicator in the column header, call `grid.sort(column.asc)`
(from karibu-tools) after the columns are defined:

```kotlin
grid<Person>(Persons.dataProvider) {
    val nameColumn = columnFor(Person::name)
    columnFor(Person::age)
    sort(nameColumn.asc)
}
```

`.asc` / `.desc` are extension properties on `Grid.Column<*>`. If you only need the SQL to be sorted
(no visual indicator), you can instead set a filter on the data provider — but `grid.sort(...)` is
almost always what you want.

> Make sure every sortable column has a matching database index, otherwise sorted SELECTs will be slow.

### Permanently-applied filter

To restrict what the user can ever see (e.g. multi-tenant scoping), push a `ColumnDeclaring<Boolean>`
straight into `setFilter()`:

```kotlin
val dataProvider = Persons.dataProvider.apply {
    setFilter(Persons.age greaterEq 18)
}
grid<Person>(dataProvider) { /* ... */ }
```

If you also have a filter bar, combine the permanent and user-driven conditions in a single
`updateFilter()` method — see [Filters](#filters) below.

## Adding columns

By default a Grid shows no columns. Add them with `columnFor()`:

```kotlin
grid<Person>(Persons.dataProvider) {
    flexGrow = 1.0

    columnFor(Person::id, sortable = false, key = Persons.id.e.key) {
        width = "90px"; isExpand = false
    }
    columnFor(Person::name, key = Persons.name.e.key)
    columnFor(Person::age, key = Persons.age.e.key)
    columnFor(Person::alive, key = Persons.alive.e.key)
    columnFor(Person::dateOfBirth, converter = { it?.toString() }, key = Persons.dateOfBirth.e.key)
    columnFor(Person::maritalStatus, key = Persons.maritalStatus.e.key)
    columnFor(Person::created, converter = { it?.toString() }, key = Persons.created.e.key)
}
```

`columnFor()` derives a human-friendly header from the property name (`dateOfBirth` → `Date Of Birth`),
defaults to `Any?.toString()` as the renderer, and makes the column sortable. Override any of those by
passing arguments or a configuration block (the receiver is `Grid.Column<T>`).

> *Column keys for sortable columns*: pass `key = Persons.<col>.e.key`. `EntityDataProvider` looks up
> the ktorm column to sort on by the Vaadin column's key — without one, clicking the column header
> can't be translated into an `ORDER BY`. The `.e.key` extension lives in ktorm-vaadin
> (`com.github.mvysny.ktormvaadin.e`).

For columns that aren't bound to a property — action buttons, computed cells — use Vaadin's
`addColumn(renderer)` directly. The example app defines a small `Grid<T>.addButtonColumn(...)` helper
to add icon-button columns; it's not part of VoK, just a one-screen utility you can copy from
[PersonListView.kt](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud/src/main/kotlin/example/crudflow/person/PersonListView.kt):

```kotlin
addButtonColumn(VaadinIcon.EYE, "view", { p: Person -> navigateTo(PersonView::class, p.id!!) }) {}
addButtonColumn(VaadinIcon.EDIT, "edit", { p: Person -> createOrEditPerson(p) }) {}
addButtonColumn(VaadinIcon.TRASH, "delete", { p: Person -> p.delete(); refresh() }) {}
```

## Column widths

Every column defaults to `flexGrow = 1`, meaning all columns share the available width equally. Set
`isExpand = false` (alias for `flexGrow = 0`) to give a column its content-based natural width:

```kotlin
columnFor(Person::id) {
    isExpand = false
    width = "90px"   // optional explicit pixel width
}
```

With at least one expanded column left, the non-expanded ones use the width of their initially-rendered
content, and the expanded ones soak up the rest in proportion to their `flexGrow` values.

The user can drag column separators to resize; once they do, all column widths switch to explicit
pixel values.

## Filters

Filter components in the header row collect user input and feed it into the data provider as a single
`ColumnDeclaring<Boolean>`. The recipe — and the ktorm-vaadin filter components (`FilterTextField`,
`NumberRangePopup`, `DateRangePopup`, `BooleanFilterField`, `EnumFilterField`) — is documented
end-to-end in [Accessing SQL Databases — Grid filters](/databases#grid-filters).

The shape is:

1. Append a header row: `val filterBar = appendHeaderRow()`.
2. Per column, create a filter component, set its value-change listener to call your `updateFilter()`,
   and place it into the cell:
   `filterBar.getCell(this).component = nameFilter`.
3. In `updateFilter()`, collect each component's current value into a list of
   `ColumnDeclaring<Boolean>?`, AND them with ktorm-vaadin's
   `Collection<ColumnDeclaring<Boolean>?>.and()`, and push to the data provider via
   `dataProvider.setFilter(...)`.

```kotlin
val filterBar = appendHeaderRow()
columnFor(Person::name) {
    nameFilter.addValueChangeListener { updateFilter() }
    filterBar.getCell(this).component = nameFilter
}
// ... and so on for the other columns
```

```kotlin
private fun updateFilter() {
    val conditions = mutableListOf<ColumnDeclaring<Boolean>?>()
    if (nameFilter.value.isNotBlank()) {
        conditions += Persons.name.ilike("${nameFilter.value.trim()}%")
    }
    conditions += Persons.age.between(ageFilter.value.asIntegerInterval())
    aliveFilter.value?.let { conditions += Persons.alive eq it }
    dataProvider.setFilter(conditions.and())
}
```

There's no auto-wiring of filter components from bean properties any more — each column's filter is
chosen and bound explicitly. Plain Vaadin fields work too; the ktorm-vaadin components are convenient
defaults, not a requirement.

The full working example is
[PersonListView.kt](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud/src/main/kotlin/example/crudflow/person/PersonListView.kt)
in the demo app — copy from there.

> Make sure every filtered column has a matching database index, otherwise filtered SELECTs will be
> slow.

## Conditional row / cell formatting

To change the look of individual cells based on the row's content (highlight overdue, color by status,
etc.), use `Grid.Column.setClassNameGenerator()` and define the matching CSS class in your theme:

```kotlin
columnFor(Person::created, converter = { it?.toString() }) {
    setClassNameGenerator { person -> if (person.age!! > 60) "senior" else null }
}
```

For text alignment within a column there's Vaadin's `Grid.Column.setTextAlign()` (or
`textAlign = ColumnTextAlign.CENTER` in karibu-dsl).

For row-level styling — striping by status, highlighting the selected row differently — use
`grid.setClassNameGenerator { row -> ... }` on the Grid itself.
