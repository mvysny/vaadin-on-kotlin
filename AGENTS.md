# Vaadin-on-Kotlin — AGENTS.md

## What this is

Vaadin-on-Kotlin is a web-application framework for database-backed apps in Kotlin. It enforces neither MVC,
dependency injection nor service-oriented architecture, and uses neither Spring nor JavaEE. The view layer is
[Vaadin](https://vaadin.com)'s component-oriented model; persistence is [ktorm](https://www.ktorm.org/) — typed-SQL
DSL, entity sequences, no XML — wrapped by [ktorm-vaadin](https://github.com/mvysny/ktorm-vaadin). VoK owns the glue
between the two plus REST server and client support; depend on `vok-framework` alone and bring your own persistence.

## Promises

- **No container, no magic.** No DI, no MVC, no service layer: a VoK app is plain Kotlin that calls Vaadin and the database directly.
- **Upstream stays reachable.** VoK is glue, never a facade — Vaadin and ktorm APIs stay in full view, and an app may depend on `vok-framework` alone and bring its own persistence.
- **Typed from view to SQL.** Views, queries and REST filters are Kotlin the compiler checks; no XML, no string-named beans.

## Design docs

| File | Owns | Loaded |
|---|---|---|
| `README.md` | the pitch, getting started, the code examples | — |
| `AGENTS.md` (this) | promises, invariants, the module map, conventions, commands | every turn |
| `design/architecture.md` | how the modules compose — dependency direction, the boot sequence, the flows; normative | lazy |
| `design/decisions.md` | why this and not that — `D_` entries, FAQ-shaped | lazy |
| `design/research.md` | what the libraries we don't own actually do — `R_` entries, each claim with provenance | lazy |
| `<module>/README.md` | how to use one published module: its API surface and its wire format | — |
| `CONTRIBUTING.md` | how to contribute, and the release-to-Maven-Central procedure | — |
| `docs/` | the Jekyll site behind www.vaadinonkotlin.eu — the user-facing guides | — |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Invariants

- **Module dependencies point one way.** `vok-framework` ← `vok-framework-vokdb` ← `vok-rest`; a DB dependency in `vok-framework`, or an ORM one in `vok-rest-client`, breaks every app that brings its own persistence. The composition is `design/architecture.md`'s.
- **`VaadinOnKotlin.dataSource` is assigned before `init()`.** Its setter is what connects `ActiveKtorm.database`, so anything ktorm does before that assignment throws.
- **Every database access runs inside `db { }`.** `database` and the enclosing transaction exist only on that receiver; a query outside one has no connection to run on.
- **A DB test deletes its own rows.** The H2 in-memory database lives for the whole run, so leftovers fail the next test — see `AbstractAppTest`'s `@BeforeEach @AfterEach cleanupDb`.

## Module map

- `vok-framework` — the core: bootstrap, `Session`, `Cookies`, async executor, i18n, Vaadin helpers. No DB.
- `vok-framework-vokdb` — Vaadin + SQL: `VaadinOnKotlin.dataSource`, `toId()`, `enumFilterField()`; re-exports ktorm-vaadin.
- `vok-rest` — REST server: Javalin + Gson, `Table<E>.getCrudHandler()`, `Javalin.crud2()`.
- `vok-rest-client` — REST client over the JDK `HttpClient`: `CrudClient<T>`, Gson converters. No ORM.
- `vok-example-crud` — the runnable demo, and the framework's integration-test harness.

## Conventions

- **Kotlin, JDK 21 floor.** Java source/target and Kotlin's `jvmTarget` are pinned to 21 — Vaadin 25's minimum.
- **Published modules declare `explicitApi()`.** Every new top-level or public declaration needs an explicit visibility modifier; `vok-example-crud` is exempt.
- **Dependencies come from `gradle/libs.versions.toml`.** A literal `"group:artifact:version"` in a `build.gradle.kts` is reserved for the `junit-platform-launcher` test line.
- **A new published module calls `configureMavenCentral(artifactId, description)`** at the bottom of its `build.gradle.kts`, exactly like the existing ones.
- **Tests: JUnit Jupiter + Karibu-Testing.** `MockVaadin.setup(routes)` in `@BeforeEach`, `MockVaadin.tearDown()` in `@AfterEach`; `vok-example-crud/src/test/kotlin/example/crudflow/AbstractAppTest.kt` is the pattern.
- **Session-scoped state is a `Session.getOrPut { }` extension property**, never a container — see `D_no_di`.
- **Check Maven Central before bumping a version**, through the `maven-tools` MCP; the pins that lag on purpose are `R_pinned_deps`.
- **Pre-1.0: break APIs freely**, with a note at the top of the README and the new shapes in the per-module READMEs.

## Commands

- `./gradlew build` — full build + tests; also the default task (`clean build`), and what CI runs (`.github/workflows/gradle.yml`, JDK 21/24 × Linux/macOS/Windows).
- `./gradlew test` / `./gradlew :vok-framework:test` / `./gradlew :vok-rest:test --tests '*PersonRestTest*'` — every test, one module, one class.
- `./gradlew vok-example-crud:run` — the demo app on http://localhost:8080.
- `./gradlew clean build -Pvaadin.productionMode` — production Vaadin build (npm bundling, prod frontend).
- `./design/verify_design_tripwires.sh` — the doc-layer checks; wired into `./gradlew check` (skipped on Windows) and run as its own CI job.

## Skills this project follows

- **Component-oriented:** self-sufficient components that reach the DB directly, no MVC layers; the `cop` skill has the rules.
- **KDoc:** document the contract, cut what the code already says, and keep each fact at the level it belongs; the `writing-kdoc` skill.
- **Karibu-Testing:** browserless Vaadin tests through `MockVaadin` and `_get` / `_click` lookups; the `karibu-testing` skill.

## Maintenance of this file

Loaded every turn; cap 34 KB, a module's own `AGENTS.md` 10 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the caps and the cites.
