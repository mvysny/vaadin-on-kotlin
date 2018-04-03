[![GitHub tag](https://img.shields.io/github/tag/mvysny/vaadin-on-kotlin.svg)](https://github.com/mvysny/vaadin-on-kotlin/tags)

# JPA support for Vaadin8 compatibility package

This module provides the `Container` implementation which is able to fetch JPA entities. It is able
to use filters and sorting. It uses the [JPAContainer](https://vaadin.com/directory/component/vaadin-jpacontainer)
add-on under the hood.

To use this dependency, just add the following to your `build.gradle`:

```groovy
dependencies {
    compile "com.github.vok:vok-framework-jpa-compat7:x.y.z"
}
```

> Note: obtain the newest version from the release name, and the tag name as well: [https://github.com/mvysny/vaadin-on-kotlin/releases](https://github.com/mvysny/vaadin-on-kotlin/releases)

> Note: this module is NOT compatible with Vaadin7-based app; it is only compatible with Vaadin8-based app which
includes the compatibility library which sports the `com.vaadin.v7.data.Container` (note the `v7` in the package name).

To use this module, just call on your `com.vaadin.v7.ui.Grid`:

```kotlin
grid.setContainerDataSource(jpaContainer<Person>())
```
