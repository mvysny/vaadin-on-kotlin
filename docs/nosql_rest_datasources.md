# Accessing NoSQL or REST data sources

VoK provides no out-of-the-box support for accessing NoSQL or REST data
sources. To access data from a NoSQL database you should use appropriate NoSQL
database Java driver; to access data from a REST endpoint you can use a REST client
library such as [Retrofit](https://square.github.io/retrofit/).

## Feeding Grid with data

In order to feed the Vaadin Grid with data, you need to have two things:

* A bean (a data class) that is able to hold all data for one row of a Grid
* A proper implementation of Vaadin's `DataProvider` which would then:
  * Fetch the data from the NoSQL/REST/other source;
  * Map the data to a bean so that it provides instances of that bean directly.

To implement the `DataProvider` interface it's easiest to extend the
`AbstractBackEndDataProvider` class.
Your initial implementation does not need to support any filters nor sorting - to keep things simple just pass in `Unit`
or `Void` or `Nothing` as the `F` generic parameter to not to support any filters.

The only thing you need to do is to:
* Support the count query and implement the `AbstractBackEndDataProvider.sizeInBackEnd()` function
* Implement the paged fetches by implementing the `AbstractBackEndDataProvider.fetchFromBackEnd()`
  function. You need to pay attention to `Query.offset` and `Query.limit` fields which
  specifies the paging for you.

Then, just call `Grid.setDataProvider()` to set your data provider and you're good to go.
Grid will never attempt to pass in any filters on its own - you need to create
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

### Adding support for filtering

todo
