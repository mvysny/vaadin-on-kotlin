---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Welcome
permalink: /
nav_order: 1
---

Vaadin-on-Kotlin (or VoK for short) is a web-application framework that includes
everything necessary to create database-backed web applications in server-side Kotlin.

This is all you need to have a button on a page which, upon clicking,
creates new row in your SQL database:

```kotlin
button("Create") {
  onLeftClick { Person(name = "Albert Einstein").save() }
}
```

# Everything is a component

Need a button? Write `button {}`. Need a text field? Write `textField {}`.

Build own components and views by composing existing components with layouts.

Navigate to components by marking them with a `@Route("path")` annotation.

## Simplicity

No JavaEE nor Spring is needed; all complex features are deliberately left out, which makes Vaadin-on-Kotlin a perfect
starting point for beginning programmers: you will write only server-side Kotlin code.
JavaScript and CSS are needed only if you decide
to style up your application or write your own custom rich component.

Note that VoK is not just another REST library, or HTTP route mapping library.
On the contrary: it is built on [Vaadin](https://vaadin.com) which provides you with a
[wide palette of built-in powerful components](https://karibu-uitest.herokuapp.com/):
lazy paged tables, color pickers, menu components, sliders.
All components have two parts:

- Rich JavaScript front which runs in the browser and provides the UI with which the user interacts; and
- The server-side part providing API you use to develop your webapps.

The components use Vaadin to handle the communication
between the client-side front and server-side part; for example, the component
Grid is basically a scrollable table which shows tabular data
The client-side front of the Grid component fetches
the tabular data from the server-side part of the Grid component. Grid configures
Vaadin to pass the data properly from server-side Grid
part to the client-side Grid front. To you as programmer, this process is completely transparent:
you develop your webapp using the server-side component API only, in pure server-side Kotlin code; the components then handle
client-server communication
(see [example demo](https://vok-crud.herokuapp.com/crud); just press the "Generate test data" button at the top to get some data).

Thanks to this approach, VoK feels more like a desktop widget library (such as Swing or JavaFX) than a web page-based framework.

# How to Start

vok is based on latest Vaadin.

You can follow the [tutorial](/tutorial) or read the [guides](/guides).

In the side panel you will find full navigation. 

# Further Reading

For technical description of what Vaadin-on-Kotlin is, please feel free to read
the [Vaadin-on-Kotlin Github page](https://github.com/mvysny/vaadin-on-kotlin).

Vaadin-on-Kotlin apps are typically three-tiered:

- The browser renders HTML and JavaScript constructed by the components orchestrated
  by the Vaadin framework. Vaadin offers the possibility to orchestrate
  interactive components entirely server-side using a rich Java API. The Karibu-DSL
  library wraps Vaadin APIs to provide more pleasant Kotlin experience.
- Server-side code is typically written using the Kotlin language. This is where
  your app logic resides and this is where you will add your code.
- The database access is handled by the VoK-ORM library: a very simple and powerful
  layer over a SQL database. You can of course decide not to use
  VoK-ORM and use JPA; you can even decide not to use SQL at all and use a NoSQL database.

Vaadin-on-Kotlin apps typically consist of several pieces. To learn more about a particular piece, just click the box below:

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu-dsl';" class="box bg-grey-lt-200"><div class="caption">Karibu-DSL</div><div class="body">Write your UI in structured Kotlin code</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-orm';" class="box bg-blue-000"><div class="caption">VoK-ORM</div><div class="body">Unleash your database, with a sprinkle of Kotlin magic</div></div>
<div onclick="location.href='https://github.com/mvysny/karibu-testing';" class="box bg-green-000"><div class="caption">Karibu-Testing</div><div class="body">Test your UI with speed and reliability</div></div>
<div onclick="location.href='https://vaadin.com/';" class="box bg-yellow-000"><div class="caption">Vaadin</div><div class="body">The Productive UI Framework for Java Web Apps</div></div>
<div onclick="location.href='https://github.com/mvysny/dynatest';" class="box bg-red-000"><div class="caption">DynaTest</div><div class="body">Create and reuse test batteries in a sane way</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-coroutines-demo';" class="box bg-grey-dk-000"><div class="caption">Async</div><div class="body">Sane async code with coroutines</div></div>
</div>

# Further Links

- Please file bug reports at the [VoK Bug Tracker](https://github.com/mvysny/vaadin-on-kotlin/issues)

<style>
.box {
  border-radius: 4px;
  padding: 5px 10px;
  margin: 10px;
  width: 200px;
  transition: box-shadow 200ms;
  transition-timing-function: cubic-bezier(0.55, 0, 0.1, 1);
  color: rgba(0, 0, 0, 0.6);
  cursor: pointer;
}
.box:hover {
  box-shadow: 0 5px 10px rgba(0,0,0,.15);
}
.box .caption {
  font-size: 22px;
}
.box .body {
  padding-top: 8px;
  font-size: 14px;
}
</style>
