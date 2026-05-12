---
layout: default
title: Forms
permalink: /forms/
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

# Creating Forms

A form does two jobs:

* it **lays out** input components — `TextField`, `DatePicker`, `ComboBox`, etc. — so the
  result is pleasant to look at and easy to fill in;
* it **moves data** between those components and a backing entity: read values out, validate
  what the user typed, write values back, save.

Vaadin's [`Binder`](https://vaadin.com/docs/latest/flow/binding-data/components-binder) handles
the second half. The first half is plain Vaadin layouts plus the karibu-dsl builders covered in
[Creating UIs](creating_ui.md). The rest of this guide stitches the two together.

> For a complete worked example, read **Chapter 6 — Editing the selected product** of the
> [Tutorial](/tutorial). This guide is the reference; the tutorial is the walkthrough.

## Positioning form components

`FormLayout` is the default container for forms. It arranges fields in a CSS-grid-like flow
and reduces columns automatically as the viewport narrows. Drop your fields inside the
`formLayout { ... }` builder:

```kotlin
import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.formlayout.FormLayout

class PersonForm : FormLayout() {
    init {
        textField("Name") { focus() }
        textField("Age")
        datePicker("Date of birth")
        comboBox<MaritalStatus>("Marital status") {
            setItems(MaritalStatus.entries)
        }
        checkBox("Alive")
    }
}
```

A few notes on the field names karibu-dsl exposes:

- **`datePicker`**, not `dateField` — the Vaadin component is `DatePicker`.
- **`checkBox`** (camelCase) — the Vaadin component is `Checkbox`.
- **`comboBox<T>`** carries the item type as a reified generic; you still call
  `setItems(...)` to fill it. Use `Enum.entries` (Kotlin 1.9+) instead of `values()`.

If you want responsive breakpoints (e.g. one column on mobile, two when there's room),
karibu-dsl gives you a condensed DSL for [`FormLayout.ResponsiveStep`](https://vaadin.com/docs/latest/components/form-layout):

```kotlin
formLayout {
    responsiveSteps { "0px"(1, top); "30em"(2, aside) }

    textField("Name")
    textField("Age")
}
```

`"0px"(1, top)` reads as *"from 0px upward, use 1 column with labels on top"*; the second
step kicks in at 30em (≈480px) and switches to 2 columns with labels beside their fields.

For more elaborate forms, nest layouts freely — `FormLayout` for the field grid,
`horizontalLayout` for the button bar, `verticalLayout` to stack sections. There is no
penalty for nesting; the resulting DOM is plain flexbox / grid.

## Binding values

`Binder<E>` is the bridge between the form's fields and a backing entity. Three responsibilities
sit on it:

1. **Read** values out of an entity into the form fields.
2. **Validate** input on its way back.
3. **Write** the result into the entity.

We're going to use this `Person` entity throughout — a ktorm `Entity<E>` interface, mirroring
the one in `vok-example-crud`:

```kotlin
import com.github.mvysny.ktormvaadin.ActiveEntity
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import java.time.LocalDate

interface Person : ActiveEntity<Person> {
    var id: Long?
    var name: String?
    var age: Int?
    var dateOfBirth: LocalDate?
    var maritalStatus: MaritalStatus?
    var alive: Boolean?

    override val table: Table<Person> get() = Persons
    companion object : Entity.Factory<Person>()
}

enum class MaritalStatus { Single, Married, Divorced, Widowed }
```

For why entities are interfaces, and why every property is nullable, see the
[Databases guide](databases.md). The short answer: ktorm builds the implementation at
runtime, and a half-filled `Entity.create<Person>()` has to be representable in the type system.

Create a `Binder<Person>` as a field of the form and bind each input to a property of `Person`:

```kotlin
import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.data.binder.Binder

class PersonForm : FormLayout() {
    val binder = Binder<Person>(Person::class.java)
    init {
        textField("Name") {
            focus()
            bind(binder).trimmingConverter().bind(Person::name)
        }
        textField("Age") {
            bind(binder).toInt().bind(Person::age)
        }
        datePicker("Date of birth") {
            bind(binder).bind(Person::dateOfBirth)
        }
        comboBox<MaritalStatus>("Marital status") {
            setItems(MaritalStatus.entries)
            bind(binder).bind(Person::maritalStatus)
        }
        checkBox("Alive") {
            bind(binder).bind(Person::alive)
        }
    }
}
```

The interesting line is `bind(binder).bind(Person::name)`. Reading right-to-left:

* `bind(binder)` (a karibu-dsl extension on any `HasValue` field) starts a fluent
  binding builder bound to `binder`.
* `.bind(Person::name)` finalises the binding by naming the target property. The
  `KMutableProperty1` reference is type-checked at compile time — if you rename `name`
  on the entity, the binding stops compiling.

Two helpers from karibu-dsl's `BinderUtils` smooth common conversions:

* `.trimmingConverter()` — strips whitespace around the field value before storing it.
* `.toInt()` / `.toLong()` / `.toBigDecimal()` / `.toDouble()` — adapt a `TextField`'s
  `String?` value to a numeric property. For numeric forms, prefer Vaadin's typed inputs
  (`integerField`, `bigDecimalField`, `numberField`) which expose the right type natively
  and need no converter.

> **Warning.** Bean-validation annotations (next section) are picked up *only* when the
> binding names a property — i.e. you used `.bind(KProperty)` or `.bind("propertyName")`.
> The lambda overload `.bind(getter, setter)` is invisible to the validator scanner;
> annotations on the underlying property are silently ignored.

### Populating the form

Two modes, pick one:

**`readBean(person)` + `writeBeanIfValid(person)`** (recommended for explicit-save forms).
Read copies values into the fields; edits stay in the fields and are only pushed back when
you call `writeBeanIfValid`. Switching to a different bean is safe — pending edits are
discarded.

```kotlin
form.binder.readBean(person)        // populate fields
// user edits the form…
if (form.binder.writeBeanIfValid(person)) {
    person.save()
}
```

**`setBean(person)`** wires the form bidirectionally — every keystroke flows straight back
into the bean. Useful when there is no Save button (auto-save) or you want live previews;
risky when the user might cancel.

`writeBeanIfValid` returns `false` if any binding fails validation, leaves the bean
untouched, and asks Vaadin to render per-field errors (red border + helper text). You
don't need to set any "component error" property yourself — that pattern (`UserError`,
`Component.componentError`) was a Vaadin 8 API and is gone from Flow.

## Validating input

Two kinds of validation, used together:

* **Bean Validation** (Jakarta Bean Validation, formerly JSR-303) — annotations on the
  entity. The same checks run wherever the bean is, including the database layer.
* **Binder validators** — added in the field-builder. Run inside Vaadin only, useful
  for UI-only constraints like "must match the password field above".

### Bean Validation on the entity

Annotate the **getters** of your ktorm entity. The `@get:` site target is essential —
ktorm entities are interfaces, so `@field:` has no field to land on:

```kotlin
import com.github.mvysny.ktormvaadin.ActiveEntity
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.ktorm.entity.Entity
import org.ktorm.schema.Table

interface Person : ActiveEntity<Person> {
    var id: Long?

    @get:NotNull
    @get:Size(min = 1, max = 200)
    var name: String?

    @get:NotNull
    @get:Min(15)
    @get:Max(100)
    var age: Int?

    // ...

    override val table: Table<Person> get() = Persons
    companion object : Entity.Factory<Person>()
}
```

`ActiveEntity` exposes `validate()` to run the annotations from anywhere:

```kotlin
person.validate()    // throws ConstraintViolationException on the first violation
```

If you prefer the raw Jakarta API:

```kotlin
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation

val violations: Set<ConstraintViolation<Person>> =
    Validation.buildDefaultValidatorFactory().validator.validate(person)
if (violations.isNotEmpty()) throw ConstraintViolationException(violations)
```

To make the binder run these annotations, swap `Binder<Person>(...)` for karibu-dsl's
`beanValidationBinder<Person>()`:

```kotlin
class PersonForm : FormLayout() {
    val binder = beanValidationBinder<Person>()
    // bindings as before
}
```

That's a `BeanValidationBinder<Person>` under the hood. With it, `binder.validate()` and
`writeBeanIfValid` consult the annotations alongside any explicit per-field validators.

### Binder validators (UI-only)

For checks that don't belong on the entity, attach them to the binding. karibu-dsl ships
a handful of common ones in `BinderUtils`:

```kotlin
textField("Email") {
    bind(binder).validEmail().bind(Person::email)
}
textField("Display name") {
    bind(binder).validateNotBlank().bind(Person::displayName)
}
integerField("Age") {
    bind(binder).validateInRange(15..100).bind(Person::age)
}
emailField("Email") {
    bind(binder).asRequiredNotNull("Email is required").bind(Person::email)
}
```

For anything custom, drop down to Vaadin's `withValidator { value, _ -> ... }`. See the
[Binder docs](https://vaadin.com/docs/latest/flow/binding-data/components-binder-validation)
for the full API.

## Saving the bean

The canonical Save handler from `vok-example-crud`:

```kotlin
import com.vaadin.flow.component.notification.Notification

private fun onSave() {
    if (!form.binder.validate().isOk || !form.binder.writeBeanIfValid(person)) {
        // Binder has already rendered field-level errors; nothing else to do.
        return
    }
    person.save()
    Notification.show("Saved ${person.name}")
}
```

`binder.validate()` marks every invalid field with a red border and a helper-text error
message, then returns a `BinderValidationStatus` whose `isOk` flag tells you whether to
proceed. `writeBeanIfValid` re-runs the validation and writes only on success — calling
both is belt-and-braces, but it's clear about intent and costs nothing.

`person.save()` is `ActiveEntity`'s wrapper around ktorm's `flushChanges()` / insert; it
picks the right SQL based on whether `id` is set. See the [Databases guide](databases.md)
for the persistence side.

## Reusing the form

A form usually wants to be embedded in more than one place — a side panel for editing, a
`Dialog` for adding new rows. Extract it as a class with a DSL builder:

```kotlin
import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.formlayout.FormLayout

class PersonForm : FormLayout() {
    val binder = beanValidationBinder<Person>()
    init {
        textField("Name") { bind(binder).trimmingConverter().bind(Person::name) }
        integerField("Age") { bind(binder).bind(Person::age) }
        datePicker("Date of birth") { bind(binder).bind(Person::dateOfBirth) }
        comboBox<MaritalStatus>("Marital status") {
            setItems(MaritalStatus.entries)
            bind(binder).bind(Person::maritalStatus)
        }
        checkBox("Alive") { bind(binder).bind(Person::alive) }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).personForm(
    block: (@VaadinDsl PersonForm).() -> Unit = {},
): PersonForm = init(PersonForm(), block)
```

Now the form drops into any view as a single line:

```kotlin
class EditPersonView : KComposite() {
    private lateinit var form: PersonForm
    private val root = ui {
        verticalLayout {
            form = personForm()
            button("Save") {
                setPrimary()
                onClick {
                    if (form.binder.writeBeanIfValid(person)) person.save()
                }
            }
        }
    }
}
```

For a fuller worked example — including a `Dialog`-based add flow that reuses the same
`PersonForm` — see `vok-example-crud/.../CreateEditPerson.kt`.

### `KComposite` vs extending `FormLayout`

Extending `FormLayout` directly (as above) exposes its full API on every `PersonForm`
instance — callers can call `add(...)`, `setResponsiveSteps(...)`, and so on. That's fine
for small internal forms but leaks implementation. For widely-reused forms, extend
`KComposite` instead:

```kotlin
import com.github.mvysny.karibudsl.v10.KComposite

class PersonForm : KComposite() {
    val binder = beanValidationBinder<Person>()
    private val root = ui {
        formLayout {
            textField("Name") { bind(binder).trimmingConverter().bind(Person::name) }
            // ...
        }
    }
}
```

The public API shrinks to whatever you choose to expose (here, `binder`). You can change
the root layout later — `verticalLayout` instead of `formLayout`, say — without breaking
callers. See [Creating UIs — The `KComposite` pattern](creating_ui.md#the-kcomposite-pattern)
for the trade-offs.

## More resources

- [Binding data to forms](https://vaadin.com/docs/latest/flow/binding-data/components-binder) —
  the canonical Vaadin reference for `Binder`.
- [Tutorial — Chapter 6](/tutorial) — builds a master-detail editor end to end.
- [Creating UIs](creating_ui.md) — the component and layout basics this guide builds on.
- [Databases guide](databases.md) — entity definitions, `ActiveEntity`, persistence.
