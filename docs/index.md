[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

![VoK Logo](iconography/vok_logo_small.svg)

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

Navigate to components by marking them with a `@Route("path")` (for
Vaadin 14-based apps) or `@AutoView("path")` (for Vaadin 8-based apps) annotation.

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

* Rich JavaScript front which runs in the browser and provides the UI with which the user interacts; and
* The server-side part providing API you use to develop your webapps.

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

# Tutorial

To get started with Vaadin-on-Kotlin, [start here](gettingstarted-v10.html). To find out more, please visit the following pages:

* [Vaadin-on-Kotlin Guides](vok-guides.md)
* [Vaadin-on-Kotlin GitHub project page](https://github.com/mvysny/vaadin-on-kotlin).

The tutorial comes in two versions, for Vaadin 14 and for Vaadin 8.

## Vaadin 14 Quick Starters

Vaadin 14 components are based on the Web Components standard; Vaadin 14-based apps
are also themable more easily than Vaadin 8-based apps.

Every VoK project tends to have several files (database migrations,
Gradle build script, themes, logger configuration etc) and our project will
be no exception. Therefore, it makes sense to
have an archetype app with all of those files already provided.

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu10-helloworld-application';" class="box md"><div class="caption">UI Base</div><div class="body">A project with one view and no db; perfect for your UI experiments</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-helloworld-app-v10';" class="box app"><div class="caption">VoK Project Base</div><div class="body">Skeletal app with support for SQL db; start building your app here</div></div>
<div onclick="location.href='https://github.com/mvysny/beverage-buddy-vok';" class="box go"><div class="caption">Full Stack App</div><div class="body">The "Beverage Buddy" app backed by SQL db; demoes two tables</div></div>
<div onclick="location.href='https://github.com/mvysny/bookstore-vok';" class="box go"><div class="caption">Full Stack App</div><div class="body">The "Bookstore" app backed by SQL db; also demoes security</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-kotlin-pwa';" class="box au"><div class="caption">Full Stack PWA</div><div class="body">Full-stack task list app backed by SQL db; for desktop and mobile browsers</div></div>
</div>

# Further Reading

For technical description of what Vaadin-on-Kotlin is, please feel free to read
the [Vaadin-on-Kotlin Github page](https://github.com/mvysny/vaadin-on-kotlin).

Vaadin-on-Kotlin apps are typically three-tiered:

* The browser renders HTML and JavaScript constructed by the components orchestrated
  by the Vaadin framework. Vaadin offers the possibility to orchestrate
  interactive components entirely server-side using a rich Java API. The Karibu-DSL
  library wraps Vaadin APIs to provide more pleasant Kotlin experience.
* Server-side code is typically written using the Kotlin language. This is where
  your app logic resides and this is where you will add your code.
* The database access is handled by the VoK-ORM library: a very simple and powerful
  layer over a SQL database. You can of course decide not to use
  VoK-ORM and use JPA; you can even decide not to use SQL at all and use a NoSQL database.

Vaadin-on-Kotlin apps typically consist of several pieces. To learn more about a particular piece, just click the box below: 

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu-dsl';" class="box md"><div class="caption">Karibu-DSL</div><div class="body">Write your UI in structured Kotlin code</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-orm';" class="box app"><div class="caption">VoK-ORM</div><div class="body">Unleash your database, with a sprinkle of Kotlin magic</div></div>
<div onclick="location.href='https://github.com/mvysny/karibu-testing';" class="box fe"><div class="caption">Karibu-Testing</div><div class="body">Test your UI with speed and reliability</div></div>
<div onclick="location.href='https://vaadin.com/';" class="box go"><div class="caption">Vaadin</div><div class="body">The Productive UI Framework for Java Web Apps</div></div>
<div onclick="location.href='https://github.com/mvysny/dynatest';" class="box au"><div class="caption">DynaTest</div><div class="body">Create and reuse test batteries in a sane way</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-coroutines-demo';" class="box pt"><div class="caption">Async</div><div class="body">Sane async code with coroutines</div></div>
</div>

## Vaadin 8-based Quick Starters

<style>
.box {
  border-radius: 4px;
  padding: 16px 10px;
  margin: 10px;
  width: 170px;
  height: 130px;
  transition: box-shadow 200ms;
  transition-timing-function: cubic-bezier(0.55, 0, 0.1, 1);
  color: rgba(0, 0, 0, 0.6);
  cursor: pointer;
}
.box:hover {
  box-shadow: 0 5px 10px rgba(0,0,0,.15);
}
.app {
  background: rgb(221, 201, 230);
}
.fe {
  background: rgb(129, 199, 132);
}
.md {
  background: rgb(255, 255, 255);
}
.go {
  background: rgb(100, 181, 246);
}
.au {
  background: rgb(255, 183, 77);
}
.pt {
  background: rgb(207, 216, 220);
}
.box .caption {
  font-size: 22px;
  font-family: Arvo, Monaco, serif;
}
.box .body {
  padding-top: 8px;
  font-size: 14px;
}
</style>

Vaadin 8 is a mature and proven web framework. If you need production-grade stability, start here.

Every VoK project tends to have several files (database migrations, Gradle build script, themes, logger configuration etc) and our project will be no exception. Therefore, it makes sense to
have an archetype app with all of those files already provided.

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu-helloworld-application';" class="box md"><div class="caption">UI Base</div><div class="body">A project with one view and no db; perfect for your UI experiments</div></div>
<div onclick="location.href='https://github.com/mvysny/karibu-dsl/tree/master/karibu-dsl-v8#component-palette-app';" class="box fe"><div class="caption">Component Palette</div><div class="body">App which demoes all Vaadin components and the Navigator; no db</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-helloworld-app';" class="box app"><div class="caption">VoK Project Base</div><div class="body">Skeletal app with support for SQL db; start building your app here</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-on-kotlin#example-project';" class="box go"><div class="caption">VoK Full Stack</div><div class="body">Full-stack with db and a db table editor; a good source of code examples</div></div>
</div>

## Complete List Of Examples

The following is a complete list of all example projects demonstrating vok or its parts, in alphabetical order:

* [beverage-buddy-vok](https://github.com/mvysny/beverage-buddy-vok): VoK, Vaadin 10, Kotlin, Gradle, DynaTest
* [karibu-helloworld-application](https://github.com/mvysny/karibu-helloworld-application): Karibu-DSL, Vaadin 8, Kotlin, Gradle, DynaTest
* [karibu-helloworld-application-maven](https://github.com/mvysny/karibu-helloworld-application-maven): Karibu-DSL, Vaadin 8, Kotlin, Gradle
* [karibu-testing-spring](https://github.com/mvysny/karibu-testing-spring): Karibu-DSL, Vaadin 8, Java, Karibu-Testing, Maven, Spring Boot, JUnit 4
* [karibu10-helloworld-application](https://github.com/mvysny/karibu10-helloworld-application): Karibu-DSL, Vaadin 10, Kotlin, Gradle, Karibu-Testing, DynaTest
* [vaadin-coroutines-demo](https://github.com/mvysny/vaadin-coroutines-demo): Karibu-DSL, Vaadin 8, Kotlin, Gradle, Karibu-Testing, DynaTest
* [vaadin-kotlin-pwa](https://github.com/mvysny/vaadin-coroutines-demo): VoK, Vaadin 10, Kotlin, Gradle, Karibu-Testing, DynaTest
* [vaadin10-sqldataprovider-example](https://github.com/mvysny/vaadin10-sqldataprovider-example): Vaadin 10, Java, Maven, Karibu-Testing, JUnit 5
* [vaadin8-jpadataprovider-example](https://github.com/mvysny/vaadin8-jpadataprovider-example): Vaadin 8, Java, Maven, Karibu-Testing, JUnit 5
* [vaadin8-sqldataprovider-example](https://github.com/mvysny/vaadin8-sqldataprovider-example): Vaadin 8, Java, Maven, Karibu-Testing, JUnit 5
* [vok-helloworld-app](https://github.com/mvysny/vok-helloworld-app): Vaadin 8, Kotlin, Gradle
* [vok-helloworld-app-v10](https://github.com/mvysny/vok-helloworld-app-v10): Vaadin 10, Kotlin, Gradle
* [vok-security-demo](https://github.com/mvysny/vok-security-demo): Vaadin 8, Kotlin, Gradle, Karibu-Testing, DynaTest
* [vok-security-demo-v10](https://github.com/mvysny/vok-security-demo-v10): Vaadin 10, Kotlin, Gradle, Karibu-Testing, DynaTest

<sub><sup>This work is licensed under a [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/)</sup></sub>
