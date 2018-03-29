[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Navigating in your app

The Vaadin `Navigator` resolves http paths to views, in order to show different parts of the app to the user. For example,
when the user navigates to `http://localhost:8080/invoices` the Navigator makes sure that the `InvoicesView` UI component is created
and placed into your `UI`.
See [Vaadin Book on Navigator](https://vaadin.com/docs/-/part/framework/advanced/advanced-navigator.html) for more details.

By default you need to manually register all of your Views into the Vaadin `Navigator` class. This is tedious and error-prone. Luckily,
Vaadin-on-Kotlin provides additional support for View auto-discovery. To enable this:

1. You need to register the Karibu-DSL's `autoViewProvider`, in your `UI.init()` as follows:
```kotlin
    fun init() {
        navigator = Navigator(this, content as ViewDisplay)
        navigator.addProvider(autoViewProvider)
    }
```
Clone the [VoK Hello World App](https://github.com/mvysny/vok-helloworld-app) example project and find
the [MyUI.kt](https://github.com/mvysny/vok-helloworld-app/blob/master/web/src/main/kotlin/com/example/vok/MyUI.kt) class for an example on how this is done.

2. Create your views. A `view` is a Kotlin class which a) extends a Vaadin UI component or a layout, and b) implements the `View`
   interface.

3. To auto-discover your views, annotate your views with the `@AutoView` annotation. `autoViewProvider` will
   now discover your views automatically and register them to the `Navigator`.

4. To navigate to given view, just call `navigateToView<YourView>()`.

## URL Path to Class Name Mapping

The mapping is generally done by converting the `CamelCaseView` Kotlin
class naming convention to hyphen-separated string `camel-case-view`, dropping the trailing `-view`.
You can always override the name in the `@AutoView` annotation, e.g. you can make one view
'primary' by mapping it to an empty view `@AutoView("")` - this view will be shown initially when the user
browses to your app's [http://localhost:8080](http://localhost:8080).

For example, URL [http://localhost:8080/my-form](http://localhost:8080/my-form)
will navigate towards the class named `MyFormView` (which must be of course annotated with `@AutoView`). The mapping is done by
the `autoViewProvider` and the `@AutoView` annotation, just check out the Kotlin documentation on those two guys.

## Parameters

The URL paths in VoK consists of the view name, followed by optional parameters. The URL format is as follows:

```
http://localhost:8080/[view-name]/[parameter-0]/[parameter-1]/[parameter-2]?queryparam1=value1&queryparam2=value2
```

For example, `http://localhost:8080/person/20?lang=en` will navigate you to the `PersonView`. The parameters will be passed into the
View's `enter()` function as follows:

```kotlin
override fun enter(event: ViewChangeListener.ViewChangeEvent) {
    println(Page.getCurrent().location.queryMap)   // will print [lang=[en]]
    println(event.parameterList)                   // will print [0=20]
    val personId: Long = event.parameterList[0]?.toLong() ?: throw IllegalArgumentException("Expected the ID parameter")
    val language: String = Page.getCurrent().location.queryMap["lang"]?.get(0) ?: "en"
}
```

The `queryMap` is a `Map<String, List<String>>` containing the parsed query string. Since a key may be present multiple times in the query,
the map maps key to a list of values. That's why the `"lang"` key will be mapped to a list containing one item, `"en"`.

The `parameterList` is a `Map<Int, String>` of unnamed parameters, mapping from parameter index to the parameter value.

## Navigating to views

To navigate to a view named `PersonView`, just call `navigateToView<PersonView>()`.
The `navigateToView()` function accepts a list of String parameters which will then be passed into the function, for example:

```kotlin
navigateToView<PersonView>("20")
```

However, it is a good practice to introduce a companion function into the `PersonView` itself which will check the parameter types and
values:

```kotlin
class PersonView : View {
    // ...
    companion object {
        fun navigateTo(id: Long) {
            navigateToView<PersonView>(id.toString())
        }
    }
}
```

Then you can simply navigate to the person view by calling `PersonView.navigateTo(25)`.
