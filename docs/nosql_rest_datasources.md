[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Accessing NoSQL or REST data sources

VoK currently provides no out-of-the-box support for accessing NoSQL databases.
However, VoK offers a lot of support for showing fetched data in the Vaadin Grid,
with support for sorting, filtering (including auto-generated filter bar) and
paging. All you need to implement is single interface called `DataLoader`.
For information on how to do that, please read on.

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

VoK data fetching revolves around a thing called a _Data Loader_. The data loader is responsible for loading
pages of data from arbitrary data source, sorting and filtering it as necessary,
converting rows into Java Beans. The data loader then can be plugged into Vaadin Grid,
in order to show the data.

For example, the REST client class `CrudClient` is a data loader since it
implements the `DataLoader` interface. It fetches lists of JSON maps and
converts them into Java Beans with the help of [Gson](https://github.com/google/gson). In order to connect
`CrudClient` into Vaadin Grid,
you need to use `DataLoaderAdapter` to convert `CrudClient` into `DataProvider`.
It's always wise to call `.withConfigurableFilter2()`
on the adapter which enables the filter row to set filters to the data provider.

Another example is the SQL database access via the `vok-orm`. vok-orm provides
data loader which fetches instances of entities from the database, with the help
of the [Sql2o](https://www.sql2o.org/) library.
VoK then provides convenience extension method on vok-orm `Dao` - the `Dao.dataProvider` -
which takes the data loader and uses `DataLoaderAdapter` to convert it into `DataProvider`.
VoK also provides the `sqlDataProvider()` function which works on arbitrary SQL SELECTs.

Another example would be to add support for MongoDB. You only need to figure out
how to fetch data with given paging and sorting, then define allowed filters
and define mapping from JSON map to a Java Bean. That's all that's needed to implement
a MongoDB-backed `DataLoader`. Then it's just a matter of using `DataLoaderAdapter` to
turn `DataLoader` into Vaadin `DataProvider`, feed that to Vaadin Grid and
let VoK auto-generate filter bar for you.

### Why Not Use DataProvider API Directly?

Why we need the data loaders? Can't we simply implement Vaadin's `DataProvider` API and pass
it to the Grid directly? Unfortunately we can't, for the following reasons:

1. There are actually two `DataProvider` interfaces: `com.vaadin.data.provider.DataProvider` for Vaadin 8,
  `com.vaadin.flow.data.provider.DataProvider` for Vaadin 10. The methods are identical but the class name
   is different. We would thus have to provide
   an implementation of the same thing two times, both for Vaadin 8 and Vaadin 10. Also,
   the data-fetching library like `vok-orm`
   would have to depend on the Vaadin 8 and Vaadin 10 API, which is simply too heavyweight dependency.
2. The `DataLoader` API is simpler than the `DataProvider`.
3. The `DataLoader` API library ([vok-dataloader](https://github.com/mvysny/vok-dataloader))
   comes with filter classes provided (`EqFilter`, `LikeFilter` etc), so you don't have to
   write your own filter classes.

## Implementing Data Loader

This chapter documents the general information what you need to write a custom `DataLoader`
for your particular data source. It is important to understand the basics,
before you start using whatever VoK provides for you.

### Feeding Grid With Data

In order to feed the Vaadin Grid with data, you need to have two things:

* A bean (a data class) that is able to hold all data for one row of a Grid
* A proper implementation of `DataLoader` which would then:
  * Fetch the data from your NoSQL/REST/other source;
  * Map the data to a bean so that it provides instances of that bean directly.

> Note: If you are accessing a SQL database, there are already two data providers pre-provided for you:
  the `EntityDataProvider` (fetching entities from their tables) and `SQLDataProvider` (fetching
  result of any SQL SELECT query).
  Please read [Grids](grids.md) and [Accessing SQL Databases](databases.md) (Vaadin 10: [Grids](grids-v10.md) and [Accessing SQL Databases](databases-v10.md)) for more details.

To implement the `DataLoader` interface you only need to implement two methods:

* `fun getCount(filter: Filter<T>?): Long` takes filters and computes number of matching rows.
  That enables the Grid to draw a proper scroll bar.
* `fun fetch(filter: Filter<T>?, sortBy: List<SortClause>, range: LongRange): List<T>`
  function retrieves the actual data. You need to pay attention to the `range` parameter which
  specifies the paging.

Your initial implementation does not need to support any filters nor sorting, to keep things simple.
Simply ignore the `filter` and `sortBy` parameters for now, and focus on implementing
the conversion from your native data source to a Java Bean. You can use:

* [Gson](https://github.com/google/gson) for conversion from JSON to Java Bean; you can use the
  vok-provided `Gson.fromJsonArray()` to convert from an array of JSON maps, or use the following code:
  `fromJson<List<T>>(reader, TypeToken.getParameterized(List::class.java, itemClass).type)`
* [Sql2o](https://www.sql2o.org/) to convert an outcome of a JDBC result to a Java Bean

Afer you have the skeletal implementation of your `DataLoader` ready,
let's focus on converting the `DataLoader` to Vaadin's `DataProvider` so that you can
use it with the Grid. Say that you fetch a list of `Person`:

```kotlin
val dataLoader = ... // your DataLoader here
val dp = DataLoaderAdapter(Person::class.java, crud, { it.id!! }).withConfigurableFilter2()
grid.dataProvider = dp
```

Grid will never attempt to pass in any filters on its own. If you need filtering, Grid expects
you to create a filter bar and implement everything yourself. Luckily, VoK is able to
do that for you. We'll go through this in a minute.

### Adding Support For Sorting

If you need certain columns in the Grid to be sortable, you need to support
sorting clauses in your data provider implementation - you need to pay attention
to the `sortBy` parameter of the `DataLoader.fetch()` method.

In your `DataLoader` implementation you need to convert the contents of the
`sortBy` parameter into e.g. a list of query parameters (in case of REST),
or a sorting clauses (in case of a NoSQL query).

Which columns are actually sortable will depend on your data source. For REST,
the sorting parameters are limited to whatever is available as a query parameter in the
REST http call. For NoSQL, the set of available indices limit available sorting parameters.
By default it is expected that all properties of the Java Bean (e.g. the `Person` class)
are sortable; the `SortClause.propertyName` in the `sortBy` parameter may be a name of any
property in the `Person` class.

Of course that may be unwanted since sorting generally
requires indices and indices tend to slow down insertion of new data. Therefore,
it is best for your `DataLoader` to throw `IllegalArgumentException` in `fetch()` for
any unsupported sort clause it encounters in the `sortBy` field. Then, document all supported sorting criteria
in the kdoc for your `DataLoader`, so that you'll know which columns to mark as non-sortable in the Grid.

You may also need to provide support for 'hidden' columns (not mapped to the Java Bean);
you may therefore decide to also support native property names on `SortClause`, e.g.
`p.id` from `SELECT p.* FROM Person p INNER JOIN ...`. Just make sure everything
is documented in your `DataLoader` kdoc. See [vok-dataloader](https://github.com/mvysny/vok-dataloader)
for an explanation of the difference between _native properties_ and _data loader properties_,
and when to support which.

### Adding Support For Filtering

To add support for filters, both of your `getCount()` and `fetch()` implementations
must take the `filter` parameter into consideration. The `DataLoader` comes with
a predefined set of filters which you should implement:

* `EqFilter` for equality comparisons
* `OpFilter` for less-than, greater-than etc comparisons
* `LikeFilter` for case-sensitive starts-with comparison
* `ILikeFilter` for case-insensitive starts-with comparison
* `IsNullFilter` and `IsNotNullFilter`
* `AndFilter` for ANDing multiple filters
* `OrFilter` for ORing multiple filters. This one is optional: for example the REST
  CRUD client doesn't support this one; also VoK-autogenerated filter bar never
  produces the OR filter.

You should document, for which fields the filtering is supported, in the data loader javadoc/kdoc.
Since filtering generally also requires indices, the remark from the sorting chapter above
also applies. You need to support exactly the same property naming scheme
which you support for sorting.

Now your data loader is ready to be plugged into the Grid. Go ahead, plug it
into the Grid and play with the sorting a bit until you're happy with the result.
However, Grid never outputs any filters. In order to test filters, we need to
add a filter bar.

## Filter Factory

If your data loader supports all filters as required by the `FilterFactory` interface, then
you can use the `FilterFieldFactory` class to automatically create the filtering UI components
and auto-populate the filter bar for you.

Just implement the `FilterFactory` interface, then override `DefaultFilterFieldFactory` and tailor it towards
your needs (e.g. make `createField()` return null for columns you don't want to support).
Then, in your code just call

```kotlin
grid.appendHeaderRow().generateFilterComponents(grid, itemClass, filterFieldFactory)
```

to create the filter row and populate it with filter components.

## More Resources

Please read the documentation for the [vok-util-vaadin8](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin8)
or [vok-util-vaadin10](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-util-vaadin10)
module for more information on generating filters.

Please find all technical details on how to implement the data loader in the
[vok-dataloader](https://github.com/mvysny/vok-dataloader) page.
