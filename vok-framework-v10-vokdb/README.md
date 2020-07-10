[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-v10-vokdb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/eu.vaadinonkotlin/vok-framework-v10-vokdb)

# VoK Vaadin 14 and SQL database support

This module includes:
 
* The [vok-util-vaadin10](../vok-util-vaadin10) module which includes Vaadin10-related
utilities for pleasant Kotlin development of Vaadin10-based app,
* The [vok-db](../vok-db) module which includes SQL database support,
* In addition it provides several
utility methods to provide nice integration of Vaadin 14 with the SQL database, namely the
Grid filters and Grid DataProvider.

To use this module in your app just add the following dependency into your `build.gradle` file:

```groovy
dependencies {
    implementation("eu.vaadinonkotlin:vok-framework-v10-vokdb:x.y.z")
}
```

> Note: to obtain the newest version see above for the most recent tag

This module provides:

* [dataProvider](src/main/kotlin/eu/vaadinonkotlin/vaadin10/vokdb/DataProviders.kt) 
  which is a Vaadin `DataProvider` implementation which is able to fetch instances
  of VoK-ORM entities and support proper sorting and filtering.
  It also defines extension property `dataProvider` on the `Dao` interface
  which allows you to write code as follows: `grid.dataProvider = Person.dataProvider`
* [sqlDataProvider](src/main/kotlin/eu/vaadinonkotlin/vaadin10/vokdb/DataProviders.kt)
  which is able to represent the outcome of any SELECT including joins
  (as opposed to `dataProvider` which is only able to represent one table or view).
* And all of the [vok-util-vaadin10](../vok-util-vaadin10) goodies.

## When to use this module

Use this module if you intend to build a Vaadin 14-based app which accesses your SQL database
using the recommended approach.

There is no support for Vaadin 14 + JPA in VoK.

If you plan to use NoSQL database or some other form of data fetching, then only use the
[vok-util-vaadin10](../vok-util-vaadin10) module. You will then have to write your own data fetching
layer; however you may find inspiration on how to do so, by checking the sources of this module.

## More Information

* The [Accessing Database Guide](https://www.vaadinonkotlin.eu/databases-v10.html)
* The [Using Grids Guide](https://www.vaadinonkotlin.eu/grids-v10.html)
