[Index](index.html) | [Getting Started](gettingstarted-v10.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v8/framework/introduction/intro-overview.html) to deliver the UI.
Vaadin lets you forget the web and develop user interfaces much like you would develop a desktop application with conventional Java toolkits such as AWT, Swing, or SWT.
You add components such as Button and TextField into the page, nesting them in layouts which then position the components.

## Introduction

The web is composed of HTML pages. The basic building block of a HTML page is the *HTML element*, such as `<div>` or `<a>`.
Typical web frameworks require you to use HTML elements to build the pages. In this regard, Vaadin is different.

Instead of composing HTML elements, in Vaadin we compose *components*, such as `Button`, `ComboBox` and `VerticalLayout`.
Every Vaadin component consists of two parts:

* The client-side part of the component renders as one or more HTML elements and controls them by the means of JavaScript. For example a Google Map
  component would fetch individual tiles and produce a mesh of `<div>`s which then lay out the tiles to show the map itself.
* The server-side part of the component then exposes a high-level API. The Google Map component example would allow you to set zoom, to focus on particular
  GPS coordinates, to add markers etc. Vaadin then provides the connector API which transmits
  events and component state from client-side to server-side and back again.

This kind of approach makes it incredibly easy to add Google Maps to your site. You just use the components' server-side Java API and you don't
have to care about what the HTML will look like, or how exactly it will fetch the data. The code looks like follows:

```java
class MyView : VerticalLayout() {
  init {
    setSizeFull()
    val maps = GoogleMaps()
    maps.setSizeFull()
    addComponent(maps)
  }
}
```

> *Info*: The client-side is typically written in Java and compiled to the JavaScript by the means of GWT.
It is however also possible to write components directly by using JavaScript. There are great resources on how to write
Vaadin client-side component, for example the [Client-side Development Guide](https://vaadin.com/docs/v8/framework/clientside/clientside-overview.html).
In this guide we will not focus on the client-side part; instead we will focus on how to compose the server-side components.

The components are typically rich in functionality. For example `ComboBox` does not render into the HTML `<input>` element but it instead renders
a rich `<div>` hierarchy which allows for features which are not possible with the `<input>` element, such as auto-completion.
There is a big palette of pre-made components, and we use server-side Java code to create them and add them into layouts. Vaadin
then makes sure to create the client-side GWT widget for every component and inserts it into the HTML into appropriate location. The rendering
process is typically self-contained, implemented in the component client-side code and typically can not be controlled directly from server-side Java.

For example, a typical Vaadin form uses the `FormLayout` component and adds a couple of `CheckBox`, `TextField` and
`ComboBox` components. The code on server-side would look like this:

```java
FormLayout layout = new FormLayout("New Employee Form");
TextField nameField = new TextField("Name:");
nameField.setValue("Donald Knuth");
layout.addComponent(nameField);
layout.addComponent(new CheckBox("Internal employee"));
layout.addComponent(new DatePicker("Date of birth:"));
```

This code builds a component *hierarchy* (a tree of components, in this case fields nested in a form layout). The components'
client-side code then renders itself as HTML elements.

With VoK, we create component hierarchies by employing the DSL Kotlin language feature. We will now show how that is done in VoK in a minute.

> **Note:** Vaadin 8 components are not to be
confused with the [Web Components Standard](https://en.wikipedia.org/wiki/Web_Components) which are used by Vaadin 14.
Vaadin 8's Components are in fact GWT widgets which are in essence just plain HTML elements controlled by JavaScript. GWT widgets
do not require the Web Component Standard to be supported
by the browser, however they also don't benefit from e.g. Shadow DOM isolation.
Vaadin 8 components can be thought of as a "predecessors" of the Web Component standard.

The following text doesn't expect you to be familiar with the Vaadin framework. However, it is best to have at least basic understanding of the Kotlin
programming language. If you feel lost in the following text, please take your time to learn of the Kotlin language features first.

## Available Components

Lots of components are already built-in. You can use e.g. [Vaadin Sampler](https://demo.vaadin.com/sampler/) to check out the built-in components;
you can also visit [Vaadin 8 Documentation on Components](https://vaadin.com/docs/v8/framework/components/components-overview.html) to read about all of the available
components.

You can find additional components at the [Vaadin Directory](https://vaadin.com/directory); just make sure to search for components intended for Vaadin 8 since
Vaadin 14 components won't work with Vaadin 8.

It is also possible to integrate a standalone GWT component into Vaadin; you can find out more at the [Integrating an existing GWT widget](https://vaadin.com/docs/v8/framework/articles/IntegratingAnExistingGWTWidget.html)
page.

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
listener. You can ctrl+click to see the sources of the `button()` function; the definition of the function looks very cryptic:

```kotlin
@VaadinDsl
fun (@VaadinDsl HasComponents).button(caption: String? = null, block: (@VaadinDsl Button).() -> Unit = {})
        = init(Button(caption), block)
```

This is a Kotlin function definition which allows us to build UIs in a structured way, by employing so-called DSLs. Don't worry if
this doesn't make any sense right now - we will explain this in a great detail later on.
What the function does is that it creates a button, adds it to the parent layout and allows the button to be configured further.

> *Info*: A technique called DSL (domain-specific language) is used to construct hierarchical structures using
just the Kotlin language features. Since the UI is a hierarchical structure with components nested inside layouts,
the DSL approach is applicable. In VoK we have constructed a set of functions which will allow you to construct Vaadin UIs
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
block, acting as a parent layout in that block. This is very important since that will make the `textField()` function to correctly insert
the newly created `TextField` class into the `FormLayout` itself, and not into the root `VerticalLayout`.
This "magic" is actually just a feature of Kotlin. Kotlin simply passes current layout as a [receiver](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver)
to the `textField()` function. But more on that later.

> **Important**: the notation is that the DSL function for a component starts with a lower-case letter. For example,
the function which creates `FormLayout` is called `formLayout()`, the same goes for `label()`, `verticalLayout()` and all
other built-in Vaadin components. To add DSL function for add-ons and custom components, please read below.

## Layouts

Since the `VerticalLayout`, `HorizontalLayout` and `FormLayout` are the most used components, let's start with them. There is a great writeup
about the features at the Vaadin page: [VerticalLayout and HorizontalLayout](https://vaadin.com/docs/v8/framework/layout/layout-orderedlayout.html) -
please make sure to read that page first, before going further with this documentation. The most important bits are:

* The Layout positions "slots"; the children components are then positioned (aligned) inside of those slots.
* The Layout uses two different algorithms to position+size slots: the first one when the Layout's width is set to undefined (=wrap contents),
  the second one when the Layout's width is set to a concrete value (either absolute or percentage, =fill parent).

Now that you understand the concepts, let us list some examples. The first example is a classical example of doing
a perfect centering of a component inside of its parent. It was impossible to do with CSS prior
the flexbox, yet Vaadin's layouts use JavaScript to position children and thus follow their own logic. Centering of a child is thus a simple
case of setting the proper alignment to the child:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    init {
        setSizeFull()
        verticalLayout {
            w = 200.px; h = 100.px; isMargin = false
            button("centered") {
                alignment = Alignment.MIDDLE_CENTER
            }
        }
    }
}
```

The result is as follows:

![Button Centered](images/creating_ui/button_centered.png)

Another case would be a button bar, having a bunch of buttons both on the left side and on the right side.
There is a `CSSLayout` acting as an spacer; since it's expanded it consumes all of the available space,
pushing follow-up buttons to the right:

```kotlin
@AutoView("")
class WelcomeView: VerticalLayout(), View {
    init {
        setSizeFull()
        horizontalLayout {
            w = 300.px; isSpacing = false
            button { icon = VaadinIcons.TRASH; styleName = ValoTheme.BUTTON_BORDERLESS }
            button { icon = VaadinIcons.ADD_DOCK; styleName = ValoTheme.BUTTON_BORDERLESS }
            cssLayout { isExpanded = true }
            button { icon = VaadinIcons.QUESTION; styleName = ValoTheme.BUTTON_BORDERLESS }
        }
    }
}
```

![Button Bar Example](images/creating_ui/button_bar.png)

The same effect can be achieved even without the spacer component. In the following example, the "question" button
will act as the spacer; it will be positioned to the right in its slot to appear as right-centered:

```kotlin
setSizeFull()
horizontalLayout {
    w = 300.px; isSpacing = false
    button { icon = VaadinIcons.TRASH; styleName = ValoTheme.BUTTON_BORDERLESS }
    button { icon = VaadinIcons.ADD_DOCK; styleName = ValoTheme.BUTTON_BORDERLESS }
    button { isExpanded = true; icon = VaadinIcons.QUESTION; styleName = ValoTheme.BUTTON_BORDERLESS; alignment = Alignment.MIDDLE_RIGHT }
    button { icon = VaadinIcons.AIRPLANE; styleName = ValoTheme.BUTTON_BORDERLESS }
}
```

> *Info*: you can use the [Vaadin Icons](https://pro.vaadin.com/icons) page to search for available Vaadin icons, or you can simply use IDE's
autocompletion feature for available `VaadinIcons` enum constants.

You can use the same approach for building the application frame. We're going to have the main menu to the right,
and expand the left area so that the views can be nested inside of it:

```kotlin
@PushStateNavigation
class MyUI : UI(), ViewDisplay {
    private lateinit var viewContainer: CssLayout
    override fun init(request: VaadinRequest) {
        navigator = Navigator(this, this as ViewDisplay)
        navigator.addProvider(autoViewProvider)
        horizontalLayout {
            setSizeFull()
            verticalLayout {
                setSizeUndefined(); isSpacing = false; isMargin = false
                button("About") { icon = VaadinIcons.QUESTION; styleName = ValoTheme.BUTTON_BORDERLESS }
                button("Users") { icon = VaadinIcons.USERS; styleName = ValoTheme.BUTTON_BORDERLESS }
                button("Log Out") { icon = VaadinIcons.USERS; styleName = ValoTheme.BUTTON_BORDERLESS }
            }
            viewContainer = cssLayout {
                setSizeFull(); isExpanded = true
            }
        }
    }

    override fun showView(view: View) {
        viewContainer.removeAllComponents()
        viewContainer.addComponent(view as Component)
    }
}
```

![Main Application Frame](images/creating_ui/appframe.png)

The layouts in Vaadin 8 are very powerful, you can use them in a wide range of use cases. In case you do not
understand what the layout does, always inspect the vertical layout and its slots in your browser's developer tools
(accessible via `F12`). You can also see the [VerticalLayout in Vaadin Sampler](https://demo.vaadin.com/sampler/#ui/layout/vertical-layout).
Here you can experiment with the various VerticalLayout sizes (explicit height vs undefined height) - you can see
how the slot size changes, how the slots themselves are positioned and how the components are positioned inside
of those slots.

## Fields

Another very important set of components are those that handle user input. All of the input components are documented
on the Vaadin site, for example on the [TextField documentation page](https://vaadin.com/docs/v8/framework/components/components-textfield.html).

Every field has a different purpose which we will not document here; for further questions please take a look at the following
resources:

* The [Vaadin Documentation on Components](https://vaadin.com/docs/v8/framework/components/components-overview.html)
* The [Vaadin Sampler](https://demo.vaadin.com/sampler/#ui/data-input) shows all (input) components.

Also please read the [Creating Forms](forms.md) article for more information on how to build forms in VoK.

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

You can therefore simply store the reference to the newly created component in a local variable, or a (lateinit) field, or pass it to a function -
you can do anything you want, since DSL is nothing more but a program in Kotlin, albeit with a fancy syntax.

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
@VaadinDsl
fun (@VaadinDsl HasComponents).nameAgeForm(block: (@VaadinDsl NameAgeForm).()->Unit = {}): NameAgeForm = init(NameAgeForm(), block)
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

## The `Composite` Pattern

The advantage of extending from `Composite`, instead of extending the layout (e.g. `VerticalLayout`) directly, is as follows:

* The component public API is not polluted by methods coming from the `VerticalLayout`,
  resulting in a more compact and to-the-point API. The API coming from `Composite` is
  tiny in comparison.
* Since the `VerticalLayout` API doesn't leak into our component, we are free to
  replace the `VerticalLayout` with any other layout in the future, without breaking the API.
* The UI structure is more clearly visible. Take the `ButtonBar` class below as
  an example: it can clearly be seen that the buttons are nested in the `HorizontalLayout`:

Example 1.: ButtonBar extending Composite with a clear UI hierarchy
```kotlin
class ButtonBar : Composite() {
    init {
        horizontalLayout {
            button("ok")
        }
    }
}
```

Example 2.: ButtonBar extending HorizontalLayout without a clear indication that
the button is nested in a horizontal layout:
```kotlin
class ButtonBar : HorizontalLayout() {
    init {
        button("ok")
    }
}
```

## More Resources

To learn Vaadin:

* [Official Vaadin website](https://www.vaadin.com)
* [Vaadin Documentation](https://vaadin.com/docs/v8) - we recommend to download and read the Vaadin Book PDF.

To learn about Kotlin DSLs:

* [DSLs: Explained](dsl_explained.md)
* [Type-safe builders](https://kotlinlang.org/docs/reference/type-safe-builders.html)
