[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Accessing NoSQL or REST data sources

VoK provides no out-of-the-box support for accessing external NoSQL or REST data
sources and polling them for data. To access data from a NoSQL database you should use appropriate NoSQL
database Java driver; to access data from a REST endpoint you can use a REST client
library such as [Retrofit](https://square.github.io/retrofit/).

## Feeding Grid with data

In order to feed the Vaadin Grid with data, you need to have two things:

* A bean (a data class) that is able to hold all data for one row of a Grid
* A proper implementation of Vaadin's `DataProvider` which would then:
  * Fetch the data from the NoSQL/REST/other source;
  * Map the data to a bean so that it provides instances of that bean directly.

> Note: If you are accessing a SQL database, there are already two data providers pre-provided for you:
  the `EntityDataProvider` (fetching entities from their tables) and `SQLDataProvider` (fetching
  result of any SQL SELECT query).
  Please read [Grids](grids.md) and [Accessing SQL Databases](databases.md) (Vaadin 10: [Grids](grids-v10.md) and [Accessing SQL Databases](databases-v10.md)) for more details.

To implement the `DataProvider` interface it's easiest to extend the
`AbstractBackEndDataProvider` class.
Your initial implementation does not need to support any filters nor sorting - to keep things simple just pass in `Unit?`
as the `F` generic parameter to not to support any filters.

There are two things you need to implement:
* Implement the `AbstractBackEndDataProvider.sizeInBackEnd()` function which queries how many rows
  there are in the data source. That enables the Grid to draw a proper scroll bar.
* Implement the `AbstractBackEndDataProvider.fetchFromBackEnd()`
  function which retrieves the actual data. You need to pay attention to `Query.offset` and `Query.limit` fields which
  specifies the paging.

Then, just call `Grid.setDataProvider()` to set your data provider and you're good to go.
Grid will never attempt to pass in any filters on its own (it will always set
`Query.filter` to `null`). If you need filtering, you need to create
a filter bar and implement it yourself to do that. We'll go through this in a minute.

### Adding support for sorting

If you need certain columns in the Grid to be sortable, you need to support
sorting clauses in your data provider implementation - you need to pay attention
to the `Query.sortOrders` field. You only need to consider this field in the
`AbstractBackEndDataProvider.fetchFromBackEnd()` method - obviously the sorting
will not alter the count of the items.

In your `DataProvider` implementation you need to convert the contents of the
`Query.sortOrders` field into e.g. a list of query parameters (in case of REST),
or a sorting clauses (in case of a NoSQL query).

Which columns are actually sortable will depend on your data source. For REST,
the sorting parameters are limited to whatever is available as a query parameter in the
REST http call. For NoSQL, the set of available indices limit available sorting parameters.

It is best for your `DataProvider` to throw `IllegalArgumentException` in `AbstractBackEndDataProvider.fetchFromBackEnd()` for
any unsupported sort clause it encounters in the `Query.sortOrders` field. Then, document all supported sorting criteria
in the kdoc for your `DataProvider`, so that you'll know which columns to mark as non-sortable in the Grid.

### Adding support for filtering

To add support for filters, both of your `sizeInBackEnd()` and `fetchFromBackEnd()` implementations
must take the `Query.filter` field into consideration.

Adding support for filtering is more complicated than adding support for sorting:

* Vaadin provides no predefined filter class hierarchy which you can use. Vaadin expects
  that every implementation of `DataProvider` will introduce its own set of filters
  which are tailored towards that particular `DataProvider`. Some data providers
  also have certain limitations, for example some data providers can't handle
  ANDing or ORing other filters etc.
* Vaadin Grid itself will never pass in any kind of filters: it will always pass in
  `null` as the value of `Query.filter`.

In order to add filters to your data provider and to the Grid displaying that
particular data provider, you need to:

* Define classes that will act as filters and will be accepted by your `DataProvider` (the `F`
  generic parameter of the `DataProvider` interface)
* Create UI components that convert user input into instances of the filter classes from
  the step above.
* Create a filter bar, by creating a `HeaderRow` in the Grid; populate the filter bar
  with the UI components from the above step.
* Wrap your `DataProvider` in a `ConfigurableFilterDataProvider` before setting it to the Grid.
  When your filter components change, compute the newest filter and set it to the data provider
  via the `ConfigurableFilterDataProvider.setFilter()` method. The method will take care
  to notify the Grid, which will then re-poll the data.

This is a lot of manual work. Fortunately, if your data provider support full filter logic, you
can use VoK's provided filtering components.

#### FilterFactory

If your filters support all logic as required by the `FilterFactory` interface, then
you can use the `FilterFieldFactory` class to automatically create the filtering UI components
and auto-populate the filter bar for you.

Just implement the `FilterFactory` interface, then override `DefaultFilterFieldFactory` and tailor it towards
your needs (e.g. make `createField()` return null for columns you don't want to support).
Then, in your code just call

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid, itemClass, filterFieldFactory)
```

to create the filter row and populate it with filter components.

Please read the documentation for the [vok-util-vaadin8](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin8)
or [vok-util-vaadin10](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin10)
module for more information.