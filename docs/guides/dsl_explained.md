---
layout: default
title: DSLs Explained
permalink: /dsl_explained/
parent: Guides
nav_order: 5
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

# DSLs: Explained

The Vaadin-on-Kotlin DSL allows you to define your UI in a hierarchical manner.
It takes advantage of the DSL Kotlin language feature;
if you feel lost at any time please feel free to consult the official Kotlin
documentation on
[Type-safe builders](https://kotlinlang.org/docs/reference/type-safe-builders.html).

> **Note**: Please feel free to skip this chapter if you're new to
> VoK and you're not yet looking for nitty-gritty technical
> details on how things work under the hood.

Let's consider the following code, written in the DSL manner:
```kotlin
@Route("")
class MyView : VerticalLayout() {
    init {
        formLayout {
            textField("Name:") {
                description = "Last name, first name"
            }
            textField("Age:") {
                width = "5em"
            }
        }
    }
}
```

The same code, but written the old way:
```kotlin
@Route("")
class MyView : VerticalLayout() {
    init {
        val fl = FormLayout()
        add(fl)
        val nameField = TextField("Name:")
        nameField.description = "Last name, first name"
        fl.add(nameField)
        val ageField = TextField("Age:")
        ageField.width = "5em"
        fl.add(ageField)
    }
}
```

In both cases the produced hierarchy is as follows:
```
VerticalLayout
  \---- FormLayout
          |----- TextField
          \----- TextField
```

If we compare both approaches, the hierarchy of Vaadin components is
more clearly visible in the DSL approach, while the "old" code
looks "flat", more boilerplate-y and the intent is not conveyed as clearly.

## Creating DSL for Vaadin

Let's start from the very beginning: let's write the DSL function for
constructing the `FormLayout` component. This `formLayout()`
function must perform two tasks:

* Create a `FormLayout` component and insert it into the parent `VerticalLayout`;
* Provide a block which would make all nested DSL calls insert components into this `FormLayout`.

### Adding new component into parent layout

The first task can be achieved simply by using [extension functions](https://kotlinlang.org/docs/reference/extensions.html).
In our example, we want to insert the FormLayout into the `MyView` class.
If we would create the `formLayout()` function as an extension function, the
Kotlin compiler will automatically fill in the `MyView` instance as the receiver,
which can then be referenced from within the `formLayout()` function simply by using the "`this.`" expression: 

```kotlin
fun HasComponents.formLayout() {
    val fl = FormLayout()
    this.add(fl)  // when calling this function from MyView, "this" will reference the instance of MyView
}
fun HasComponents.textField(caption: String = "") {
    val tf = TextField(caption)
    this.add(tf)
}
```

What about the type of the receiver of the `formLayout()` function? We could make
the receiver to be of type `MyView`, or even better `VerticalLayout` since
`MyView` extends from `VerticalLayout`. However, that would still be quite limiting.
In order to make the function truly flexible and usable with all layouts and component containers,
it's best to use the `HasComponents` type.

Kotlin will automatically pick the proper receiver:
* In the `init{}` block of the `MyView` class, the receiver will be the
  `MyView` class itself (which extends `VerticalLayout`). Calling `formLayout()` in the `init{}` block will therefore
  cause `FormLayout` to be added into `MyView`.
* However, we wish to call the `textField()` function from `formLayout()`'s block in such a
  way that would insert the `TextField` into the `FormLayout` instead of into the `MyView`. In order to do that,
  we need the `formLayout()` function to run a specially designed closure.

### Designing the DSL closure

The `formLayout()` function must accept a closure as its parameter since we wish to write a code like this:

```kotlin
formLayout {
}
```

The closure needs to be declared
in a special way so that any DSL functions invoked from the closure itself will add components into this `FormLayout`.
The DSL functions insert components into whatever layout is referenced by `this`; therefore
we need a way to set the `this` reference in the closure to be the `FormLayout` itself.

That's precisely what [closures with receivers](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver) do.
We will therefore modify the `formLayout()` function accordingly:

```kotlin
fun HasComponents.formLayout(block: FormLayout.()->Unit) {
    val fl = FormLayout()
    this.add(fl)
    fl.block()
}
```

That will allow us to call the `textField()` function from `formLayout()`'s block as follows:
```kotlin
...
formLayout({  // here the receiver is the newly constructed FormLayout
    this.textField()    // 'this' is the FormLayout
})
...
```

The `this.` stanza can be dropped as usual. Also, when a closure is the last parameter of a Kotlin function,
it may go after the parenthesis:

```kotlin
...
formLayout() {  // here the receiver is the newly constructed FormLayout
    textField()    // 'this' is the FormLayout and has been omitted
}
...
```

If a Kotlin function takes a closure as the only parameter, the empty parentheses can be omitted:

```kotlin
...
formLayout {  // here the receiver is the newly constructed FormLayout
    textField()    // 'this' is the FormLayout and has been omitted
}
...
```

We now have constructed functions in a way that allows us to write hierarchical code.
Since Kotlin allows us to omit syntactic sugar
we can now define UIs in a way that is both concise and hierarchical.

## Specifying properties for the TextField

It is handy to specify all properties for the newly created TextField at one place, for example:

```kotlin
textField {
    label = "Foo"
    width = "150px"
    element.classList.add("big")
}
```

We can achieve that by having the `textField()` function also take a closure with receiver:

```kotlin
fun HasComponents.textField(caption: String = "", block: TextField.()->Unit) {
    val fl = TextField(caption)
    this.add(fl) // this. is for brevity and can be omitted
    fl.block()
}
```

However, this will introduce an intriguing problem: now we are able to write the following code:
```kotlin
formLayout {
    textField {
        textField()
    }
}
```

The code compiles but it apparently makes no sense, since `TextField` is not a `HasComponents`
and cannot take any children! Yet the code compiles happily and it will actually add
two text fields into the form layout. The problem here is
that Kotlin will look up the nearest `HasComponents` as the receiver for the `textField()`
function; since `TextField` is not `HasComponents` Kotlin will hop level up and will take the `FormLayout`.

Note that if we rewrite the code as follows, it no longer compiles:
```kotlin
formLayout {
    textField {
        this.textField()  // doesn't compile since 'this' is TextField and the textField() function only works on HasComponents
    }
}
```

Yet writing `this.` every time to protect ourselves from this kind of issue is highly annoying. Therefore we will use another technique:
the [DSL markers](https://kotlinlang.org/docs/reference/type-safe-builders.html#scope-control-dslmarker-since-11).
If we mark both `textField()`, `formLayout()` and `HasComponents` with a particular DSL Marker annotation (in our case, `@VaadinDsl` annotation), that would
prevent Kotlin from crossing to the outer receiver. However, we can't add annotation to the `HasComponents` interface since it's bundled in the Vaadin jar and
hence we can't modify its sources!

The solution is to add the `@VaadinDsl` annotation not to the `HasComponents` interface .java source file,
but into our DSL function definition sources. And
hence the DSL function becomes like follows:

```kotlin
fun (@VaadinDsl HasComponents).formLayout(block: (@VaadinDsl FormLayout).()->Unit) {
    val fl = FormLayout()
    this.add(fl)
    fl.block()
}
fun (@VaadinDsl HasComponents).textField(caption: String = "", block: (@VaadinDsl TextField).()->Unit) {
    val fl = TextField(caption)
    this.add(fl)
    fl.block()
}
```

And now the confusing code doesn't compile anymore:
```kotlin
formLayout {
    textField {
        textField()   // compilation error: a member of outer receiver
    }
}
```

A final touch would be to mark the `formLayout()` function itself with the `@VaadinDsl` annotation.
Such placed annotation doesn't do anything on its own,
but it causes Intellij Kotlin plugin to highlight DSL functions with a different color.
That makes them stand out in the code and be easy to spot.

## DSLs in VoK

The above-mentioned DSL approach is employed in VoK to define the UIs. The DSL function handles the actual creation of the component; then it
passes the created component to the `init()` method which then adds the component into the parent layout.

If you need to only create the component, without adding it to the parent just yet, you can not use DSLs -
just construct the component directly, using the component's constructor. You can then use `.apply{}` to use the DSL to define
the contents if need be:

```kotlin
val form = FormLayout().apply {
  textField("Name:")
  checkBox("Employed")
}
```

DSLs do not contain the functionality needed to *remove* the component from its parent. If you need this kind of functionality, you will
have to resort to Vaadin's built-in methods, or use Karibu-DSL's `removeFromParent()` function.