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

Vaadin Grid allows you to display tabular data in a scrollable div, lazy-loading rows as they are scrolled into the viewport.
It also allows you to sort and filter for particular column.

Vaadin Grid is probably the most versatile and complex component, and it may take some time to learn all of its capabilities.
Just take things slow and you'll be used to Grid in no time.

You can learn about Grid capabilities in the [official Vaadin 14 Grid Documentation page](https://vaadin.com/docs/v14/flow/components/tutorial-flow-grid.html).

There are more ways of using Grid; in the tutorial we will use the easiest way as endorsed by the VoK framework.

> Note: this article uses a SQL database to fetch data. If you are using NoSQL or REST
  or other means to fetch data, please read [Accessing NoSQL/REST DataSources](/nosql_rest_datasources) for more details.

## General Rules

Since the Grid scrolls by itself, it makes no sense to set its height to `wrapContent`. The best way is to make the Grid to fill its parent
in both width and height directions, simply by calling `setSizeFull()`; then just make sure its parent layout also fills its parent (or has
a particular height set, e.g. `200.px`) and so on.

## Binding to Data

VoK entities automatically provide a `DataLoader` instance.
The `DataLoader` interface automatically fetches pages of entities lazily from
the underlying database table, supporting all optional features such as sorting
and filtering.

A simplest Grid loading data from such a `DataLoader`
can be constructed as follows:

```kotlin
grid<Person> {
  setDataLoader(Person.dataLoader)
}
```

> Note: if you wish to display an outcome of a complex SELECT JOIN, please read the [Showing an arbitrary output of any SQL SELECT command](databases.md)
section.

You can further configure the `DataLoader` by for example adding a default sorting,
or adding an unremovable filter. If the user has not selected any particular sorting
in the Grid, you can provide a default one:

```kotlin
grid<Person> {
  setDataLoader(Person.dataLoader.sortedBy(Person::name.asc))
}
```

You can also impose an unremovable filter which limits the data the user is able
to see:

```kotlin
grid<Person> {
  setDataLoader(Person.dataLoader.withFilter { Person::age ge 18 })
}
```

## Adding columns

By default the Grid will show no columns. You can add columns easily, by calling the `columnFor()` function as follows:

```kotlin
personGrid = grid<Person> {
  flexGrow = 1.0
  setDataLoader(Person.dataLoader)

  columnFor(Person::id, sortable = false)
  columnFor(Person::name)
  columnFor(Person::age)
  columnFor(Person::alive)
  columnFor(Person::dateOfBirth, converter = { it?.toString() })
  columnFor(Person::maritalStatus)
  // example of a custom renderer which converts value to a displayable string.
  columnFor(Person::created, converter = { it?.toString() })

  // add additional columns with buttons
  addButtonColumn(VaadinIcon.EYE, "view", { person: Person -> navigateToView(PersonView::class, person.id!!) }) {}
  addButtonColumn(VaadinIcon.EDIT, "edit", { person: Person -> createOrEditPerson(person) }) {}
  addButtonColumn(VaadinIcon.TRASH, "delete", { person: Person -> person.delete(); refresh() }) {}
}
```

The `columnFor()` will set the column header automatically, by converting `dateOfBirth` camelCase to `Date Of Birth` Human Friendly
format. It will also use the default renderer/converter pair which simply calls `.toString()` on any value of that particular property.
The column is also by default sortable. To override this behavior, you can provide a configuration block to the `addColumnFor()` function
which will allow you to configure the column further (simply by calling setters/methods on the `Grid.Column` receiver).

The `columnFor()` function is tailored towards creating columns bound to a bean property. If you wish to create columns not backed by
any property (say, a column with the "Show Details" link), you can simply use the `addColumn()` function as provided by the Vaadin Grid
itself. Please see above for the example.

## Sorting

The Grid is initially unsorted and shows the data in the whatever order the `DataProvider` considers the default one. In entity data providers
the data is typically sorted by the primary key by default, which makes little sense for the user. You can hence create a data provider which
provides a different default sorting instead:

```kotlin
grid<Person> {
  setDataLoader(Person.dataLoader.sortedBy(Person::name.asc))
}
```

However, this doesn't cause a sorting indicator to appear in the Grid Column's header.
Therefore, it's better to configure the Grid itself to sort by given column.
That can be achieved by calling `Grid.sort()`. That way, a visual
sort indicator would be displayed for the user as well:

```kotlin
grid<Person> {
  setDataLoader(Person.dataLoader)
  sort(Person::name.asc)
}
```

> Note: please make sure to create appropriate database index for every sortable column, otherwise the database SELECTs would be quite slow.

## Column Widths

All columns are by default expanded, with the expand ratio of `1`. That means
they all use the same portion of available width space and hence
they all have the same width. You can turn off this behavior by setting column's
`isExpand` to false (which is an alias for setting `flexGrow` to 0):

```kotlin
grid(...) {
    addColumn(newDeleteButtonRenderer()).apply {
        isExpand = false
    }
}
```

This will cause the column to have undefined width, which causes automatic sizing based on the widths of the displayed data.
You can set column widths explicitly by pixel value with setting the column `width` property, or relatively using expand ratios with `flexGrow`.

When using expand ratios, the columns with a non-zero expand ratio use the extra space remaining from other columns, in proportion
to the defined ratios. Do note that the minimum width of an expanded column by default is based on the contents of the column
(the initially rendered rows).

The user can resize columns by dragging their separators with the mouse. When resized manually, all the columns widths are set to explicit
pixel values, even if they had relative values before.

## Filters

In the simplest case, when you do not wish to fine-tune the filter appearance, you can simply add the following command into your
`grid{}` block:

```kotlin
grid(dataProvider = Person.dataProvider) {
    // ..
    val filterBar = appendHeaderRow().asFilterBar(this)
    columnFor(Person::name) {
        filterBar.forField(TextField(), this).istartsWith()
    }
    columnFor(Person::age) {
        filterBar.forField(NumberRangePopup(), this).inRange()
    }
    columnFor(Person::alive) {
        filterBar.forField(BooleanComboBox(), this).eq()
    }
    columnFor(Person::dateOfBirth, converter = { it?.toString() }) {
        filterBar.forField(DateRangePopup(), this).inRange(Person::dateOfBirth)
    }
}
```

You need to create the filter component themselves, but VoK provides the means
to bind auto-generate filter components for all bean properties shown in the Grid itself. For more information on this topic
please read the [VoK Vaadin Utils Documentation](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-util-vaadin/README.md).

> Note: please make sure to create appropriate database index for every filtrable
> column, otherwise the database SELECTs would be quite slow.

You can do quite a lot with the data providers - please see the [Databases Guide](databases.md) for more details.

## Conditional Row/Cell formats

Often it is required to change the cell formating of a Vaadin grid depending on
the rows content, for example in order to highlight certain values.
In order to achieve that, simply use the `Grid.Column.setClassNameGenerator()`
function as follows:

```kotlin
columnFor(Person::created, converter = { it!!.toInstant().toString() }) {
  filterBar.forField(DateRangePopup(), this).inRange(Person::created)
  setClassNameGenerator { it -> "black" }
}
```

Most commonly you only need to align the text within the column cell; for that
there's `Grid.Column.setTextAlign()` function provided by Vaadin directly.
