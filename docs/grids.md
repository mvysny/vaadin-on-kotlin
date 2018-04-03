[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Using Grids

Vaadin Grid allows you to display tabular data in a scrollable div, lazy-loading rows as they are scrolled into the viewport.
It also allows you to sort and filter for particular column.

Vaadin Grid is probably the most versatile and complex component, and it may take some time to learn all of its capabilities.
Just take things slow and you'll be used to Grid in no time.

You can learn about Grid capabilities in the [official Vaadin 8 Grid Documentation page](https://vaadin.com/docs/v8/framework/components/components-grid.html).

There are more ways of using Grid; in the tutorial we will use the easiest way endorsed by the VoK framework.

## General Rules

Since the Grid scrolls by itself, it makes no sense to set its height to `wrapContent`. The best way is to make the Grid to fill its parent
in both width and height directions, simply by calling `setSizeFull()`; then just make sure its parent layout also fills its parent (or has
a particular height set, e.g. `200.px`) and so on.

## Binding to Data

VoK entities automatically provide a `DataProvider` instances. It is the means to populate Grid with the data. The simplest Grid
can be constructed as follows:

```kotlin
grid(dataProvider = Person.dataProvider) {
}
```

The `DataProvider` automatically fetches the data lazily from a single SQL page, supporting all optional features such as sorting
and filtering.

> Note: if you wish to display an outcome of a complex SELECT JOIN, please read the [Showing an arbitrary output of any SQL SELECT command](databases.md)
section.

You can configure the `Person.dataProvider` in multiple ways:

* If the user has not selected any particular sorting in the Grid, you can provide a default one: `grid(dataProvider = Person.dataProvider.sortedBy(Person::name.asc)) {}`
* You can also impose an unremovable filter which limits the data the user is able to see: `grid(dataProvider = Person.dataProvider.withFilter { Person::age ge 18 }) {}`

## Adding columns

By default the Grid will show no columns. You can add columns easily, by calling the `addColumnFor()` function as follows:

```kotlin
grid(dataProvider = Person.dataProvider) {
    expandRatio = 1f; setSizeFull()

    // a sample of how to reconfigure a column
    addColumnFor(Person::id) { isSortable = false }
    addColumnFor(Person::name)
    addColumnFor(Person::age)
    addColumnFor(Person::dateOfBirth) {
        setRenderer(LocalDateRenderer())
    }
    addColumnFor(Person::maritalStatus)
    addColumnFor(Person::alive)
    addColumnFor(Person::created) {
        // example of a custom renderer which converts value to a displayable string.
        setRenderer({ it.toString() }, TextRenderer())
    }

    // add additional columns with buttons
    addColumn({ "Show" }, ButtonRenderer<Person>({ event -> PersonView.navigateTo(event.item) }))
    addColumn({ "Edit" }, ButtonRenderer<Person>({ event -> createOrEditPerson(event.item) })).id = "edit"
    addColumn({ "Delete" }, ButtonRenderer<Person>({ event -> deletePerson(event.item.id!!) }))
}
```

The `addColumnFor()` will set the column header automatically, by converting `dateOfBirth` camelCase to `Date Of Birth` Human Friendly
format. It will also use the default renderer/converter pair which simply calls `.toString()` on any value of that particular property.
The column is also by default sortable. To override this behavior, you can provide a configuration block to the `addColumnFor()` function
which will allow you to configure the column further (simply by calling setters/methods on the `Grid.Column` receiver).

The `addColumnFor()` function is tailored towards creating columns bound to a bean property. If you wish to create columns not backed by
any property (say, a column with the "Show Details" link), you can simply use the `addColumn()` function as provided by the Vaadin Grid
itself. Please see above for the example.

## Sorting

The Grid is initially unsorted and shows the data in the whatever order the `DataProvider` considers the default one. In entity data providers
the data is typically sorted by the primary key by default, which makes little sense for the user. You can hence create a data provider which
provides a different default sorting instead: `grid(dataProvider = Person.dataProvider.sortedBy(Person::name.asc)) {}`

Even better is to tell the Grid to initially sort by given column, by calling one of the `Grid.sort()` functions. That way, a visual
sort indicator is displayed for the user as well.

> Note: please make sure to create appropriate database index for every sortable column, otherwise the database SELECTs would be quite slow.

## Column Widths

Columns have by default undefined width, which causes automatic sizing based on the widths of the displayed data.
You can set column widths explicitly by pixel value with `setWidth()``, or relatively using expand ratios with `setExpandRatio()``.

When using expand ratios, the columns with a non-zero expand ratio use the extra space remaining from other columns, in proportion
to the defined ratios. Do note that the minimum width of an expanded column by default is based on the contents of the column
(the initially rendered rows). To allow the column to become narrower than this, use `setMinimumWidthFromContent(false)` (introduced in 8.1).

You can specify minimum and maximum widths for the expanding columns with `setMinimumWidth()` and `setMaximumWidth()`, respectively.

The user can resize columns by dragging their separators with the mouse. When resized manually, all the columns widths are set to explicit
pixel values, even if they had relative values before.

## Filters

In the simplest case, when you do not wish to fine-tune the filter appearance, you can simply add the following command into your
`grid{}` block:

```kotlin
grid(dataProvider = Person.dataProvider) {
    // ..

    // automatically create filters, based on the types of values present in particular columns.
    grid.appendHeaderRow().generateFilterComponents(grid)
}
```

VoK provides means to auto-generate filter components for all bean properties shown in the Grid itself. For more information on this topic
please read the [VoK Vaadin 8 Utils Documentation](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-util-vaadin8/README.md).

> Note: please make sure to create appropriate database index for every filtrable column, otherwise the database SELECTs would be quite slow.

## Editing rows, customizing Grid look and feel

For more information on the Vaadin Grid, please consult the [official Vaadin 8 Grid Documentation page](https://vaadin.com/docs/v8/framework/components/components-grid.html).
