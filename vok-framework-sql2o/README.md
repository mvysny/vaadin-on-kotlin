[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# VoK Vaadin 8 and SQL database support

This module includes:
 
* The [vok-util-vaadin8](../vok-util-vaadin8) module which includes Vaadin8-related
utilities for pleasant Kotlin development of Vaadin8-based app,
* The [vok-db](../vok-db) module which includes SQL database support,
* In addition it provides several
utility methods to provide nice integration of Vaadin 8 with the SQL database, namely the
Grid filters and Grid DataProvider.

To use this module in your app just add the following dependency into your `build.gradle` file:

```groovy
dependencies {
    compile "com.github.vok:vok-framework-sql2o:x.y.z"
}
```

> Note: to obtain the newest version see above for the most recent tag

This module provides:

* The [EntityDataProvider.kt](src/main/kotlin/com/github/vok/framework/sql2o/vaadin/EntityDataProvider.kt)
  which is a Vaadin `DataProvider` implementation which is able to fetch instances of VoK-ORM entities
  and support proper sorting and filtering. It also defines extension property `dataProvider` on the `Dao`
  interface which allows you to write code as follows: `grid.dataProvider = Person.dataProvider.configurableFilter()`

> Note: The `configurableFilter()` bit is necessary if you use filter components, since these components need to set
appropriate filters to the `DataProvider` on their value change. See the `vok-util-vaadin8` module for more details.

* The [SqlDataProvider.kt](src/main/kotlin/com/github/vok/framework/sql2o/vaadin/SqlDataProvider.kt) which
  is able to represent the outcome of any SELECT including joins (as opposed to `EntityDataProvider` which
  is only able to represent one table or view).
* The `generateFilterComponents()` function which is able to automatically generate Grid filter components:
  `grid.appendHeaderRow().generateFilterComponents(grid)`

## When to use this module

Use this module if you intend to build a Vaadin8-based app which accesses your SQL database
using the recommended approach.

If you plan to use JPA, then use the [vok-framework-jpa](../vok-framework-jpa) module instead.

If you plan to use NoSQL database or some other form of data fetching, then only use the
[vok-util-vaadin8](../vok-util-vaadin8) module. You will then have to write your own data fetching
layer; however you may find inspiration on how to do so, by checking the sources of this module.
