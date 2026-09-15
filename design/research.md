# Research — the VoK dependency stack: Vaadin 25.1, ktorm-vaadin, Javalin, Jetty 12, Karibu-Testing, H2

What the things we don't own actually do. About *them*, never us: a sentence starting "we chose" is
a `D_`. `## R_<slug> — <title>`, one claim per bullet, one provenance marker per claim — **[docs]**,
**[src]**, **[verified <date>, <version>]**, **[unverified]** (a hypothesis; a design built on it
says so). A claim is earned by its provenance, or by having cost real work to find out. Checked
against the versions pinned in `gradle/libs.versions.toml`; a version-sensitive claim names the
version it was seen on. Cite by slug, `R_<slug>`, never by position;
`grep '^## R_' design/research.md` is the index. The first entry is the ruler.

---

## R_pinned_deps — Catalog coordinates: the traps and the deliberate lags

- **karibu-dsl publishes two artifacts at one version.** `karibu-dsl` and `karibu-dsl-v23` are both at 2.6.0, so the version number does not tell them apart — the name is the only thing that does, and VoK wants the plain one. **[verified 2026-09-15, Maven Central]**
- **Karibu-Testing has no `-v25` artifact.** `com.github.mvysny.kaributesting:karibu-testing-v25` resolves to nothing at all, not to an old version; the `-v24` id is a historical holdover and 2.6.x onwards supports Vaadin 25. Newest is 2.7.2, this project pins 2.7.0. **[verified 2026-09-15, Maven Central]**
- **Javalin's newest release is 7.2.3**, two majors past the 5.6.3 pinned here. **[verified 2026-09-15, Maven Central]**
- Javalin 6 does not support Jetty 12 — the catalog comment's reason for that pin, carried over from 0.18 and not re-checked since. **[unverified]**
- **Jetty 12 ships parallel `ee10` and `ee11` namespaces** over one core, and the namespace must match across every Jetty artifact: `jetty-ee10-webapp` goes with `jetty-ee10-websocket-jakarta-server`, never with an `ee11` sibling. `ee10` is Jakarta EE 10 / Servlet 6.0. **[docs]**
- Vaadin 25.1.5 runs under Jetty's `ee10` environment. Vaadin's docs state no minimum Servlet version, so this is observation rather than a documented guarantee. **[unverified]**
- **ktorm-vaadin 0.2 is the newest release**, and brings ktorm-core, ktorm-support-postgresql and Hibernate-Validator transitively. **[verified 2026-09-15, Maven Central]**
- Bean Validation constrains a *getter*, so annotations sit on an interface's `@get:` accessors and Hibernate-Validator reads them there — which is why a ktorm entity, being an interface, validates at all. **[docs]**

## R_ktorm_vaadin — What ktorm-vaadin adds on top of ktorm

- It supplies the Vaadin seam over ktorm: `Table<E>.dataProvider` / `EntityDataProvider`, `EntityToIdConverter`, and the filter components `FilterTextField`, `DateRangePopup`, `NumberRangePopup`, `BooleanFilterField`, `EnumFilterField`. **[verified 2026-09-15, ktorm-vaadin 0.2]**
- `db { }` wraps ktorm's `useTransaction` and exposes `database` and `transaction` on a `KtormContext` receiver; `ActiveEntity<E>` adds `save()` / `create()` / `delete()` to an entity, and `Table<E>.deleteAll()` to a table. **[verified 2026-09-15, ktorm-vaadin 0.2]**
- A ktorm `Entity<E>` is a JDK dynamic proxy backed by a `LinkedHashMap`, so a reflection-based serializer finds no fields on it; `Entity.properties` reports exactly the properties that were assigned, which is how a partial update tells "set to null" from "absent". **[src]**
- `ilike` lives in `ktorm-support-postgresql`, yet emits SQL that H2 accepts too — so the Postgres support module is not Postgres-only in practice. **[unverified]**
- **H2 folds unquoted identifiers to upper case** unless the URL carries `DATABASE_TO_UPPER=FALSE`; ktorm quotes its identifiers, so without that flag a quoted `"Person"` fails to match a Flyway-created `PERSON`. **[docs]**
