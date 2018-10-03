[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v10/flow/Overview.html) to deliver the UI.
Vaadin lets you forget the web and develop user interfaces much like you would develop a desktop application with conventional Java toolkits such as AWT, Swing, or SWT.
You add components such as Button and TextField into the page, nesting them in layouts which then position the components.

> To learn Vaadin:
>
> * [Official Vaadin website](https://www.vaadin.com)
> * [Vaadin Documentation](https://vaadin.com/docs/v10/index.html) - we recommend to read the Vaadin Flow documentation.

## Introduction

The web is composed of HTML pages. The basic building block of a HTML page is the *HTML element*, such as `<div>` or `<a>`.
However, with the advent of [Web Components](https://en.wikipedia.org/wiki/Web_Components) the paradigm is shifted - you compose
the page by using the Web Components such as [Paper Input](https://www.webcomponents.org/element/@polymer/paper-input)
or [Vaadin Grid](https://www.webcomponents.org/element/vaadin/vaadin-grid), which then host the necessary HTML elements themselves.

The web has been reinventing itself, pushing typical Java frameworks such as JSF away and bringing JavaScript
frameworks such as Angular and React to the front, reducing Java server side to a REST endpoint service.
In this regard, Vaadin is different.

Instead of being a mere REST directory, Vaadin provides means to control the Web Components from Server side. It employs
so-called Vaadin Flow to synchronize a subset of Web Component properties (the Web component state) between client-side and server-side.
Every Vaadin component then consists of two parts:

* The client-side part of the component is a standard Web Component. The web component is typically implemented in Polymer
  (this enables value change observers and Polymer model templates integration with Vaadin), but web components based on other JavaScript libraries (or
  plain web components based on no library at all) are also supported.
* The server-side part of the component is a Java class that extends from Flow Component and uses APIs provided by Flow to
  catch events (such as button click events) and allows to control the underlying web component via Polymer properties, plain HTML attributes
  or via calls to JavaScript functions exposed on the web component itself.

This kind of approach makes it incredibly easy to add e.g. Google Maps to your site. You just use the components' server-side Java API and you don't
have to care about what the HTML will look like, or how exactly it will fetch the data. The code looks like follows:

```java
class MyView : VerticalLayout {
  init {
    setSizeFull()
    val maps = GoogleMaps()
    maps.setSizeFull()
    addComponent(maps)
  }
}
```

> *Info*: There are great resources on how to write a web component using Polymer 2,
for example the [Getting Started With Polymer 2](https://www.polymer-project.org/2.0/start/).
In this guide we will not focus on the client-side part; instead we will focus on how to compose the server-side components.

The components are typically rich in functionality. For example `ComboBox` does not render into the HTML `<input>` element but it instead renders
a rich `<div>` hierarchy which allows for features which are not possible with the `<input>` element, such as auto-completion.
There is a big palette of pre-made components, and we use server-side Java code to we compose and nest them. Vaadin
then makes sure to call the client-side of every component, to render the proper HTML elements. The rendering
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

The following text doesn't expect you to be familiar with the Vaadin framework. However, it is best to have at least basic understanding of the Kotlin
programming language. If you feel lost in the following text, please take your time to learn of the Kotlin language features first.

## Available Components

Lots of components are already built-in. You can [browse built-in Vaadin 10 components](https://vaadin.com/components/browse).
You can also visit [Vaadin 10 Documentation on Components](https://vaadin.com/docs/v10/flow/components/tutorial-flow-components-setup.html) to read about all of the available
components.

You can find additional components at the [Vaadin Directory](https://vaadin.com/directory); just make sure to search for components intended for Vaadin 10 since
Vaadin 8/7 components won't work with Vaadin 10.

It is also possible to integrate a standalone Web Component into Vaadin. Just find a (preferably Polymer 2-based)
component at the [webcomponents.org](https://www.webcomponents.org/) page, then find and include appropriate [webjar](https://www.webjars.org/)
to include the web component into your project. Then, read the [Integrating a Web Component](https://vaadin.com/docs/v10/flow/web-components/integrating-a-web-component.html)
Vaadin 10 documentation on how to integrate web component into Vaadin 10.

## Creating Views

Please git clone the [VoK Hello World App Vaadin 10](https://github.com/mvysny/vok-helloworld-app-v10) - we're going to experiment on that app.

If you open the [WelcomeView.kt](https://github.com/mvysny/vok-helloworld-app-v10/blob/master/web/src/main/kotlin/com/example/vok/WelcomeView.kt)
file, you'll notice that it extends from the `VerticalLayout`. By extending the `VerticalLayout` we state that the root layout of this view is going to be vertical.

> Vaadin 10 no longer introduces its own layout manager but uses
the CSS Flex Layout extensively. Yet, we still provide `VerticalLayout` and `HorizontalLayout` as a wrappers over
the Flex Layout, for those familiar with Vaadin 8's `VerticalLayout` and `HorizontalLayout` and unfamiliar with the Flex layout.
However, the behavior of those two classes is a bit different with Vaadin 10 than it was in Vaadin 8, please
read [Vaadin 10 server-side layouting for Vaadin 8 and Android developers](http://mavi.logdown.com/posts/6855605)
for a full explanation.

Let's now replace the contents of the `WelcomeView` by a single button. Rewrite the `WelcomeView` class as follows:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView: VerticalLayout() {
    init {
        button("Click me") {
            onLeftClick {
                Notification.show("Clicked")
            }
        }
    }
}
```

The `button()` function simply creates the Vaadin `Button`, sets a caption for it, inserts it into the parent layout (in this case,
the root `VerticalLayout`/`WelcomeView`) and runs the configuration block for that button. The configuration block adds a left click
listener.

> Note: to read the technical explanation how this exactly works, please read the [Using DSL to write structured UI code](http://mavi.logdown.com/posts/7073786)
article.

Let us add a simple form, consisting of two text fields:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView: VerticalLayout() {
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

> The `FormLayout` is a powerful responsive layout which can change the number of columns depending on its width. Please find out more
at the [vaadin-form-layout](https://vaadin.com/components/vaadin-form-layout) documentation page.

## Referencing Components

The `textField()` function also returns the newly created `TextField`. This is handy if we want to reference those text fields later, for
example from the button click handler:

```kotlin
package com.example.vok

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView: VerticalLayout() {
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
package com.example.vok

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("")
class WelcomeView: VerticalLayout() {
    init {
        val form = nameAgeForm()
        button("Click me") {
            onLeftClick {
                Notification.show(form.greeting)
            }
        }
    }
}

class NameAgeForm : FormLayout() {
    private val nameField = textField("Name:")
    private val ageField = textField("Age:")
    val greeting: String get() = "Hello, ${nameField.value} of age ${ageField.value}"
}
fun HasComponents.nameAgeForm(block: NameAgeForm.()->Unit = {}): NameAgeForm = init(NameAgeForm(), block)
```
