[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-jpa/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-jpa)

# VoK Vaadin 8 and SQL database support via JPA

This module includes:
 
* The [vok-util-vaadin8](../vok-util-vaadin8) module which includes Vaadin8-related
utilities for pleasant Kotlin development of Vaadin8-based app,
* In addition it provides several
utility methods to provide nice integration of Vaadin 8 with the SQL database via JPA, namely the
Grid filters and Grid DataProvider.

To use this module in your app just add the following dependency into your `build.gradle` file:

```groovy
dependencies {
    compile("eu.vaadinonkotlin:vok-framework-jpa:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

This module provides:

* The [JPADataProvider.kt](src/main/kotlin/eu/vaadinonkotlin/vaadin8/jpa/JPADataProvider.kt)
  which is a Vaadin `DataProvider` implementation which is able to fetch instances of JPA entities
  and support proper sorting and filtering. To use it, just write `grid.dataProvider = JPADataProvider(Person::class).configurableFilter()`

> Note: The `configurableFilter()` bit is necessary if you use filter components, since these components need to set
appropriate filters to the `DataProvider` on their value change. See the `vok-util-vaadin8` module for more details.

* The `generateFilterComponents()` function which is able to automatically generate Grid filter components:
  `grid.appendHeaderRow().generateFilterComponents(grid)`

## When to use this module

Use this module if you intend to build a Vaadin8-based app which accesses your SQL database
using the non-recommended JPA approach.

If you plan to use the recommended approach of using [vok-orm](https://github.com/mvysny/vok-orm),
then use the [vok-framework-sql2o](../vok-framework-sql2o) module instead.

If you plan to use NoSQL database or some other form of data fetching, then only use the
[vok-util-vaadin8](../vok-util-vaadin8) module. You will then have to write your own data fetching
layer; however you may find inspiration on how to do so, by checking the sources of this module.

## More documentation

* See the documentation of the [vok-util-vaadin8](../vok-util-vaadin8) module on how the filters work
  in general and how to use them.
