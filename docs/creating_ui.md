[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Creating UIs

Vaadin-on-Kotlin uses the [Vaadin Framework](https://vaadin.com/docs/v8/framework/introduction/intro-overview.html) to deliver the UI.
Vaadin is a single-page Rich Internet Application framework with the programming paradigm that resembles desktop toolkits like JavaFX
and Swing. You add components such as Button and TextField into the page, nesting them in layouts which define the component placement on page.

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
            textField("Age:"
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

The `formLayout()` function will create Vaadin `FormLayout` component and add it into the root `VerticalLayout`. Then it runs the configuration
block, acting as a parent layout in that block. This is very important since that will correctly allow the `textField()` function to insert
the newly created `TextField` class into the `FormLayout` itself, and not into the root `VerticalLayout`.

todo more
