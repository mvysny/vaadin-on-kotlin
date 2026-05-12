---
layout: default
title: Navigating
permalink: /navigating/
parent: Guides
nav_order: 3
---

<br/>
<details close markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>
<br/>

# Navigating In Your App

Vaadin's `Router` resolves URL paths to route targets — server-side components that get shown when the
user lands on a given URL. When the user navigates to `http://localhost:8080/invoices`, the router
locates the `@Route("invoices")`-annotated component, instantiates it, and adds it into the UI.

See the [Vaadin Routing & Navigation documentation](https://vaadin.com/docs/latest/flow/routing) for the
full reference; this guide focuses on the patterns VoK and karibu-dsl encourage.

## Defining a route

A route is any Vaadin `Component` annotated with `@Route`. There is no separate `Navigator` to register —
Vaadin scans the classpath at startup and wires routes automatically.

```kotlin
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Route("invoices")
@PageTitle("Invoices")
class InvoicesView : VerticalLayout() {
    init {
        // build the UI here
    }
}
```

Use `@Route("")` for the root view. To nest a view inside a shared layout (header, side nav, etc.),
implement `RouterLayout` and reference it from `@Route`:

```kotlin
class MainLayout : VerticalLayout(), RouterLayout {
    init {
        setSizeFull()
        // header, navigation, etc.
    }
}

@Route(value = "", layout = MainLayout::class)
@PageTitle("Home")
class WelcomeView : KComposite() { /* ... */ }

@Route(value = "invoices", layout = MainLayout::class)
@PageTitle("Invoices")
class InvoicesView : KComposite() { /* ... */ }
```

`vok-example-crud` follows exactly this shape — see `MainLayout.kt` and `PersonListView.kt`.

## Programmatic navigation

The plain Vaadin API is `UI.getCurrent().navigate(InvoicesView::class.java)`. karibu-dsl (via the
[`karibu-tools`](https://github.com/mvysny/karibu-tools) artifact, pulled in transitively) wraps this
in a more Kotlin-friendly helper:

```kotlin
import com.github.mvysny.kaributools.navigateTo

// no parameters — reified form
navigateTo<InvoicesView>()

// no parameters — KClass form (useful when the type isn't known at compile time)
navigateTo(InvoicesView::class)
```

Both delegate to the current `UI`. There is also a string-based form for arbitrary locations, including
ones that mix path and query parameters:

```kotlin
navigateTo("invoices/25?lang=en")
```

### Passing a single URL parameter (`HasUrlParameter`)

If your view takes a single value (an entity ID, a slug), implement `HasUrlParameter<T>`. Vaadin calls
`setParameter()` before the view is shown; if the parameter is missing or invalid, redirect away.

```kotlin
import com.github.mvysny.kaributools.navigateTo
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route

@Route("person")
class PersonView : VerticalLayout(), HasUrlParameter<Long> {
    override fun setParameter(event: BeforeEvent, id: Long?) {
        val person: Person = (
            if (id == null) null
            else db { database.sequenceOf(Persons).find { it.id eq id } }
        ) ?: return navigateTo<PersonListView>()
        // ...render person
    }
}
```

To navigate to it, pass the value alongside the route class:

```kotlin
navigateTo(PersonView::class, person.id!!)
```

The signature is `navigateTo(route: KClass<out C>, param: T?)` where `C: Component, C: HasUrlParameter<T>`,
so the compiler infers the parameter type from the view — you don't need to spell it out.

> Why no `navigateTo<PersonView>(id)` reified form? Reifying both `C` and `T` would force the caller to
> write `navigateTo<Long, PersonView>(id)`, which is uglier than the current shape. See the comment in
> `RouterUtils.kt` in `karibu-tools` for details.

### Named route parameters

For richer URL templates, use Vaadin's `@RouteAlias` / template parameters and pass a `RouteParameters`
map. Build URLs with `getRouteUrl()`:

```kotlin
import com.github.mvysny.kaributools.QueryParameters
import com.github.mvysny.kaributools.getRouteUrl
import com.github.mvysny.kaributools.navigateTo
import com.vaadin.flow.router.RouteParameters

val url = getRouteUrl(
    AdminView::class,
    routeParameters = RouteParameters("id", "25"),
    queryParameters = QueryParameters("lang=en"),
)
navigateTo(url)   // navigates to e.g. "admin/25?lang=en"
```

### Reading query parameters inside a view

karibu-tools adds a `get(name)` operator on `QueryParameters`, plus a `QueryParameters("foo=bar")` parser:

```kotlin
val params: QueryParameters = event.location.queryParameters
val lang: String? = params["lang"]                    // single value or null
val tags: List<String> = params.getValues("tag")      // multi-valued
```

`event` here is the `BeforeEvent` you receive in `setParameter()` (or `BeforeEnterEvent` from
`BeforeEnterObserver.beforeEnter()`).

## Linking — `RouterLink`

For declarative in-app links that don't trigger a full page reload, prefer `RouterLink` over `Anchor`.
karibu-dsl provides a DSL extension with three overloads:

```kotlin
import com.github.mvysny.karibudsl.v10.routerLink
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.router.HighlightConditions

// no parameter
routerLink(VaadinIcon.LIST, "Reviews", ReviewsList::class) {
    addClassName("main-layout__nav-item")
    highlightCondition = HighlightConditions.sameLocation()
}

// single typed URL parameter (HasUrlParameter<T>)
routerLink(text = "Edit", viewType = PersonView::class, parameter = person.id!!)

// named RouteParameters
routerLink(text = "Open", viewType = AdminView::class, parameters = mapOf("id" to "25"))

// no target — useful when the destination is set later, e.g. in HasUrlParameter.setParameter()
routerLink(text = "Loading…") { /* set route later via setRoute(...) */ }
```

`HighlightConditions.sameLocation()` highlights the link when the user is currently on its target, which
is what you usually want for side-nav items.

### Setting / changing a link's target after construction

```kotlin
import com.github.mvysny.kaributools.setRoute

val link = RouterLink()
link.setRoute(PersonView::class, person.id!!)
```

### Opening a link in a new tab

```kotlin
import com.github.mvysny.kaributools.setOpenInNewTab

routerLink(text = "Docs", viewType = DocsView::class) {
    setOpenInNewTab()
}
```

### Triggering navigation from a `RouterLink` programmatically

```kotlin
import com.github.mvysny.kaributools.navigateTo

val link: RouterLink = /* ... */
link.navigateTo()   // navigates to link.href
```

## Reacting to navigation events

Implement one of the Vaadin observer interfaces on your view:

- `BeforeEnterObserver.beforeEnter(BeforeEnterEvent)` — fires before the view is attached; can `forwardTo`
  or `rerouteTo` a different view (e.g. login redirect).
- `BeforeLeaveObserver.beforeLeave(BeforeLeaveEvent)` — fires before leaving the view; can show a
  "discard unsaved changes?" dialog.
- `AfterNavigationObserver.afterNavigation(AfterNavigationEvent)` — fires after the navigation is committed;
  use `event.routeClass` (karibu-tools extension) to learn the new active route.

```kotlin
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver

@Route("admin")
class AdminView : VerticalLayout(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!Session.loginManager.isLoggedIn) {
            event.forwardTo(LoginView::class.java)
        }
    }
}
```

For app-wide guards (login, role checks), prefer a Vaadin service-level filter instead of duplicating
the check on every view — see the [Security guide]({{ '/security/' | relative_url }}).

## URL Path to Class Name Mapping

VoK does **not** derive a route path from the class name. The path comes from the explicit `@Route` value
on each component. Pick the path you want; there's no `MyFormView` → `my-form` magic to memorize.

## Testing navigation

Karibu-Testing's `MockVaadin.setup(Routes().autoDiscoverViews("your.package"))` discovers `@Route`-annotated
classes the same way Vaadin does at runtime. After setup, use `UI.getCurrent().navigate(...)` (or
`navigateTo`) in your test and then look up components with `_get<T>()` to assert what was rendered.
See `vok-example-crud/src/test/kotlin/.../AbstractAppTest.kt` for the canonical lifecycle.
