[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v10/flow/Overview.html) to deliver the UI.
Vaadin lets you forget the web and develop user interfaces much like you would develop a desktop application with conventional Java toolkits such as AWT, Swing, or SWT.
You add components such as Button and TextField into the page, nesting them in layouts which then position the components.

> To learn Vaadin:
>
> * [Official Vaadin website](https://www.vaadin.com)
> * [Vaadin Documentation](https://vaadin.com/docs/v10/index.html) - we recommend to read the Vaadin Flow documentation.

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
