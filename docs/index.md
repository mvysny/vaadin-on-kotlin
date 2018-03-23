[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Vaadin-on-Kotlin

Vaadin-on-Kotlin (or VoK for short) is a web-application framework that includes everything necessary to create database-backed web applications in server-side Kotlin:

```kotlin
button("Create") {
  onLeftClick { db { person.save() } }
}
```

No JavaEE nor Spring needed; all complex features are deliberately left out, which makes Vaadin-on-Kotlin a perfect
starting point for beginner programmers.

With VoK you only write server-side Kotlin code - no JavaScript and no CSS is necessary until much later on, when you decide
to style up your application and/or write your own custom rich component. 

VoK is not just yet another HTTP route mapping library. On the contrary: it is built on Vaadin which provides you with a 
[wide palette of built-in powerful components](https://karibu-uitest.herokuapp.com/): lazy paged tables, color pickers, menu components, sliders; allows drag'n'drop between those components.
All components have rich JavaScript facade which provides rich functionality. Vaadin transparently handles the state synchronisation between the client part and
the server part of the component which allows you to focus on wiring the components in pure server-side Kotlin code.

Because of that, VoK feels more of a desktop widget library (such as Swing or JavaFX) than a web page-based framework.

## Tutorial

To get started with Vaadin-on-Kotlin, [Start Here](gettingstarted.html). To find out more, please visit the [Vaadin-on-Kotlin GitHub project page](https://github.com/mvysny/vaadin-on-kotlin).

To see the old JPA-based tutorial, [Start Here](gettingstartedjpa.html)

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

Every VoK project tend to have several files (database migrations, Gradle build script, themes, logger configuration etc), it makes sense to
have an archetype app with all of those files already provided.

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu-helloworld-application';" class="box md"><div class="caption">UI Base</div><div class="body">A project with one view and no db; perfect for your UI experiments</div></div>
<div onclick="location.href='https://github.com/mvysny/karibu-dsl#quickstart';" class="box fe"><div class="caption">Component Palette</div><div class="body">App which demoes all Vaadin components and the Navigator; no db</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-helloword-app';" class="box app"><div class="caption">VoK Project Base</div><div class="body">Skeletal app with support for SQL db; start building your app here</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-on-kotlin#example-project';" class="box go"><div class="caption">VoK Full Stack</div><div class="body">Full-stack with db and a db table editor; a good source of code examples</div></div>
</div>

## Vaadin 10-based Quick Starters

Vaadin 10 components are based on the Web Components standard; Vaadin 10-based apps are also themable more easily than Vaadin 8-based apps.

Every VoK project tend to have several files (database migrations, Gradle build script, themes, logger configuration etc), it makes sense to
have an archetype app with all of those files already provided.

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu10-helloworld-application';" class="box md"><div class="caption">UI Base</div><div class="body">A project with one view and no db; perfect for your UI experiments</div></div>
<div onclick="location.href='https://github.com/mvysny/beverage-buddy-vok';" class="box app"><div class="caption">Full Stack App</div><div class="body">The "Beverage Buddy" app backed by SQL db; demoes two tables</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-kotlin-pwa';" class="box au"><div class="caption">Full Stack PWA</div><div class="body">Full-stack task list app backed by SQL db; for desktop and mobile browsers</div></div>
</div>

## Further Reading

For technical description of what Vaadin-on-Kotlin is, please feel free to read the [Vaadin-on-Kotlin Github page](https://github.com/mvysny/vaadin-on-kotlin).

Vaadin-on-Kotlin apps are typically three-tiered:

* The browser renders HTML and JavaScript constructed by the components orchestrated by the Vaadin framework. Vaadin offers the possibility to orchestrate
  interactive components entirely server-side using a rich Java API. The Karibu-DSL library wraps Vaadin APIs to provide more pleasant Kotlin experience.
* Server-side code is typically written using the Kotlin language. This is where your app logic resides and this is where you will add your code.
* The database access is handled by the VoK-ORM library: a very simple and powerful layer over a SQL database. You can of course decide not to use
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

<sub><sup>This work is licensed under a [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/)</sup></sub>
