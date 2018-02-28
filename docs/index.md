[Index](index.html) | [Getting Started](gettingstarted.html)

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

VoK is not yet another HTTP route mapping library. On the contrary: it provides you with a 
[wide palette of built-in powerful components](https://karibu-uitest.herokuapp.com/): lazy paged tables, color pickers, menu components, sliders; allows drag'n'drop between those components.
All components have rich JavaScript facade which provides rich functionality. Vaadin transparently handles the state synchronisation between the client part and
the server part of the component which allows you to focus on wiring the components in pure server-side Kotlin code.

Because of that, VoK feels more of a client widget library (such as JavaFX) than a web page-based framework.

## QuickStart

You will only need Java 8 JDK and git installed to run the example application from the command-line. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud-sql2o:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080)

## Tutorial

To get started with Vaadin-on-Kotlin, [Start Here](gettingstarted.html). To find out more, please visit the [Vaadin-on-Kotlin GitHub project page](https://github.com/mvysny/vaadin-on-kotlin).

To see the old JPA-based tutorial, [Start Here](gettingstartedjpa.html)

## About

Vaadin-on-Kotlin is a puzzle which contains the following items:

<style>
.box {
  border-radius: 4px;
  padding: 16px 10px;
  margin: 10px;
  width: 170px;
  height: 118px;
  transition: box-shadow 200ms;
  transition-timing-function: cubic-bezier(0.55, 0, 0.1, 1);
  display: inline-block;
  color: rgba(0, 0, 0, 0.6);
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
.box .caption {
  font-size: 22px;
  font-family: Arvo, Monaco, serif;
}
.box .body {
  padding-top: 8px;
  font-size: 14px;
}
</style>

<div onclick="location.href='https://github.com/mvysny/karibu-dsl';" class="box app"><div class="caption">Karibu-DSL</div><div class="body">Write your UI in structured Kotlin code</div></div>
<div onclick="location.href='https://github.com/mvysny/karibu-testing';" class="box fe"><div class="caption">Karibu-Testing</div><div class="body">Test your UI with speed and reliability</div></div>

<sub><sup>This work is licensed under a [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/)</sup></sub>
