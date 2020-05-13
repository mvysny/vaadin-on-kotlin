[Index](index.html) | [Getting Started](gettingstarted-v10.html) | [Guides](vok-guides.html)

# DSLs: Explained

The Vaadin-on-Kotlin DSL allows you to define your UI in a hierarchical manner. It takes advantage of the DSL Kotlin language feature;
if you feel lost at any time please feel free to consult the official Kotlin documentation on
[Type-safe builders](https://kotlinlang.org/docs/reference/type-safe-builders.html).

> **Note**: Please feel free to skip this chapter if you're new to VoK and you're not yet looking for nitty-gritty technical
details on how things work under the hood.

Let's focus on the following code:
```kotlin
class MyView : VerticalLayout() {
    init {
        formLayout {
            textField("Name:")
            textField("Age:")
        }
    }
}
```

It is equivalent to the following code in a sense that it produces the same UI component hierarchy:
```kotlin
class MyView : VerticalLayout() {
    init {
        val fl = FormLayout()
        addComponent(fl)
        val nameField = TextField("Name:")
        fl.addComponent(nameField)
        val ageField = TextField("Age:")
        fl.addComponent(ageField)
    }
}
```

The produced hierarchy in both cases is as follows:
```
VerticalLayout
  \---- FormLayout
          |----- TextField
          \----- TextField
```

It's clear that the DSL code above has advantage over the plain flat code since it reflects the produced hierarchy.
Let's thus write the DSL function for constructing the `FormLayout` component. This `formLayout()` function must perform two tasks:

* Create a `FormLayout` component and insert it into the parent `VerticalLayout`;
* Provide a block which would make all functions, called from this block, insert components into the `FormLayout`.

## The First Task: Pick Proper Parent To Insert The Component Into

The first task can be achieved simply by using [functions with receivers](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver).
The receiver for the `formLayout()` function will denote the parent where the `FormLayout` will be inserted. It will be of the `VerticalLayout` type
(or rather `HasComponents` which is a supertype of all
layouts and component containers which would allow us to create form layouts in, say, `HorizontalLayout`). Kotlin will then auto-fill the closest `this` which matches
the receiver type. In this example the receiver will be the `MyView` class itself (since it extends from `VerticalLayout` which implements `HasComponents`).

An example of an (so-called extension) function with such receiver would be:
```kotlin
fun HasComponents.formLayout() {
    val fl = FormLayout()
    this.addChild(fl)  // "this" is HasComponents
}
fun HasComponents.textField() {
    val fl = TextField()
    this.addChild(fl)
}
```

> *Info*: Technically `HasComponents` doesn't have the `addChild()` method, but it's possible to implement such (extension) method
in a way which works with all Vaadin component containers. Let's skip this detail for now; you can always check out the sources of this
method in the Karibu-DSL project.

Kotlin will automatically pick the proper receiver:
* In the `init{}` block of the `MyView` the receiver would be the
`MyView` class itself (which extends `VerticalLayout`). Calling `formLayout()` in the `init{}` block will therefore
cause `FormLayout` to be added into `MyView`
* Exactly the same situation occurs within the function defined on `MyView`
* However, when the `textField()` function is called from `formLayout()`'s block, the nearest receiver is
the `FormLayout` itself, which takes precedence over `MyView`. Hence, the `TextField` will be nested inside of the
`FormLayout` as opposed of nesting inside of the `MyView`. Yet, we haven't defined such a block in the `formLayout()` yet! Let's fix that.

## The Second Task: Make Sure All Children Are Inserted Into Us

To implement the second task, we need the `formLayout()` function to be able to run a block. We need to declare the block
in a special way so that any DSL functions invoked from the block will add components into this `FormLayout`. Hence, we need
to make the block run with a *receiver* being the `FormLayout` itself. That would make Kotlin to run all nested DSL functions such as
`textField()` in the context of that receiver (`FormLayout` in this example). Since we have defined the `textField()` function to insert
the newly created `TextField` into the receiver, it will be inserted into the `FormLayout`.

We will therefore modify the `formLayout()` function accordingly:
```kotlin
fun HasComponents.formLayout(block: FormLayout.()->Unit) {
    val fl = FormLayout()
    this.addChild(fl)
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

`this.` is explicit and can be dropped. Also, when the `block` is the last parameter of a Kotlin function,
it goes after the parenthesis:

```kotlin
...
formLayout() {  // here the receiver is the newly constructed FormLayout
    textField()    // 'this' is the FormLayout and has been omitted
}
...
```

If a Kotlin function takes `block` as the only parameter, the empty parentheses can be omitted too:

```kotlin
...
formLayout {  // here the receiver is the newly constructed FormLayout
    textField()    // 'this' is the FormLayout and has been omitted
}
...
```

We have constructed functions in a way that allows us to write hierarchical code. Since Kotlin allows us to omit syntactic sugar
we can now define UIs in a way that is both concise and hierarchical.

## Specifying properties for the TextField

It is handy to make the `textField()` function also take a block, so that we can specify the properties
of the newly constructed `TextField` right next to the `TextField` creation itself. We will therefore add `block`
to the `textField()` function as well:

```kotlin
fun HasComponents.textField(block: TextField.()->Unit) {
    val fl = TextField(caption)
    this.addChild(fl)
    fl.block()
}
```

This will allow us to write the following code:
```kotlin
textField {
    caption = "Name"
    w = 30.em             // type-safe way of calling setWidth("30em")
    addStyleName("big")
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

The code compiles but it apparently makes no sense, since `TextField` is not a `HasComponents` and cannot take any children! Still,
the code compiles happily and it will actually add two text fields into the form layout. The problem here is
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

Yet writing `this.` to guard ourselves from this issue is highly annoying. Therefore we will use another technique:
the [DSL markers](https://kotlinlang.org/docs/reference/type-safe-builders.html#scope-control-dslmarker-since-11).
If we mark both `textField()`, `formLayout()` and `HasComponents` with a particular DSL Marker annotation (in our case, `@VaadinDsl` annotation), that would
prevent Kotlin from crossing to the outer receiver. However, we can't add annotation to the `HasComponents` interface since it's bundled in the Vaadin jar and
hence we can't modify its sources!

The solution is to add the `@VaadinDsl` annotation not to the `HasComponents` interface .java source, but into our DSL function definition sources. And
hence the DSL function becomes like follows:

```kotlin
fun (@VaadinDsl HasComponents).formLayout(block: (@VaadinDsl FormLayout).()->Unit) {
    val fl = FormLayout()
    this.addChild(fl)
    fl.block()
}
fun (@VaadinDsl HasComponents).textField(block: (@VaadinDsl TextField).()->Unit) {
    val fl = TextField(caption)
    this.addChild(fl)
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

A final touch would be to mark the `formLayout()` function itself with the `@VaadinDsl` annotation. It doesn't do anything on its own,
but it causes Intellij Kotlin plugin to highlight DSL functions with a different color. That makes them stand out in the code and be easy to spot.

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
