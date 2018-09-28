# Creating Forms

When creating UI for forms, we typically solve two separate tasks:

* We position the form components on the screen and we try to make the form pleasant for the eye and easy to use
* We populate the components with data, visualise the values, validate the values and then gather the values into a
  single bean when the user presses the "Save" button.

## Positioning Form Components

We typically use `FormLayout` to position fields such as `TextField` in a form. Please see below on
the example of a very simple form:

```kotlin
class PersonForm : FormLayout() {
    init {
        w = wrapContent
        textField("Name:") {
            focus()
        }
        textField("Age:")
        dateField("Date of birth:")
        comboBox<MaritalStatus>("Marital status:") {
            setItems(*MaritalStatus.values())
        }
        checkBox("Alive")
    }
}
```

The form simply uses basic `FormLayout` to position the components. You can of course use any layout you wish,
and you can even nest multiple levels of layouts, in order for your form to provide a good UX to the user.

Most commonly, you either use `GridLayout` to position the fields of a complex forms inside of a grid.
However, a better idea is to use a responsive layout - the `CssLayout` accompanied by a couple of properly designed
CSS rules. Such a layout lays out fields in a grid but is able to reduce columns when the screen space is narrow.
You can for example use the [Responsive Layout Add-on](https://vaadin.com/directory/component/responsive-layout).

## Binding Values

Vaadin 8 uses the `Binder` class to bind bean properties into particular UI fields such as `TextField` or `CheckBox`.
Say that we have the following bean `Person`:
```kotlin
data class Person(
        var id: Long? = null,
        var personName: String? = null,
        var age: Int? = null,
        var dateOfBirth: LocalDate? = null,
        var maritalStatus: MaritalStatus? = null,
        var alive: Boolean? = null
)
```

We will bind individual properties of the `Person` bean to the UI fields using the `Binder`. The modified form
follows:

```kotlin
class PersonForm : FormLayout() {
    val binder = Binder<Person>(Person::class.java)
    init {
        w = wrapContent
        textField("Name:") {
            focus()
            bind(binder).trimmingConverter().bind(Person::personName)
        }
        textField("Age:") {
            bind(binder).toInt().bind(Person::age)
        }
        dateField("Date of birth:") {
            bind(binder).bind(Person::dateOfBirth)
        }
        comboBox<MaritalStatus>("Marital status:") {
            setItems(*MaritalStatus.values())
            bind(binder).bind(Person::maritalStatus)
        }
        checkBox("Alive") {
            bind(binder).bind(Person::alive)
        }
    }
}
```

In order to populate the UI fields with data from an existing person, we simply run

```kotlin
val form = PersonForm()
form.binder.readBean(person)
```

## Validating Input

We have two options on how to validate the input:

* We either add validators directly to the binder, when creating a binding. However, this type of
  validations can not be run outside of Vaadin UI (e.g. they can not be run by the database storage layer,
  to validate objects). Therefore, it's usually better to use Java Validator API
* The Java Validator API (or JSR-303). We add validation annotations to the individual fields of the bean,
  then we run validator to pick up the annotations and run validations on the bean, failing if the values
  do not comply.

For example, adding annotations to the `Person` class would make it look like this:

```kotlin
data class Person(
        var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
        var personName: String? = null,

        @field:NotNull
        @field:Min(15)
        @field:Max(100)
        var age: Int? = null,

        var dateOfBirth: LocalDate? = null,

        @field:NotNull
        var created: Instant? = null,

        @field:NotNull
        var maritalStatus: MaritalStatus? = null,

        @field:NotNull
        var alive: Boolean? = null
)
```

Now you can simply validate the bean outside of Vaadin UI, simply by using the following code:

```kotlin
val violations: Set<ConstraintViolation<Person>> = Validation.buildDefaultValidatorFactory().validator.validate(person)
if (!violations.isEmpty()) {
    throw ConstraintViolationException(violations)
}
```

> **Note**: If you're using VoK entities, just call `person.validate()`. More on that in the database guide.

In order to properly validate the bean in the Vaadin UI, you must use a special binder: the `BeanValidationBinder`:

```kotlin
class PersonForm : FormLayout() {
    val binder = beanValidationBinder<Person>()
    ...
```

In order to save the bean, we can simply run the following code in a "Save" button onclick listener:

```kotlin
if (!form.binder.validate().isOk || !form.binder.writeBeanIfValid(person)) {
    saveButton.componentError = UserError("Please fix the errors on the form")
} else {
    person.save()
}
```

The `binder.validate()` will mark invalid components visually with a red border and will add the error message
as a tooltip. The `binder.writeBeanIfValid(person)` will write the data from the UI components into the `person`
instance, but only if all the data passes the validator.

## More Information

You can read more information on the binder itself in the [Binding Data to Forms](https://vaadin.com/docs/v8/framework/datamodel/datamodel-forms.html)
Vaadin documentation.

> **Warning:** If you intend to use the Java validation annotations, you must not use the `.bind(callback, callback)` binding
functions since they have no way of knowing which field the binding is bound to, and therefore can't discover the annotations
from that bind. You **must** use `.bind(String)` or `.bind(KProperty)` otherwise your validations annotations will be silently
ignored by the binder.
