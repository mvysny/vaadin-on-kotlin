[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v8/framework/introduction/intro-overview.html) to deliver the UI.
Vaadin lets you forget the web and develop user interfaces much like you would develop a desktop application with conventional Java toolkits such as AWT, Swing, or SWT.
You add components such as Button and TextField into the page, nesting them in layouts which then position the components.

## Introduction

The web is composed of HTML pages. The basic building block of a HTML page is the *HTML element*, such as `<div>`.
Typical web frameworks requires you to build the page out of HTML elements.
In this regard, Vaadin is different.

Instead of composing HTML elements, in Vaadin we compose *components*, such as `Button`, `ComboBox` and `VerticalLayout`.
Every Vaadin component consists of two parts:

* The client-side part renders one or more HTML elements and controls them by the means of JavaScript. For example a Google Map
  component would fetch individual tiles and produce a mesh of `<div>`s which then lay out the tiles to show the map itself.
* The server-side part then exposes a high-level API. The Google Map component example would allow you to set zoom, to focus on particular
  GPS coordinates, to add markers etc.

> *Info*: The client-side is typically written in Java and compiled to the JavaScript by the means of GWT.
It is however also possible to write components directly by using JavaScript. There are great resources on how to write
Vaadin client-side component, for example the [Client-side Development Guide](https://vaadin.com/docs/v8/framework/clientside/clientside-overview.html).
In this guide we will not focus on the client-side part; instead we will focus on how to compose the server-side components.

The components are typically rich in functionality. For example `ComboBox` does not render into the HTML `<input>` element but it instead renders
a rich `<div>` hierarchy which allows features not possible with the `<input>` element such as auto-completion.
There is a big palette of pre-made components which we compose and nest in the server-side Java code. Vaadin
then makes sure to call the client-side of every component, to render the proper HTML elements. The rendering
process is typically self-contained, implemented in the component client-side code and typically can not be controlled from server-side Java.

> **Note:** Vaadin 8 components are not to be
confused with the [Web Components Standard](https://en.wikipedia.org/wiki/Web_Components) which are used by Vaadin 10.
Vaadin 8 uses GWT/JavaScript to implement the components and doesn't require the Web Component Standard to be supported
by the browser. Vaadin 8 components can be thought of as a predecessors of the Web Component standard.

For example, a typical Vaadin form uses the `FormLayout` component and adds a couple of `CheckBox`, `TextField` and
`ComboBox` components. The code on server-side would look like this:

```java
FormLayout layout = new FormLayout("New Employee Form");
layout.addComponent(new TextField("Name:"));
layout.addComponent(new CheckBox("Internal employee"));
layout.addComponent(new DatePicker("Date of birth:"));
```

This code builds a component *hierarchy* (a tree of components, in this case fields nested in a form layout). The components'
client-side code then renders themselves as HTML elements.

With VoK, we create UIs by creating component hierarchies. We will now show how that is done in VoK in a minute.

> The following text doesn't expect you to be familiar with the Vaadin framework. However, it is best to have at least basic understanding of the Kotlin
programming language. If you feel lost in the following text, please take your time to learn of the Kotlin language features first.

## Creating Component Hierarchies

Please git-clone the [VoK Hello World App](https://github.com/mvysny/vok-helloworld-app) - we're going to experiment on that app. You can
do that by running this in your terminal:

```bash
git clone https://github.com/mvysny/vok-helloworld-app
```

If you open the [WelcomeView.kt](https://github.com/mvysny/vok-helloworld-app/blob/master/web/src/main/kotlin/com/example/vok/WelcomeView.kt)
file, you'll notice that it extends from the `VerticalLayout`. In Vaadin, `VerticalLayout` and `HorizontalLayout` are two most commonly
used layouts. By extending the `VerticalLayout` we state that the root layout of this view is going to be vertical.

Let's now replace the contents of the `WelcomeView` by a single button. Rewrite the `WelcomeView` class as follows:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    init {
        button("Click me") {
            onLeftClick {
                Notification.show("Clicked")
            }
        }
    }
}
```

The `button()` function simply creates the Vaadin `Button`, sets a caption into it, inserts it into the parent layout (in this case,
the root `VerticalLayout`/`WelcomeView`) and runs the configuration block for that button. The configuration block adds a left click
listener. When you ctrl+click the `button()` function, the definition of the function looks very cryptic:

```kotlin
fun (@VaadinDsl HasComponents).button(caption: String? = null, block: (@VaadinDsl Button).() -> Unit = {})
        = init(Button(caption), block)
```

This is a Kotlin function definition which allows us to build UIs in a structured way, by employing so-called DSLs. Don't worry if
this doesn't make any sense right now - we will explain this in a great detail later on.

> *Info*: A technique called DSL (domain-specific language) is used in the Kotlin language to construct
hierarchical structures. Since the UI is a hierarchical structure with components nested inside layouts, we can
use the DSL approach here. In VoK we have constructed a set of functions which will allow you to construct Vaadin UIs
in a hierarchical manner. Please read the [Using DSL to write structured UI code](http://mavi.logdown.com/posts/7073786)
article on why a hierarchical code beats plain Java code.

Let us add a simple form, consisting of two text fields:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    init {
        formLayout {
            textField("Name:")
            textField("Age:")
        }
        button("Click me") {
            onLeftClick {
                Notification.show("Clicked")
            }
        }
    }
}
```

The `formLayout()` function creates Vaadin `FormLayout` component and adds it into the root `VerticalLayout`. Then it runs the configuration
block, acting as a parent layout in that block. This is very important since that will correctly allow the `textField()` function to insert
the newly created `TextField` class into the `FormLayout` itself, and not into the root `VerticalLayout`.

## Referencing Components

The `textField()` function also returns the newly created `TextField`. This is handy if we want to reference those text fields later, for
example from the button click handler:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    private lateinit var nameField: TextField
    private lateinit var ageField: TextField
    init {
        formLayout {
            nameField = textField("Name:")
            ageField = textField("Age:")
        }
        button("Click me") {
            onLeftClick {
                Notification.show("Hello, ${nameField.value} of age ${ageField.value}")
            }
        }
    }
}
```

## DSLs Explained

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
* Provide a block which would make all functions called from this block insert components into the `FormLayout`.

The first item can be achieved simply by using [functions with receivers](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver).
Here, the receiver would simply be the parent `VerticalLayout` (or rather `HasComponents` which is a supertype of all
layouts and component containers which would allow us to create form layouts in, say, `HorizontalLayout`).

An example of such function would be:
```kotlin
fun HasComponents.formLayout() {
    val fl = FormLayout()
    this.addChild(fl)
}
fun HasComponents.textField() {
    val fl = TextField()
    this.addChild(fl)
}
```

> *Info*: Technically `HasComponents` doesn't have the `addChild()` method, but it's possible to implement such method
in a way which works with all Vaadin component containers. Let's skip this detail for now.

Kotlin will automatically pick the proper receiver:
* In the `init{}` block of the `MyView` the receiver would be the
`MyView` itself (which extends `VerticalLayout`). Calling `formLayout()` in the `init{}` block will therefore
cause `FormLayout` to be added into `MyView`
* Exactly the same situation occurs within the function defined on `MyView`
* However, when the `textField()` function is called from `formLayout()`'s block, the nearest receiver is
the `FormLayout` itself, which takes precedence over `MyView`. Hence, the `TextField` will be nested inside of the
`FormLayout` as opposed of nesting inside of the `MyView`. Yet, we haven't defined such a block in the `formLayout()` yet! Let's fix that.

To satisfy the second item, we need the `formLayout()` function to be able to run a block. We need to declare the block
in a special way so that any DSL functions invoked from the block will add components into this `FormLayout`. Hence, we need
to provide a *receiver* to the block which is then picked by the DSL functions such as `textField()`.

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

### Specifying properties for the TextField

It is handy to make the `textField()` function also take a block, so that we can specify the properties
of the newly constructed `TextField` right next to the `TextField` creation itself. We will therefore add `block`
to the `textField()` function as well:

```kotlin
fun HasComponents.textField(block: TextField.()->Unit) {
    val fl = TextField(caption)
    this.add(fl)
    fl.block()
}
```

This will allow us to write the following code:
```kotlin
textField {
    caption = "Name"
    width = "30em"
    style = "big"
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

The code compiles but it apparently makes no sense, since `TextField` is not a `HasComponents` and cannot take any children! Yet
it still compiles happily and it will actually add two text fields into the form layout. The problem here is
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
If we mark both `textField()`, `formLayout()` and `HasComponents` with a single DSL Marker annotation, that would
prevent Kotlin from crossing to the outer receiver. However, we can't add annotation to `HasComponents` since it's built-in
in Vaadin!

The solution is to annotate `HasComponents` not in its definition place, but in the DSL function definition. And
hence the DSL function becomes like follows:

```kotlin
fun (@VaadinDsl HasComponents).formLayout(block: (@VaadinDsl FormLayout).()->Unit) {
    val fl = FormLayout()
    this.add(fl)
    fl.block()
}
fun (@VaadinDsl HasComponents).textField(block: (@VaadinDsl TextField).()->Unit) {
    val fl = TextField(caption)
    this.add(fl)
    fl.block()
}
```

And now the following code doesn't compile anymore:
```kotlin
formLayout {
    textField {
        textField()   // compilation error: a member of outer receiver
    }
}
```

A final touch would be to mark the `formLayout()` function itself with the `@VaadinDsl` annotation. It doesn't do anything on its own,
but it causes Intellij Kotlin plugin to highlight DSL functions with a different color. That makes them stand out in the code and be easy to spot.

## Creating reusable components

The core principle of Vaadin is that it is very easy to create reusable components. For example, in order to create a reusable form,
all you need to do is to define a class:

```kotlin
class NameAgeForm : FormLayout() {
    private val nameField = textField("Name:")
    private val ageField = textField("Age:")
    val greeting: String get() = "Hello, ${nameField.value} of age ${ageField.value}"
}
```

This class is a form layout with two text fields nested inside of it. However, we can't use it directly in the DSL fashion yet -
the integration function is missing:

```kotlin
fun HasComponents.nameAgeForm(block: NameAgeForm.()->Unit = {}): NameAgeForm = init(NameAgeForm(), block)
```

The function instantiates the form and calls the `init()` method which will add the newly created form into the parent layout and then
it will call the configuration `block` on it. Now we can rewrite the `WelcomeView` as follows:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    init {
        val form = nameAgeForm()
        button("Click me") {
            onLeftClick {
                Notification.show(form.greeting)
            }
        }
    }
}
```

## More resources

To learn Vaadin:

* [Official Vaadin website](https://www.vaadin.com)
* [Vaadin Documentation](https://vaadin.com/docs/v8) - we recommend to download and read the Vaadin Book PDF.
