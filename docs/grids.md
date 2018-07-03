[Index](index.html) | [Getting Started](gettingstarted.html) | [Guides](vok-guides.html)

# Using Grids

Vaadin Grid allows you to display tabular data in a scrollable div, lazy-loading rows as they are scrolled into the viewport.
It also allows you to sort and filter for particular column.

Vaadin Grid is probably the most versatile and complex component, and it may take some time to learn all of its capabilities.
Just take things slow and you'll be used to Grid in no time.

You can learn about Grid capabilities in the [official Vaadin 8 Grid Documentation page](https://vaadin.com/docs/v8/framework/components/components-grid.html).

There are more ways of using Grid; in the tutorial we will use the easiest way as endorsed by the VoK framework.

## General Rules

Since the Grid scrolls by itself, it makes no sense to set its height to `wrapContent`. The best way is to make the Grid to fill its parent
in both width and height directions, simply by calling `setSizeFull()`; then just make sure its parent layout also fills its parent (or has
a particular height set, e.g. `200.px`) and so on.

## Binding to Data

VoK entities automatically provide a `DataProvider` instances. It is the means to populate Grid with the data. The simplest Grid
can be constructed as follows:

```kotlin
grid(dataProvider = Person.dataProvider) {
}
```

This `DataProvider` automatically fetches pages of data lazily from a database table, supporting all optional features such as sorting
and filtering.

> Note: if you wish to display an outcome of a complex SELECT JOIN, please read the [Showing an arbitrary output of any SQL SELECT command](databases.md)
section.

You can configure the `Person.dataProvider` in multiple ways:

* If the user has not selected any particular sorting in the Grid, you can provide a default one: `grid(dataProvider = Person.dataProvider.sortedBy(Person::name.asc)) {}`
* You can also impose an unremovable filter which limits the data the user is able to see: `grid(dataProvider = Person.dataProvider.withFilter { Person::age ge 18 }) {}`

## Adding columns

By default the Grid will show no columns. You can add columns easily, by calling the `addColumnFor()` function as follows:

```kotlin
grid(dataProvider = Person.dataProvider) {
    expandRatio = 1f; setSizeFull()

    // a sample of how to reconfigure a column
    addColumnFor(Person::id) { isSortable = false }
    addColumnFor(Person::name)
    addColumnFor(Person::age)
    addColumnFor(Person::dateOfBirth) {
        setRenderer(LocalDateRenderer())
    }
    addColumnFor(Person::maritalStatus)
    addColumnFor(Person::alive)
    addColumnFor(Person::created) {
        // example of a custom renderer which converts value to a displayable string.
        setRenderer({ it.toString() }, TextRenderer())
    }

    // add additional columns with buttons
    addColumn({ "Show" }, ButtonRenderer<Person>({ event -> PersonView.navigateTo(event.item) }))
    addColumn({ "Edit" }, ButtonRenderer<Person>({ event -> createOrEditPerson(event.item) })).id = "edit"
    addColumn({ "Delete" }, ButtonRenderer<Person>({ event -> deletePerson(event.item.id!!) }))
}
```

The `addColumnFor()` will set the column header automatically, by converting `dateOfBirth` camelCase to `Date Of Birth` Human Friendly
format. It will also use the default renderer/converter pair which simply calls `.toString()` on any value of that particular property.
The column is also by default sortable. To override this behavior, you can provide a configuration block to the `addColumnFor()` function
which will allow you to configure the column further (simply by calling setters/methods on the `Grid.Column` receiver).

The `addColumnFor()` function is tailored towards creating columns bound to a bean property. If you wish to create columns not backed by
any property (say, a column with the "Show Details" link), you can simply use the `addColumn()` function as provided by the Vaadin Grid
itself. Please see above for the example.

## Sorting

The Grid is initially unsorted and shows the data in the whatever order the `DataProvider` considers the default one. In entity data providers
the data is typically sorted by the primary key by default, which makes little sense for the user. You can hence create a data provider which
provides a different default sorting instead: `grid(dataProvider = Person.dataProvider.sortedBy(Person::name.asc)) {}`

Even better is to tell the Grid to initially sort by given column, by calling one of the `Grid.sort()` functions. That way, a visual
sort indicator is displayed for the user as well.

> Note: please make sure to create appropriate database index for every sortable column, otherwise the database SELECTs would be quite slow.

## Column Widths

Columns have by default undefined width, which causes automatic sizing based on the widths of the displayed data.
You can set column widths explicitly by pixel value with `setWidth()`, or relatively using expand ratios with `setExpandRatio()`.

When using expand ratios, the columns with a non-zero expand ratio use the extra space remaining from other columns, in proportion
to the defined ratios. Do note that the minimum width of an expanded column by default is based on the contents of the column
(the initially rendered rows). To allow the column to become narrower than this, use `setMinimumWidthFromContent(false)` (introduced in 8.1).

You can specify minimum and maximum widths for the expanding columns with `setMinimumWidth()` and `setMaximumWidth()`, respectively.

The user can resize columns by dragging their separators with the mouse. When resized manually, all the columns widths are set to explicit
pixel values, even if they had relative values before.

## Filters

In the simplest case, when you do not wish to fine-tune the filter appearance, you can simply add the following command into your
`grid{}` block:

```kotlin
grid(dataProvider = Person.dataProvider) {
    // ..

    // automatically create filters, based on the types of values present in particular columns.
    grid.appendHeaderRow().generateFilterComponents(grid, Task::class)
}
```

VoK provides means to auto-generate filter components for all bean properties shown in the Grid itself. For more information on this topic
please read the [VoK Vaadin 8 Utils Documentation](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-util-vaadin8/README.md).

> Note: please make sure to create appropriate database index for every filtrable column, otherwise the database SELECTs would be quite slow.

You can do quite a lot with the data providers - please see the (Databases Guide)[databases.md] for more details.

## Conditional Row formats

Often it is required to change the row formating of a Vaadin grid depending on the rows content, for example in order to highlight certain values.

```kotlin
grid (dataSource) {
    // ..
    styleGenerator = StyleGenerator<Person> {
        if (it.name == "John Doe") "redFlag" else ""
    }
}
```

Definition of redFlag Style in mytheme.scss:

```css
@import "../valo/valo.scss";
@mixin mytheme {
    @include valo;
    …
    .v-grid-row.redFlag > .v-grid-cell{
        background-color: coral;
        font-weight: 400;
    }
}
```

# Grid Editor

One of the powerful functions of Vaadin Grid is the inline editor which allows
a the developer the implement a complete CRUD stack with minimum coding
in a spreadsheet like manner. In order to properly set-up the grid editor,
the following steps need to be taken:

1. making the grid editable
2. setting a simple editor component for each column **or** setting up a complex editor component and a converting field binder for each column
3. Saving data after value change
4. Creating support for deleting and adding a row

## Making Grid editable

As a first step, the grid editor must be enabled. The `Editor` uses Vaadin `Binder` to bind to
the editing fields, exactly as it is done when writing forms.
By default, the binder will not be aware of validation constraints; therefore it is good to
overwrite the default binder with the `beanValidationBinder`.

There are 2 different editing modes for Vaadin grids: **buffered** and **unbuffered**:

* In buffered mode (which is the standard setting), whenever a row is selected for editing,
  a bean with the current row content is created and passed on to the editor. Only after all changes
  in all columns of this row are made, all values are valid and the user has pressed “Save”, this edited bean is passed back to the grid.
  At this point, the new values will appear in the grid but not in the database. In order to propagate the changes into the database
  we need to hook to the "save" event and save the bean into the database, simply by calling `save()`.
* In unbuffered mode, any value change on a particular field will be immediately saved to the grid,
  thus the developer has to either hook the value-change on every field for saving data back to the datasource,
  or to implement a separate commit changes process.

Since using the buffered mode is safer (in a sense that the bean is overwritten only when validations have passed),
we recommend to use the buffered mode.

```kotlin
grid<Person>(dataSource = Person.dataSource) {
    editor.isEnabled = true; editor.binder = beanValidationBinder(); editor.isBuffered = true
    editor.addSaveListener { e -> e.bean.save(); refresh() }
    // configure columns, etc
}
```

## Simple editor components

The editor is shown when the Grid row is double-clicked. By default there are no editor components configured and hence
no information in the editor can be changed. Therefore, we need to either:

* set the editor component directly, for simple cases when there is no value conversion needed; or
* set the binder manually.

Either of those actions will automatically switch the column to the edit mode (sets the `Column.isEditable` to true).

In simple cases when there is no data conversion necessary (for example editing a `String` field with a `TextField`), setting up the editor Component
(which handles the data entry) is all that's needed:

```kotlin
grid { // ..
    addColumnFor(Person::name) { setEditorComponent(TextField()) }
}
```

Another example might be to use a combobox of static values without conversion:

```kotlin
// ..
addColumnFor(Person::gender) {
    setEditorComponent(ComboBox(null, listOf("-", "m", "f")).apply { isEmptySelectionAllowed = false })
}
```

Yet another example is `DateField` with custom formatting. Please note that
the formatting has to be handled separately for the grid column (the renderer) and the editor.

```kotlin
// ..
addColumnFor(Person::birthday) {
    setRenderer(LocalDateRenderer("dd.MM.yyyy"))
    setEditorComponent(DateField().apply { dateFormat = "dd.MM.yyyy" })
}
```

## Conversion on editing

If any conversion has to be done between the type of the editor component and the type of
the data (for example editing a `Long` value in a `TextField`), we need to use the binder.

```kotlin
// ..
addColumnFor(Person::age) {
    caption = "age in years"
    editorBinding = editor.binder.forField(TextField()).toInt().bind(Person::age)
    setRenderer(NumberRenderer ("%,d", Locale.GERMANY))
    setStyleGenerator({ "v-align-right" })
}
```

## Saving edited data

```kotlin
editor.addSaveListener({ event -> event.bean.save(); refresh() })
```

## Creating support for deleting and adding a row

An easy way of deleting a row is by adding an additional column with a delete button.

```kotlin
// ..
addColumn({ "\u274C" }, ButtonRenderer<Person>({ event ->
    event.item.delete()
    refresh()  // this will call the Grid.refresh() extension method which will in turn call DataProvider.refreshAll()
}))
```

Likewise, a new row can be create by the push of a button, which than can be edited like a normal row of data. This code is best placed below the grid and needs the grid to get a name in order to be referenced. Beware that as this method always creates a new database row, actions have to be taken in case of unique constraints on the database.

```kotlin
val gridPerson = grid(dataSource) {
    // ..
}

button("New Line") {
  onLeftClick {
    Person().save()
    gridPerson.refresh()
    gridPerson.scrollToEnd()
  }
}
```

## All-in-One CRUD Grid example

```kotlin
import com.github.vok.framework.sql2o.vaadin.dataProvider
import com.github.vok.framework.sql2o.vaadin.generateFilterComponents
import com.github.vok.karibudsl.*
import com.github.vokorm.*
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.data.sort.SortDirection
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.LocalDateRenderer
import com.vaadin.ui.renderers.NumberRenderer
import java.time.LocalDate
import java.util.*
import javax.validation.constraints.*

data class Person(
    override var id: Long? = null,

    var name: String? = "new",

    @field:NotNull
    @field:Min(0)
    @field:Max(100)
    var age: Long = 0,

    @field:NotNull
    var dateOfBirth: LocalDate = LocalDate.now(),

    @field:NotNull
    @field:Pattern(regexp="[-mf]")
    var gender: String = "-"

) : Entity<Long> {
    companion object : Dao<Person>
}

@AutoView("")
class MainView: VerticalLayout(), View {
    init {
        setSizeFull()
        isMargin = false
        val gridPerson = grid(dataProvider = Person.dataProvider) {
            expandRatio = 1f; setSizeFull()

            editor.isEnabled = true; editor.binder = beanValidationBinder()
            editor.addSaveListener { e -> e.bean.save(); refresh() }

            addColumnFor(Person::name) { width = 300.0; setEditorComponent(TextField()) }
            addColumnFor(Person::age) {
                width = 100.0
                editorBinding = editor.binder.forField(TextField()).toInt().bind(Person::age)
                setStyleGenerator({ "v-align-right" })
                setRenderer(NumberRenderer ("%,d", Locale.GERMANY))
            }
            column(Person::gender) {
                caption = "Gen"
                val combo = ComboBox<String>(null, listOf("-", "m", "f"))
                combo.isEmptySelectionAllowed = false
                setEditorComponent(combo)
                width = 100.0
            }
            column(Person::dateOfBirth) {
                caption = "Birthday"
                setRenderer(LocalDateRenderer("dd.MM.yyyy"))
                val dateField = DateField()
                dateField.dateFormat = "dd.MM.yyyy"
                setEditorBinding(editor.binder.forField(dateField).bind(Person::dateOfBirth))
                width = 200.0
            }
            editor.addSaveListener({ event -> event.bean.save(); refresh() })

            addColumn({ "\u274C" }, ButtonRenderer<Person>({ event ->
                event.item.delete(); refresh()
            })).width=70.0
            styleGenerator = StyleGenerator<Person> {
                if (it.name == "new") "redFlag" else ""
            }
            appendHeaderRow().generateFilterComponents(this@grid, Person::class)
            sort(Person::id.name, SortDirection.ASCENDING)
        }
        button("New Line", {
            Person().save()
            gridPerson.refresh()
            gridPerson.scrollToEnd()
        })
    }
}
```

Also please see the [CrudView](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-example-crud-sql2o/src/main/kotlin/com/github/vok/example/crud/personeditor/CrudView.kt)
class for a complete demo; see the [vok-example-crud-sql2o](https://github.com/mvysny/vaadin-on-kotlin/tree/master/vok-example-crud-sql2o) for documentation
on the example project.

## Other Grid options

For more information on the Vaadin Grid, please consult the [official Vaadin 8 Grid Documentation page](https://vaadin.com/docs/v8/framework/components/components-grid.html).
