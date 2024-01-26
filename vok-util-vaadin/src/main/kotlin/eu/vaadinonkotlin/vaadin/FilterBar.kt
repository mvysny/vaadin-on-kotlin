package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributools.BrowserTimeZone
import com.github.mvysny.kaributools.getColumnBy
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateInterval
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberInterval
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasSize
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.HeaderRow
import com.vaadin.flow.component.shared.HasClearButton
import com.vaadin.flow.component.textfield.*
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.provider.InMemoryDataProvider
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.function.SerializablePredicate
import com.vaadin.flow.shared.Registration
import eu.vaadinonkotlin.FilterFactory
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Creates a [FilterBar] which produces [com.gitlab.mvysny.jdbiorm.condition.Condition]s. Perfect for using with the
 * [EntityDataProvider].
 * ```kotlin
 * val filterBar = grid.appendHeaderRow().asFilterBar(grid)
 * ```
 * @param T the bean type present in the [grid]
 * @param grid the owner grid
 */
public fun <T : Any> HeaderRow.asFilterBar(grid: Grid<T>): FilterBar<T> =
        FilterBar(this, grid)

/**
 * Wraps [HeaderRow] and helps you build [Filter] components. Call [forField] to
 * configure filter for particular [Grid.Column], for example:
 * ```kotlin
 * personGrid = grid(dataProvider = Person.dataProvider) {
 *   flexGrow = 1.0
 *   appendHeaderRow()
 *   val filterBar: VokFilterBar<Person> = appendHeaderRow().asFilterBar(this)
 *
 *   addColumnFor(Person::name) {
 *     filterBar.forField(TextField(), this).ilike()
 *   }
 * }
 * ```
 *
 * After the user
 * changes the value in the filter UI component, a new [Grid] filter of type [Condition] is computed and
 * set to [Grid.getDataProvider]. The DataProvider must be able to accept [Condition]; use either [EntityDataProvider]
 * or Vaadin built-in ListDataProvider.
 *
 * Every filter component is configured using the [configure] method.
 *
 * You can also call [setCustomFilter] to set or remove completely custom conditions which will
 * be ANDed with component-bound filters. You can use this functionality to e.g.
 * implement a global search bar, or to create an unremovable [FILTER] (e.g. filter rows
 * for the current user only).
 *
 * WARNING: WORK IN PROGRESS, THE API MAY CHANGE AT ANY TIME.
 * @param BEAN the type of items in the [grid].
 * @param FILTER the type of the filters accepted by [grid]'s [ConfigurableFilterDataProvider]. For VoK
 * it's usually [Filter].
 * @param grid the owner grid. It is expected that [Grid.getDataProvider] is of type [VokDataProvider]<BEAN>
 * (or [ConfigurableFilterDataProvider]<BEAN, FILTER, FILTER>).
 * @property headerRow the wrapped [HeaderRow]
 * @param filterFactory used to combine [FILTER] values when multiple [FILTER]s are applied
 * (using the [FilterFactory.and] function).
 */
@Deprecated("too complex and still only supports basic use-case of putting filter components into a filter bar. Projects will most probably create their own rich-looking filter bars and will ignore this one. I'm keeping this around for example projects")
public open class FilterBar<BEAN : Any>(
        public val headerRow: HeaderRow,
        public val grid: Grid<BEAN>
) : Serializable {

    /**
     * The current filter. Defaults to [Condition.NO_CONDITION].
     */
    public var currentFilter: Condition = Condition.NO_CONDITION
        private set

    /**
     * Lists all currently registered [Binding]s. The value is a [Registration] of the
     * [Binding.addFilterChangeListener].
     */
    private val bindings: MutableMap<Binding<BEAN>, Registration> = mutableMapOf()

    /**
     * Contains a set of custom [FILTER]s. See [setCustomFilter] for more details.
     */
    private val customFilters: MutableMap<String, Condition> = mutableMapOf()

    private fun applyFilterToGrid(filter: Condition) {
        if (grid.dataProvider.isInMemory) {
            // need to wrap Condition in SerializablePredicate
            val c = SerializablePredicate<BEAN> { filter.test(it) }
            (grid.dataProvider as InMemoryDataProvider<BEAN>).setFilter(c)
        } else {
            @Suppress("UNCHECKED_CAST")
            (grid.dataProvider as ConfigurableFilterDataProvider<BEAN, Any, Any>).setFilter(filter)
        }
    }

    /**
     * Invoked when a [FILTER] changes. By default does nothing. Read [currentFilter]
     * to obtain the current [FILTER].
     *
     * Invoked after the [FILTER] has already been set to [Grid]'s [com.vaadin.flow.data.provider.DataProvider].
     */
    public var onFilterChanged: () -> Unit = {}

    /**
     * Along with component/column-scoped [FILTER]s, you can add a completely custom [FILTER]s
     * using this method. Two use-cases:
     * 1. call `setCustomFilter("unremovable-filter-by-user", buildFilter { Record::userId eq Session.currentUserId })`
     * to create an unremovable [FILTER] which only passes records intended for the current user.
     * 2. create a global search [TextField] above the [Grid], then call
     * `setCustomFilter("global", ...)` to set the new global [FILTER].
     *
     * All [FILTER]s - both custom and column-scoped - are ANDed together.
     * @param key an arbitrary key to set the filter under. Setting a new [FILTER] with
     * an existing key overwrites the old [FILTER] stored under that key.
     * @param filter the new [FILTER] to set. Pass `null` to remove the [FILTER].
     */
    public fun setCustomFilter(key: String, filter: Condition?) {
        if (filter == null) {
            customFilters.remove(key)
        } else {
            customFilters[key] = filter
        }
        updateFilter()
    }

    /**
     * Computes a [FILTER] from all currently registered [FILTER] components, but doesn't
     * set it into [currentFilter].
     */
    private fun computeFilter(): Condition {
        val filters: List<Condition> = bindings.keys.map { it.getFilter() }
        val customFilters: Collection<Condition> = customFilters.values
        return (filters + customFilters).toSet().reduce { acc, condition -> acc.and(condition) }
    }

    /**
     * Recomputes the most current [Condition] from all filter components. If the
     * new [Condition] differs from the current one, applies it to the [Grid].
     */
    private fun updateFilter() {
        val newFilter: Condition = computeFilter()
        if (newFilter != currentFilter) {
            applyFilterToGrid(newFilter)
            onFilterChanged()
            currentFilter = newFilter
        }
    }

    /**
     * Starts configuring [component] to act as a [FILTER] component for given [column]:
     * ```kotlin
     * personGrid = grid(dataProvider = Person.dataProvider) {
     *   flexGrow = 1.0
     *   appendHeaderRow()
     *   val filterBar: VokFilterBar<Person> = appendHeaderRow().asFilterBar(this)
     *
     *   addColumnFor(Person::name) {
     *     filterBar.forField(TextField(), this).ilike()
     *   }
     * }
     * ```
     */
    public fun <VALUE : Any> forField(component: HasValue<*, VALUE?>, column: Grid.Column<BEAN>): Binding.Builder<BEAN, VALUE> {
        require(!column.key.isNullOrBlank()) { "The column needs to have the property name as its key" }
        // start with a very simple getter which simply polls the component for its value.
        val fieldValueGetter: ()->VALUE? = {
            // useless cast to keep the Kotlin compiler happy
            @Suppress("UNCHECKED_CAST")
            (component as HasValue<HasValue.ValueChangeEvent<VALUE?>, VALUE?>).value
        }
        return Binding.Builder(this, column, component as Component, fieldValueGetter)
    }

    /**
     * The idea here is to gradually introduce converters on top of a Vaadin component, which
     * will ultimately convert the value into a Filter [F].
     *
     * After that's done, we can append the entire setup
     * into the [FilterBar]: [FilterBar] will now track changes to the filter component,
     * combine all filters into one and ultimately set it to the [com.vaadin.flow.data.provider.DataProvider].
     *
     * Call [forField] to start building a filter component for a grid column.
     * @param BEAN the bean type as provided by the [com.vaadin.flow.data.provider.DataProvider].
     * @param F the filter type which the [com.vaadin.flow.data.provider.DataProvider] uses.
     * For VoK it's usually [Filter].
     */
    public class Binding<BEAN : Any>(internal val builder: Builder<BEAN, Condition>) : Serializable {
        /**
         * Returns the current filter from the filter component.
         */
        public fun getFilter(): Condition = builder.valueGetter() ?: Condition.NO_CONDITION

        /**
         * Registers a [filterChangeListener], fired when the filter is changed.
         * @return the listener registration; use [Registration.remove] to remove the listener.
         */
        public fun addFilterChangeListener(filterChangeListener: () -> Unit) : Registration =
            (filterComponent as HasValue<*, *>).addValueChangeListener { filterChangeListener() }

        /**
         * Removes the filter component from the [FilterBar] and stops listening for value changes.
         */
        public fun unbind() {
            builder.filterBar.unregisterBinding(this)
        }

        /**
         * The filter component.
         */
        public val filterComponent: Component get() = builder.filterComponent

        /**
         * Clears the [filterComponent] value - e.g. sets the empty [String] value `""` to a [TextField].
         */
        public fun clearFilter() {
            (filterComponent as HasValue<*, *>).clear()
        }

        /**
         * Gradually builds the [Binding].
         * @param BEAN the type of bean present in the [Grid]
         * @param VALUE this [Binding] is able to extract a value of this type out of the [filterComponent].
         * @param filterComponent the Vaadin filter component, ultimately placed into the [Grid]'s [HeaderRow].
         * @param valueGetter retrieves the current value of type [VALUE] from the [filterComponent].
         */
        public class Builder<BEAN : Any, VALUE : Any>(
                internal val filterBar: FilterBar<BEAN>,
                public val column: Grid.Column<BEAN>,
                internal val filterComponent: Component,
                internal val valueGetter: () -> VALUE?) : Serializable {
            public val property: Property<Any> = Property.fromExternalString(column.key) as Property<Any>

            /**
             * Adds a converter to the chain which converts the value from [filterComponent]
             * (or rather from the previous [valueGetter]) to some kind of [NEWVALUE]. The converter
             * usually directly produces [TARGETFILTER] out of the current [VALUE], but in some cases
             * it's handy to have multiple intermediate converters.
             *
             * @param closure converts [VALUE] currently produced by [filterComponent] into
             * a [NEWVALUE]. Never receives nulls nor blank [String]s: nulls are automatically
             * short-circuited and converted to nulls of type [NEWVALUE]? . Blank [String]s are
             * automatically treated as nulls.
             * @param NEWVALUE the new value produced by the new [Builder] (which is now the head of the conversion chain).
             * @return the new converter chain head
             */
            public fun <NEWVALUE : Any> withConverter(closure: (VALUE) -> NEWVALUE?): Builder<BEAN, NEWVALUE> {
                val chainedValueGetter: () -> NEWVALUE? = {
                    val prevValue: VALUE? = valueGetter()
                    val isBlankString: Boolean = (prevValue as? String)?.isBlank() ?: false
                    if (prevValue == null || isBlankString) null else closure(prevValue)
                }
                return Builder(filterBar, column, filterComponent, chainedValueGetter)
            }

            /**
             * Finalizes the [Binding]. Creates a [TARGETFILTER] which passes rows with value
             * equal to the value selected in the [TARGETFILTER] component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            public fun eq(trim: Boolean = true): Binding<BEAN> {
                val self: Builder<BEAN, VALUE> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, Condition> = self
                        .withConverter { property.eq(it) }
                // now we can finalize the [Binding]
                return builder.bind()
            }

            /**
             * Finalizes the [Binding]. Creates a [Condition] which passes rows with value
             * less-than or equal to the value selected in the filter component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            public fun le(trim: Boolean = true): Binding<BEAN> {
                val self: Builder<BEAN, VALUE> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, Condition> = self
                        .withConverter { property.le(it) }
                // now we can finalize the binding
                return builder.bind()
            }

            /**
             * Finalizes the [Binding]. Creates a [TARGETFILTER] which passes rows with value
             * greater-than or equal to the value selected in the [TARGETFILTER] component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            public fun ge(trim: Boolean = true): Binding<BEAN> {
                val self: Builder<BEAN, VALUE> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, Condition> = self
                        .withConverter { property.ge(it) }
                // now we can finalize the binding
                return builder.bind()
            }

            /**
             * Adds a trimming converter to the chain: if the value is a String then it's trimmed,
             * otherwise the value is passed as-is.
             *
             * No need to use this with [istartsWith] or [fullText] since those filters will
             * always perform trimming automatically.
             */
            public fun trim(): Builder<BEAN, VALUE> =
                    withConverter {
                        @Suppress("UNCHECKED_CAST")
                        when (it) {
                            is String -> it.trim() as VALUE // safe cast since VALUE is String
                            else -> it
                        }
                    }
        }
    }

    /**
     * For internal purpose only.
     *
     * Finalizes given [binding]:
     * 1. Starts watching the filter component for changes and calls [updateFilter] on change.
     * 2. [configure]s the filter component
     * 3. Places the filter component into the [HeaderRow.HeaderCell].
     */
    internal fun finalizeBinding(binding: Binding<BEAN>) {
        val reg: Registration = binding.addFilterChangeListener { updateFilter() }
        bindings[binding] = reg
        updateFilter()
        configure(binding.builder.filterComponent)
        headerRow.getCell(binding.builder.column).component = binding.builder.filterComponent
    }

    private fun unregisterBinding(binding: Binding<BEAN>) {
        headerRow.getCell(binding.builder.column).component = null
        bindings.remove(binding)?.remove()
        updateFilter()
    }

    /**
     * Clears all [FILTER] components, which effectively removes any [FILTER]s.
     */
    public fun clear() {
        bindings.keys.forEach { it.clearFilter() }
    }

    /**
     * Returns all currently active [Binding]s.
     */
    public fun getBindings(): List<Binding<BEAN>> = bindings.keys.toList()

    /**
     * Removes all [FILTER] components from this [FilterBar] and stops listening for value changes.
     */
    public fun removeAllBindings() {
        bindings.keys.toList().forEach { it.unbind() }
    }

    /**
     * Return the [FILTER] [Binding] for given [Grid] [column]. Fails if no [FILTER]
     * [Binding] has been configured for given [column].
     */
    public fun getBindingFor(column: Grid.Column<BEAN>): Binding<BEAN> {
        val binding: Binding<BEAN>? = bindings.keys.firstOrNull { it.builder.column == column }
        checkNotNull(binding) { "No binding for column ${column.key}: $column" }
        return binding
    }

    public fun getBindingFor(property: KProperty1<BEAN, *>): Binding<BEAN> =
            getBindingFor(grid.getColumnBy(property))

    /**
     * Configures every Vaadin UI [FILTER] [field]. By default:
     * * the width is set to 100%
     * * the clear button is made visible for [HasClearButton], for example [TextField], [TextArea], [IntegerField],
     *   [BigDecimalField], [EmailField], [PasswordField], [ComboBox], [DatePicker], and [TimePicker].
     * * [HasValueChangeMode.setValueChangeMode] is set to [ValueChangeMode.LAZY]: not to bombard the database with [ValueChangeMode.EAGER], but
     *   also not to wait until the focus is lost from the [FILTER] - not a good UX since the user types in something and waits and waits and waits with nothing going on.
     */
    protected open fun configure(field: Component) {
        (field as? HasSize)?.setWidthFull()
        (field as? HasClearButton)?.isClearButtonVisible = true
        (field as? HasValueChangeMode)?.valueChangeMode = ValueChangeMode.LAZY
    }

    /**
     * Return the filtering component for given [column]. Fails if no [FILTER]
     * [Binding] has been configured for given [column].
     */
    public fun getFilterComponent(column: Grid.Column<BEAN>): Component = getBindingFor(column).filterComponent

    /**
     * Return the filtering component for given [property]. Fails if no [FILTER]
     * [Binding] has been configured for given [property].
     */
    public fun getFilterComponent(property: KProperty1<BEAN, *>): Component = getFilterComponent(grid.getColumnBy(property))
}

/**
 * A terminal operation which finalizes the [FilterBar.Binding] and makes the FilterBar
 * listen on value changes of the filter component.
 *
 * Rarely called directly: usually it's far easier to finalize
 * the [FilterBar.Binding] using helper filter-creating terminal functions:
 * [istartsWith], [FilterBar.Binding.Builder.le], [FilterBar.Binding.Builder.ge],
 * [FilterBar.Binding.Builder.eq], [onDay], [inRange], [fullText].
 *
 * You are only able to finalize the [FilterBar.Binding] after you manage to configure [FilterBar.Binding.Builder]
 * to convert the filter component value to [FILTER].
 * @param BEAN the type of beans displayed in the [Grid]
 * @param FILTER the type of the filtering value accepted by the [com.vaadin.flow.data.provider.DataProvider].
 * @return the finalized [FilterBar.Binding], with component properly configured and placed into
 * the [HeaderRow].
 * @see [FilterBar.finalizeBinding] for more details.
 */
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, Condition>.bind(): FilterBar.Binding<BEAN> {
    val binding: FilterBar.Binding<BEAN> = FilterBar.Binding(this)
    filterBar.finalizeBinding(binding)
    return binding
}

/**
 * Finalizes the [FilterBar.Binding] and compares [String] values using [com.gitlab.mvysny.jdbiorm.condition.LikeIgnoreCase].
 */
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, String>.istartsWith(): FilterBar.Binding<BEAN> {
    // first we need to have a converter, converting the component's value to a ILIKE filter
    val builder: FilterBar.Binding.Builder<BEAN, Condition> = withConverter { if (it.isBlank()) null else property.likeIgnoreCase("$it%") }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which matches [Number]s in given range.
 */
@JvmName("numberIntervalInRange")
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, NumberInterval<Double>>.inRange(): FilterBar.Binding<BEAN> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, Condition> = withConverter { it.contains(property) }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which matches dates and datetimes in given day.
 *
 * Note: [com.github.mvysny.kaributools.BrowserTimeZone] is used when comparing [LocalDate] with [Instant], [Date] and [Calendar] instances.
 * It's important for your app to initialize [com.github.mvysny.kaributools.BrowserTimeZone] properly as described in the kdoc of that variable.
 */
@JvmName("numberIntervalInRange")
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, LocalDate>.onDay(): FilterBar.Binding<BEAN> {
    // https://github.com/mvysny/vaadin-on-kotlin/issues/49
    // first convert the LocalDate value to a DateInterval which spans given day
    val builder: FilterBar.Binding.Builder<BEAN, DateInterval> = withConverter { DateInterval.of(it) }
    // now simply use inRange() to return a filter accepting instants in given day.
    return builder.inRange()
}

/**
 * Terminal operation which matches dates in given range.
 *
 * Note: [com.github.mvysny.kaributools.BrowserTimeZone] is used when comparing [LocalDate] with [Instant], [Date] and [Calendar] instances.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the [FILTER] component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
@JvmName("dateIntervalInRange2")
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, DateInterval>.inRange(): FilterBar.Binding<BEAN> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, Condition> = withConverter { it.contains(property, BrowserTimeZone.get) }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which performs full-text search.
 *
 * Please read the [FullTextFilter] documentation on how to properly prepare your database
 * for full-text search.
 */
public fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, String>.fullText(): FilterBar.Binding<BEAN> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, Condition> = withConverter { property.fullTextMatches(it) }
    // now we can finalize the binding
    return builder.bind()
}
