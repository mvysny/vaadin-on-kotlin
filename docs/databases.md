[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Accessing SQL databases with Vaadin-on-Kotlin

Vaadin-on-Kotlin provides first-class support for the following SQL databases out-of-the-box:

* [H2 Database](http://h2database.com) - a 100% Java database which can be quick-started as an in-memory
  database; perfect for writing tests for your app.
* [PostgreSQL](https://www.postgresql.org/)
* [MariaDB](https://mariadb.org/)
* [MySQL](https://www.mysql.com/)

All other SQL databases may or may not work. Care has been taken to only use the SQL92 syntax,
but we only test and officially support the four of the above-mentioned databases.

> *NoSQL Note*: Only SQL databases which provide appropriate JDBC drivers are currently supported.
There is no direct support for NoSQL databases, but you can easily integrate any NoSQL database with VoK.

> *Note for experienced Java developers*: Experienced Java developers will notice that VoK is *not* using JPA nor Hibernate to access the
database. The reason is that there are inherent issues with the abstraction that JPA
mandates - you can read more about the topic in the [Why Not JPA](http://mavi.logdown.com/posts/5771422) article.

## Basic CRUD ORM

The above horrific acronym stands for inserting, querying and deleting rows from your database,
and mapping those rows into Kotlin objects so that they are easy to work with.

The "CRUD" stands for [Create, read, update and delete](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) -
the four basic operations performed on a collection of entities mapped to the database, such as a
collection of `Person`s.

The ORM stands for Object-Relational Mapping and stands for mapping database rows into Kotlin objects,
for easier use from within your application. VoK does not use JPA but instead features
a new, vastly simplified database access layer called `vok-orm`.

## About vok-orm

`vok-orm` is a very simple object-relational mapping library, built around the following ideas:

* Simplicity is the most valued property; working with plain SQL commands is preferred over having a type-safe
  query language.
* Kotlin objects merely capture JDBC `ResultSet` rows, by the means of invoking appropriate setters (based on the column name) via
  Java reflection. 
* The entities are just plain objects: they do not track modifications as JPA entities do,
  they do not automatically store modified
  values back into the database. They are never runtime-enhanced and can be final.
* A switch from one type of database to another never happens. We understand that the programmer
  wants to exploit the full potential of the database, by writing SQLs tailored for that particular database.
  `vok-orm` should not attempt to generate SELECTs on behalf of the programmer (except for the very basic ones related to CRUD);
  instead it should simply allow SELECTs to be passed as Strings, and then map the result
  to an object of programmer's choosing.

Because of its simple design principles, `vok-orm` supports not just mapping tables to Kotlin classes,
but it allows mapping of any complex SELECT with joins and everything, even VIEWs, into Kotlin classes.
Naturally this allows you to use any SELECT inside of a Vaadin Grid component which is a
very powerful combination. 

## Persisting simple objects into tables

Please read the [Usage examples](https://github.com/mvysny/vok-orm#usage-examples) chapter of
the `vok-orm` documentation.

## Using `vok-orm` with Vaadin Grid

Vaadin Grid is a very powerful component which allows you to show a lazy-loaded list of rows
on a web page. It allows the user to:

* efficiently scroll the list, lazy-loading more data as they are scrolled into the viewport,
* sorting by one or more columns (shift-click the caption to add sorting columns)
* filtering from code
* VoK provides means to auto-generate filter components and auto-populate them into the Grid,
  which provides you with a simple means to allow the user to filter as well.

You can find more information about the Vaadin Grid at the [Vaadin Grid Documentation](http://wc.demo.vaadin.com/mcm/out/framework/components/components-grid.html) page.

