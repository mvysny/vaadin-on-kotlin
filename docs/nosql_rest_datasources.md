[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Accessing NoSQL or REST data sources

VoK provides no out-of-the-box support for accessing NoSQL databases;
to access data from a NoSQL database you should use appropriate NoSQL
database Java driver. VoK however does support REST REST endpoints: you can use the
[vok-rest-client](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client)
VOK module which uses the [OkHttp](http://square.github.io/okhttp/) REST client
library and adds couple of useful functionality on top of that.

## VoK REST Support

VoK provides full support for publishing and consuming entities in a CRUD
fashion over REST, with all features like paging, sorting and filtering. VoK
even provides an implementation of Vaadin Grid's `DataProvider` which fetches
data over REST.

There are two modules on VoK providing REST support:

* [vok-rest](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest) when you need to publish data
from your VoK server to the world;
* [vok-rest-client](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-rest-client)
when you need to consume REST endpoints published elsewhere, and maybe displaying the fetched
data in a Grid.

Please click the links to read the module documentation - it should contain
all information necessary for you to get started.

There is also an example project which exposes a list of entities over REST server,
then self-consumes them via REST client connected to localhost:8080 and exposes
them in a Grid: [vaadin8-restdataprovider-example](https://github.com/mvysny/vaadin8-restdataprovider-example)
and [vaadin10-restdataprovider-example](https://gitlab.com/mvysny/vaadin10-restdataprovider-example).

## Data Loaders

VoK uses the concept of a _Data Loader_. The data loader is responsible for loading
pages of data from arbitrary data source, sorting and filtering it as necessary,
converting rows into Java Beans. The data loader then can be plugged into Vaadin Grid,
in order to show the data.

For example, the REST client class `CrudClient` is a data loader since it
implements the `DataLoader` interface. Since Vaadin Grid works with `DataProvider`s,
you need to use `DataLoaderAdapter` to convert `CrudClient` into `DataProvider`,
which you can then assign to the Grid. It's always wise to call `.withConfigurableFilter2()`
on the adapter which enables the filter row to set filters to the data provider.

Another example is the SQL database access via the `vok-orm`. vok-orm provides
data loader for every entity which is able to load instances of that entity.
VoK then provides convenience extension method on vok-orm `Dao` (the `Dao.dataProvider`)
which takes the data loader and uses `DataLoaderAdapter` to convert it into `DataProvider`.

### Why Not Use DataProvider API Directly?

Why we need the data loaders? Can't we simply implement `DataProvider` API and assign
it to the Grid directly? Unfortunately we can't, for the following reasons:

1. There are two different `DataProvider` interfaces (they are identical but the class name
   is different): one for Vaadin 8, one for Vaadin 10. We would have to implement
   every provider two times, one for Vaadin 8, other for Vaadin 10. And the vok-orm
   would have to depend on the Vaadin 8 and Vaadin 10 API, which doesn't make any sense.
2. The `DataLoader` API is vastly simpler than the `DataProvider`.
3. The `DataLoader` API library ([vok-dataloader](https://github.com/mvysny/vok-dataloader))
   comes with a filter hierarchy, so you don't have to write your own hierarchy.


TBD more

## The 'Behind The Scenes' Info

This chapter documents the general information what you need to write a custom `DataProvider`
for your particular type of data source. It is important to understand the basics,
before you start using whatever VoK provides for you.

### Feeding Grid With Data

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
as the `<F>` generic parameter to not to support any filters.

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

### Adding Support For Sorting

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

### Adding Support For Filtering

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

## Filter Factory

If your filters support all logic as required by the `FilterFactory` interface, then
you can use the `FilterFieldFactory` class to automatically create the filtering UI components
and auto-populate the filter bar for you.

Just implement the `FilterFactory` interface, then override `DefaultFilterFieldFactory` and tailor it towards
your needs (e.g. make `createField()` return null for columns you don't want to support).
Then, in your code just call

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid, itemClass, filterFactory, filterFieldFactory)
```

to create the filter row and populate it with filter components.

Please read the documentation for the [vok-util-vaadin8](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin8)
or [vok-util-vaadin10](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin10)
module for more information on generating filters.

If you're using the standard `Filter` hierarchy as provided by VOK (which
all of VOK's `DataProvider` do), it is already compatible with `FilterFactory` via the
`SqlFilterFactory` implementor. In that case you can use the more simplified generator which
will also use `DefaultFilterFieldFactory` by default:

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid, itemClass)
```
