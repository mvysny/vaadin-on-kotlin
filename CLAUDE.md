# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Vaadin-on-Kotlin (VoK) is a published **library** (`eu.vaadinonkotlin:*` on Maven Central), not an application. The repo is a multi-module Gradle build of the library's modules plus one demo app (`vok-example-crud`) used both as a runnable example and as the integration-test harness for the framework.

Home page: https://www.vaadinonkotlin.eu

Current library version lives in `build.gradle.kts` (`allprojects { version = ... }`). Release procedure is in `CONTRIBUTING.md`.

## Build / test commands

```bash
./gradlew build                       # full build + tests (also the default task: clean + build)
./gradlew test                        # all unit tests across modules
./gradlew :vok-framework:test         # tests for one module
./gradlew :vok-rest:test --tests '*PersonRestTest*'   # single test class
./gradlew vok-example-crud:run        # launch demo app on http://localhost:8080
./gradlew clean build -Pvaadin.productionMode        # production Vaadin build (npm bundling, prod frontend)
```

JDK 21 is the minimum (Vaadin 25 requirement); CI matrix runs JDK 21/24 on Linux/macOS/Windows. Both source/target Java compatibility and Kotlin's `jvmTarget` are pinned to 21.

## Module structure & boundaries

Dependency direction is strict; respect it when adding code:

- `vok-framework` — core. Bootstrap (`VaadinOnKotlin.init()/destroy()`), `Session`, `Cookies`, async executor, i18n bundle, Vaadin/Karibu helpers. Depends on `karibu-dsl` (the main artifact, not the `-v23` variant — that variant pins Vaadin 23) and `vaadin-core`. No DB dependency.
- `vok-framework-vokdb` — Vaadin + SQL via [ktorm](https://www.ktorm.org/) + [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin). Provides `VaadinOnKotlin.dataSource` (which also wires `ActiveKtorm.database`), `toId()` Binder helper, and `enumFilterField()`. Depends on `vok-framework` + `ktorm-vaadin`. ktorm-vaadin re-exports `Table<E>.dataProvider`, `EntityDataProvider`, `EntityToIdConverter`, and the filter components (`FilterTextField`, `DateRangePopup`, `NumberRangePopup`, `BooleanFilterField`, `EnumFilterField`).
- `vok-rest` — REST **server** support. Javalin 5 + Gson. Exposes ktorm tables as CRUD endpoints via `KtormCrudHandler<E>` / `Table<E>.getCrudHandler()`. Depends on `vok-framework-vokdb`. Read endpoints (GET-all, GET-one, ?count=true) are implemented; create/update/delete return 501 pending a Gson↔ktorm Entity adapter.
- `vok-rest-client` — REST **client** helpers built on the JDK `HttpClient`. No ORM dependency. `CrudClient<T>` is `AbstractBackEndDataProvider<T, Map<String, String>>` — caller supplies an eq-only filter map and pre-formats values as strings.
- `vok-example-crud` — runnable demo (Vaadin Boot, embedded Jetty via `MainKt#main`) and the de-facto integration test for the published modules. Also where you'll find end-to-end test patterns (`AbstractAppTest`, `MockVaadin.setup(routes)`).

Add a dependency to a published module only through the version catalog (`gradle/libs.versions.toml`) — direct `"group:artifact:version"` strings in `build.gradle.kts` are reserved for the test JUnit-platform-launcher line.

## Published-API contract

All library modules declare `kotlin { explicitApi() }`. Every new top-level/public declaration needs an explicit visibility modifier (`public`, `internal`, …) — the compiler will reject it otherwise. The example app (`vok-example-crud`) does not have this enabled.

Each published module wires up Maven Central + signing via the `configureMavenCentral(artifactId, description)` helper defined in the root `build.gradle.kts`. When adding a new published module, call this helper at the bottom of its `build.gradle.kts` exactly like the existing ones do.

## Testing conventions

- **JUnit Jupiter (JUnit 6)** — every module's `build.gradle.kts` uses `useJUnitPlatform()`. Tests use `@BeforeAll`/`@BeforeEach`/`@Test` from `org.junit.jupiter.api`.
- **Karibu-Testing v24** is the Vaadin testing layer (`MockVaadin.setup(routes)` in `@BeforeEach`, `MockVaadin.tearDown()` in `@AfterEach`). See `vok-example-crud/src/test/kotlin/.../AbstractAppTest.kt` for the canonical lifecycle pattern.
- **DB tests** boot the app via `Bootstrap().contextInitialized(null)` and use H2 in-memory; tests are expected to clean their own rows (`Persons.deleteAll()` — the ktorm-vaadin `Table<E>` extension — between tests).
- `vok-rest-client` has minimal tests of its own — it's exercised through `vok-rest`'s and `vok-example-crud`'s test suites, which spin up a real Jetty.

## Bootstrap pattern

VoK has no DI container. The expected app shape (mirrored in `vok-example-crud/.../Bootstrap.kt`) is:

1. Build a `HikariDataSource`, assign to `VaadinOnKotlin.dataSource` (extension from `vok-framework-vokdb`). The setter also wires `ActiveKtorm.database = Database.connect(value)` so ktorm queries work without an extra step.
2. `VaadinOnKotlin.init()`.
3. Run Flyway migrations against `VaadinOnKotlin.dataSource`.
4. On shutdown: `VaadinOnKotlin.destroy()`.

Entities are ktorm `Entity<E>` interfaces (proxied; backed by a `LinkedHashMap`). Use `ActiveEntity<E>` (from ktorm-vaadin) to get `save()` / `create()` / `delete()` instance methods. Define a `Table<E>` object alongside each entity. Run queries inside `db { database.sequenceOf(Foos)...  }` blocks — `db { }` wraps `useTransaction` and exposes `database` + `transaction` on a `KtormContext` receiver.

Session-scoped state goes on the `Session` object via `getOrPut { … }` extension properties (see `vok-framework/README.md` "Support for Session"); don't introduce a DI framework to solve this.

## Library version checks

When bumping a dependency in `gradle/libs.versions.toml`, verify the latest GA on Maven Central via the `maven-tools` MCP — training data lags reality. Notable pins that intentionally lag and should NOT be auto-bumped without thought:

- **Javalin is pinned to 5.6.3** (`libs.javalin`). Javalin 7 is now out and supports Jetty 12 (vaadin-boot itself moved to 7.2.0), so this pin can be revisited — but the bump is unrelated to the Vaadin 25 work and was deferred. The catalog comment still references the old "wait for Javalin 7" rationale.
- **Jetty** is on the `ee10` artifacts (`jetty-ee10-webapp`, `jetty-ee10-websocket-jakarta-server`) — match the `ee10` namespace when editing. Vaadin 25.1 still works on Jakarta EE 10 (Servlet 6.x), so we have not moved to `ee11`.
- **Karibu-Testing** artifact id is `karibu-testing-v24` despite the name (a historical holdover); versions 2.6.x+ support Vaadin 25. Do not switch the artifact id to a non-existent `karibu-testing-v25` — it does not exist on Maven Central as of this writing.
- **`ktorm-vaadin` is at 0.1** (single-maintainer, the same author as the VoK example app `mvysny/beverage-buddy-ktorm`). Brings ktorm-core 4.1.1, ktorm-support-postgresql (only for `ilike`, which happens to work on H2 too), and Hibernate-Validator 9 (required for validating interface-backed entity getters) transitively.
