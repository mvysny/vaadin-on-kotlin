# Release the Javalin 7 upgrade, then bring the site tutorial up to it

Commit `8a696ceb` upgraded every dependency, Javalin 5.6.3 → 7.2.4 among them, and that breaks
`vok-rest`'s API: `Javalin.crud2()` is now `RoutesConfig.crud2()`, and `Javalin.createStandalone()` is gone.
The site tutorial (www.vaadinonkotlin.eu/tutorial) still teaches the Javalin 5 shape, which is right for
the released 0.19.0 but wrong as soon as the next release ships. So the order matters: release first,
then the starter repo, then the tutorial.

## 1. Release

- Procedure: `CONTRIBUTING.md`, "Releasing". The version is currently `0.19.1-SNAPSHOT` in the root
  `build.gradle.kts`.
- `Q_version`: 0.19.1 or 0.20.0? The `vok-rest` API break suggests 0.20.0. Pre-1.0 allows breaking
  changes in a patch release, but a patch number tells users it's a drop-in upgrade, and this one isn't.
- The README's top note ("`vok-rest` moved from Javalin 5 to Javalin 7") names no version. Add it at
  release time, the way the 0.19 note does.

## 2. The starter repo: github.com/mvysny/vok-helloworld-app

Chapter 0 has readers clone its default branch (the starter), and the tutorial's intro points at its
`complete` branch for the finished app. Both branches need the new VoK version.

- Starter branch: `Bootstrap.kt` has the placeholder REST servlet and an empty
  `fun Javalin.configureRest(): Javalin { return this }`. Change that to the Javalin 7 shape (below).
- `complete` branch: the full Chapter 11 code, ported the same way.
- Also check the other branches the tutorial links to, if any.

## 3. The tutorial: `docs/tutorial.md`, Chapter 11 "A REST API for the catalog"

Only the wiring changes. Javalin 7's `config.routes` is a `RoutesConfig`, which still has `get` /
`post` / `exception`, and `ctx.bodyAsClass` / `ctx.pathParam` are unchanged (checked on 7.2.4). So the
handler bodies and their explanations stay as they are:

```kotlin
class JavalinRestServlet : HttpServlet() {
    val javalin: Servlet = Javalin.create {
        it.startup.startupWatcherEnabled = false
        it.gsonMapper(VokRest.gson)
        it.routes.configureRest()
    }.javalinServlet()
    // service() unchanged
}

fun RoutesConfig.configureRest() {
    get("/api/products") { ctx -> /* unchanged */ }
    // ... post, exception: unchanged; drop the trailing `return this`
}
```

- Two snippets to change: the Chapter 0 placeholder that Step 2 quotes (around line 2633) and Step 2's
  full code (around line 2643).
- Imports: `io.javalin.config.RoutesConfig` and `jakarta.servlet.Servlet` replace
  `io.javalin.http.servlet.JavalinServlet`.
- One sentence on `startupWatcherEnabled = false`: Javalin only serves through the servlet and never
  starts its own Jetty. Without the flag it logs "you never started it" 5 seconds after creation.
- Step 3's `curl` output: the 404 body for an unknown *route* is now "Endpoint GET /x not found". The
  chapter's own `NotFoundResponse` messages are unaffected. Re-run Step 3 against the `complete` branch
  and paste the real output.
- `Q_test_the_snippets`: build the `complete` branch and run its tests first, then copy the snippets
  from it, rather than editing the Markdown by hand.

## 4. Other site cleanups to ship with it

- `docs/index.md` lines 10–14: the "Heads up" box says the tutorial and the NoSQL/REST guide still
  teach `vok-orm` and the old `Condition` filters. Neither does; both are already on ktorm. Delete the
  box.
- Build the site locally before pushing (`docs/mise.toml` pins Ruby 3.3, gems in `docs/vendor/bundle`):
  `bundle exec jekyll build`.

## Graduation

Once released and the tutorial is live: the version goes into the README note, and this file is
deleted. Nothing here belongs in `design/`; the steps are one-off.
