[Index](index.html) | [Getting Started](gettingstarted.html)

# Vaadin-on-Kotlin

A framework which allows you to design your UI and access your database from a Servlet container:
```kotlin
button("Create") {
  onLeftClick { db { em.persist(person) } }
}
```
No JavaEE or Spring needed. We at Vaadin fight for
simplicity and invite everyone to join this fight.

## QuickStart

#### Run the example application from the command-line

You will only need Java 8 JDK installed. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:appRun
```

The web app will be running at [http://localhost:8080](http://localhost:8080)

To get started with Vaadin-on-Kotlin, [Start Here](gettingstarted.html).

TODO more documentation coming, for now please visit the [https://github.com/mvysny/vaadin-on-kotlin](https://github.com/mvysny/vaadin-on-kotlin) GitHub pages.
