[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v8/framework/introduction/intro-overview.html) to deliver the UI.
Vaadin lets you forget the web and program user interfaces much like you would program a desktop application with conventional Java toolkits such as AWT, Swing, or SWT.
You add components such as Button and TextField into the page, nesting them in layouts which then position the components.

> To learn Vaadin:
>
> * [Official Vaadin website](https://www.vaadin.com)
> * [Vaadin Documentation](https://vaadin.com/docs/v8) - we recommend to download and read the Vaadin Book PDF.

## Creating Views

Please git clone the [VoK Hello World App](https://github.com/mvysny/vok-helloworld-app) - we're going to experiment on that app.

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
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    }
}
```

The `button()` function simply creates the Vaadin `Button`, sets a caption into it, inserts it into the parent layout (in this case,
the root `VerticalLayout`/`WelcomeView`) and runs the configuration block for that button. The configuration block adds a left click
listener.

> Note: to read the technical explanation how this exactly works, please read the [Using DSL to write structured UI code](http://mavi.logdown.com/posts/7073786)
article.

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
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    }
}
```

The `formLayout()` function creates Vaadin `FormLayout` component and adds it into the root `VerticalLayout`. Then it runs the configuration
block, acting as a parent layout in that block. This is very important since that will correctly allow the `textField()` function to insert
the newly created `TextField` class into the `FormLayout` itself, and not into the root `VerticalLayout`.

## Referencing the components

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
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
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
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    }
}
```
