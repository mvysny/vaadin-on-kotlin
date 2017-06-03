[Index](index.html) | [Getting Started](gettingstarted.html)

# Vaadin-on-Kotlin

A framework which allows you to design your UI and access your database in the following fashion
```kotlin
button("Create") {
  onLeftClick { db { em.persist(person) } }
}
```
without any need for JavaEE or Spring.

## QuickStart

#### Run the example application from the command-line

You will only need Java 8 JDK installed. Just type this into your terminal:

```bash
git clone https://github.com/mvysny/vaadin-on-kotlin
cd vaadin-on-kotlin
./gradlew vok-example-crud:appRun
```

The web app will be running at [http://localhost:8080/vok-example-crud](http://localhost:8080/vok-example-crud)

To get started with VOK, [Start Here](gettingstarted.html).

TODO more documentation coming, for now please visit the [https://github.com/mvysny/vaadin-on-kotlin](https://github.com/mvysny/vaadin-on-kotlin) GitHub pages.
