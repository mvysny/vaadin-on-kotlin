# Decisions

Why this project is the way it is and not otherwise — FAQ-shaped: each entry is a question and its
current answer. Rewrite the answer when it changes; delete the entry when nobody asks any more. An
entry is earned by what it would cost to reverse — half the code base — or by research the next
person would otherwise redo (cited as its `R_`). Not an entry: the testing library, the CI host, a
version bump, a naming preference — those are a comment at the site of the choice, or nothing. Cite
by slug, `D_<slug>`, never by position; `grep '^## D_' design/decisions.md` is the index. The first
entry is the ruler: every later one trims to its length.

---

## D_no_di — Why no dependency injection container?

Because dependency injection is an antipattern, and VoK is built on that belief. What it costs is
code locality and the ability to navigate: ctrl-clicking a dependency lands on an interface, and
what actually runs there is decided by configuration written somewhere else, so the reader pays a
lookup at every hop — the argument in full is
[Code locality and ability to navigate](https://mvysny.github.io/code-locality-and-ability-to-navigate/).
The size of the container is not the variable: Spring and Koin are rejected on identical grounds, a
"lightweight" DI library being the same indirection in a smaller jar. So a VoK app wires itself in
one method — `VaadinOnKotlin.dataSource = …`, then `init()`, inside a `ServletContextListener` — and
everything else is reached where it is needed: a view calls `db { }` directly, session state is a
`Session.getOrPut { }` extension property, an app-scoped singleton is a `Services` entry. Every call
site names the thing it calls. This is the **No container, no magic.** promise, and it is why
`vok-framework` carries no DB dependency: composition is a dependency on a module, not a wiring
file. What we carry for it: no constructor seam to hang a fake on, so tests boot the real thing —
`Bootstrap().contextInitialized(null)` against H2 in memory, Karibu-Testing for the UI.

## D_ktorm — Why ktorm rather than the in-house vok-orm?

0.19 moved persistence from vok-orm — a jdbi-orm wrapper maintained alongside VoK — to ktorm plus
ktorm-vaadin (`R_ktorm_vaadin`). The requirement that decided it: the object a query returns must be
the object the UI binds. A ktorm entity is a bean — `Person` is an interface of `var` properties
carrying JSR-380 annotations on its getters — so it goes straight into a Vaadin `Binder` and into a
`Grid` column, with no DTO in between. Second, a ktorm filter is a typed `ColumnDeclaring<Boolean>`
the compiler checks, and one such expression serves the grid's data provider, its count query and
`KtormCrudHandler` alike — the **Typed from view to SQL.** promise, with the SQL half on someone
else's bill. Why not keep vok-orm: its filters were a VoK-defined `Filter` hierarchy that VoK itself
turned into SQL, so every predicate a user wanted was ours to write, document and test, and ktorm's
DSL already covers what that layer existed for. Why not JPA/Hibernate: its entity is a managed object
whose persistence context has no natural scope in a Vaadin UI, and a lazily-loaded field fails at
the component that reads it. What it cost: a breaking release — entities are ktorm `Entity<E>`
interfaces, not data classes — the REST wire format cut down to eq-only filters, and a hand-written
entity snapshot in `KtormCrudHandler`, Gson being unable to reflect into a ktorm proxy
(`R_ktorm_vaadin`).
