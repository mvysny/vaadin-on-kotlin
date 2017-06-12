[Index](index.html) | [Getting Started](gettingstarted.html)

# Vaadin-on-Kotlin

Vaadin-on-Kotlin is a web-application framework that includes everything needed to create database-backed web applications:

```kotlin
button("Create") {
  onLeftClick { db { em.persist(person) } }
}
```

No JavaEE nor Spring needed; all complex features are deliberately left out, which makes Vaadin-on-Kotlin a perfect
starting point for beginner programmers.

## QuickStart

You will only need Java 8 JDK and git installed to run the example application from the command-line. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080)

## Tutorial

To get started with Vaadin-on-Kotlin, [Start Here](gettingstarted.html). To find out more, please visit the [Vaadin-on-Kotlin GitHub project page](https://github.com/mvysny/vaadin-on-kotlin).

<sub><sup>This work is licensed under a [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/)</sup></sub>
