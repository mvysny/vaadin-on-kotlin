# Architecture

How the pieces compose — what no single symbol can say and what would be expensive to overturn:
wiring and dependency direction, the boot and data-flow story, the flows a newcomer needs, where to
start reading. **Normative: the code conforms.** Change this file first, then the code. Not here:
why (`decisions.md` — cite the `D_`), what upstream does (`research.md` — cite the `R_`), one
symbol's behaviour (its doc comment), the module map (`AGENTS.md`). Cap 12 KB — over it, research
or doc-comment content has crept in.

## Wiring

- Dependencies point toward data: `vok-rest` → `vok-framework-vokdb` → `vok-framework`, and `vok-rest-client` → `vok-framework` only. Nothing in `vok-framework` knows about SQL and nothing in `vok-rest-client` knows about ktorm — that is what lets an app keep the Vaadin half and replace the persistence half.
- There is no container and no registry (`D_no_di`). `VaadinOnKotlin` is a Kotlin `object` holding the async executor and the started flag; the database hangs off it as `VaadinOnKotlin.dataSource`, an extension property declared in `vok-framework-vokdb` whose setter also runs `ActiveKtorm.database = Database.connect(value)`. That one assignment is the entire seam between the Vaadin half and the ktorm half.
- Longer-lived state is reached, not injected: session-scoped state is a `Session.getOrPut { }` extension property, app-scoped singletons are `Services`, and a view reaches the database through `db { }` itself.
- ktorm-vaadin owns the grid-to-SQL seam: `Table<E>.dataProvider` is an `EntityDataProvider` that turns Vaadin's `Query` — offset, limit, sort — plus one ktorm `ColumnDeclaring<Boolean>` filter into a single statement. VoK adds two Vaadin-side pieces around it — `toId()` for `Binder` and `enumFilterField()` — and nothing else; the filter components are ktorm-vaadin's (`R_ktorm_vaadin`).
- `vok-rest` hangs off the same `Table<E>`: `KtormCrudHandler` reads the table's `Column`s for filter and sort names and its `NestedBinding`s for JSON keys, so a REST endpoint and a grid agree on names without either knowing about the other.
- The app is servlet-hosted, not framework-hosted: `@WebListener Bootstrap` owns the lifecycle, Vaadin's servlet serves `/`, and an `HttpServlet` delegates `/rest/*` to a standalone Javalin instance. Vaadin Boot's embedded Jetty in `MainKt#main` supplies the container; in tests, a real Jetty does.

## Flows

**Boot** (`Bootstrap#contextInitialized`, once per app — and once per test class, via `AbstractAppTest`):

1. Build a `HikariDataSource`.
2. `VaadinOnKotlin.dataSource = ds` — the setter connects `ActiveKtorm.database`; everything ktorm does depends on this having happened first.
3. `VaadinOnKotlin.init()` — starts the scheduled executor and installs the i18n lookup into karibu-dsl.
4. Flyway migrates `VaadinOnKotlin.dataSource`.
5. Shutdown mirrors it: `VaadinOnKotlin.destroy()` destroys the `Services` singletons and drains the executor.

**A filtered grid** (every interaction with a filter component):

1. A filter component — `FilterTextField`, `NumberRangePopup`, `EnumFilterField` — fires its value-change listener on the view that owns it.
2. The view folds the current filter values into one ktorm `ColumnDeclaring<Boolean>` and calls `dataProvider.setFilter(...)`.
3. Vaadin re-runs the data provider; `EntityDataProvider` composes that filter with the `Query`'s offset, limit and sort clauses.
4. `db { }` opens a transaction on `ActiveKtorm.database`: one select for the visible page, one `count` for the scrollbar.
5. Rows come back as ktorm entity proxies and are rendered by the grid's columns; the view holds no copy of them.

**A REST list call** (`GET /rest/person?name=Leto&limit=20&sort=age:desc`):

1. The servlet hands the request to the standalone Javalin instance, where `crud2` routes it to `KtormCrudHandler#getAll`.
2. `offset`, `limit`, `sort` and `count` are reserved; every other query parameter becomes an eq filter, its column looked up by **SQL** column name and its value coerced through that column's ktorm `SqlType`.
3. `db { }` runs one select — or one `count`, when `?count=true`, which answers as plain text.
4. Each entity is snapshotted into a `LinkedHashMap` keyed by **property** name and serialized by `VokRest.gson`, so a plain DTO on the client round-trips without an adapter.
5. An unknown column, an uncoercible value or a bad sort direction is a `BadRequestResponse` — 400 with a plain-text reason, never a stack trace.

## Where to start reading

`vok-example-crud/src/main/kotlin/example/crudflow/` — `Bootstrap.kt` is the whole boot sequence in forty lines, `person/PersonListView.kt` the grid-filter-SQL flow, `PersonRest.kt` the REST wiring. Then the framework modules, in dependency order.
