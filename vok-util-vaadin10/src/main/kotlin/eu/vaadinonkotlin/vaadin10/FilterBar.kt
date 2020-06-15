package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.karibudsl.v10.DateInterval
import com.github.mvysny.karibudsl.v10.NumberInterval
import com.github.mvysny.karibudsl.v10.getColumnBy
import com.github.mvysny.vokdataloader.Filter
import com.github.mvysny.vokdataloader.FullTextFilter
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasSize
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.HeaderRow
import com.vaadin.flow.component.textfield.*
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
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
 * Wraps a [Grid] [HeaderRow] as a [FilterBar], with additional functionality:
 * ```kotlin
 * val filterBar = grid.appendHeaderRow().asFilterBar(grid, DataLoaderFilterFactory<Person>())
 * ```
 */
fun <T : Any, F : Any> HeaderRow.asFilterBar(grid: Grid<T>, filterFactory: FilterFactory<F>): FilterBar<T, F> =
        FilterBar(this, grid, filterFactory)

typealias VokFilterBar<T> = FilterBar<T, Filter<T>>

/**
 * Creates a [FilterBar] which produces VOK [Filter]s. Perfect for using with the
 * [VokDataProvider].
 * ```kotlin
 * val filterBar = grid.appendHeaderRow().asFilterBar(grid)
 * ```
 * @param T the bean type present in the [grid]
 * @param grid the owner grid
 */
fun <T : Any> HeaderRow.asFilterBar(grid: Grid<T>): VokFilterBar<T> =
        asFilterBar(grid, DataLoaderFilterFactory<T>())

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
 * changes the value in the [FILTER] UI component, a new [Grid] filter of type [FILTER] is computed and
 * set to [Grid.getDataProvider].
 *
 * Every [FILTER] component is configured using the [configure] method.
 *
 * You can also call [setCustomFilter] to set or remove completely custom [FILTER]s which will
 * be ANDed with component-bound [FILTER]s. You can use this functionality to e.g.
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
open class FilterBar<BEAN : Any, FILTER : Any>(
        val headerRow: HeaderRow,
        val grid: Grid<BEAN>,
        val filterFactory: FilterFactory<FILTER>
) : Serializable {

    /**
     * The current [FILTER]. Defaults to null.
     */
    var currentFilter: FILTER? = null
        private set

    /**
     * Lists all currently registered [Binding]s. The value is a [Registration] of the
     * [Binding.addFilterChangeListener].
     */
    private val bindings: MutableMap<Binding<BEAN, FILTER>, Registration> = mutableMapOf()

    /**
     * Contains a set of custom [FILTER]s. See [setCustomFilter] for more details.
     */
    private val customFilters: MutableMap<String, FILTER> = mutableMapOf()

    private fun applyFilterToGrid(filter: FILTER?) {
        @Suppress("UNCHECKED_CAST")
        (grid.dataProvider as ConfigurableFilterDataProvider<BEAN, FILTER, FILTER>).setFilter(filter)
    }

    /**
     * Invoked when a [FILTER] changes. By default does nothing. Read [currentFilter]
     * to obtain the current [FILTER].
     *
     * Invoked after the [FILTER] has already been set to [Grid]'s [com.vaadin.flow.data.provider.DataProvider].
     */
    var onFilterChanged: () -> Unit = {}

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
    fun setCustomFilter(key: String, filter: FILTER?) {
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
    private fun computeFilter(): FILTER? {
        val filters: List<FILTER> = bindings.keys.mapNotNull { it.getFilter() }
        val customFilters: Collection<FILTER> = customFilters.values
        return filterFactory.and((filters + customFilters).toSet())
    }

    /**
     * Recomputes the most current [FILTER] from all [FILTER] components. If the
     * [FILTER] differs from the current one, applies it to the [Grid].
     */
    private fun updateFilter() {
        val newFilter: FILTER? = computeFilter()
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
    fun <VALUE : Any> forField(component: HasValue<*, VALUE?>, column: Grid.Column<BEAN>): Binding.Builder<BEAN, VALUE, FILTER> {
        require(!column.key.isNullOrBlank()) { "The column needs to have the property name as its key" }
        // start with a very simple getter which simply polls the component for its value.
        val fieldValueGetter: ()->VALUE? = {
            // useless cast to keep the Kotlin compiler happy
            @Suppress("UNCHECKED_CAST")
            (component as HasValue<HasValue.ValueChangeEvent<VALUE?>, VALUE?>).value
        }
        return Binding.Builder(filterFactory, this, column, component as Component, fieldValueGetter)
    }

    /**
     * The idea here is to gradually introduce converters on top of a Vaadin component, which
     * will ultimately convert the value into a Filter [F].
     *
     * After that's done, we can append the entire setup
     * into the [FilterBar]: [FilterBar] will now track changes to the filter component,
     * combine all filters into one and ultimately set it to the [com.vaadin.flow.data.provider.DataProvider].
     * @param BEAN the bean type as provided by the [com.vaadin.flow.data.provider.DataProvider].
     * @param F the filter type which the [com.vaadin.flow.data.provider.DataProvider] uses.
     */
    class Binding<BEAN : Any, F : Any>(internal val builder: Builder<BEAN, F, F>) : Serializable {
        /**
         * Returns the current filter from the filter component.
         */
        fun getFilter(): F? = builder.valueGetter()

        /**
         * Registers a [filterChangeListener], fired when the filter is changed.
         * @return the listener registration; use [Registration.remove] to remove the listener.
         */
        fun addFilterChangeListener(filterChangeListener: () -> Unit) : Registration =
            (filterComponent as HasValue<*, *>).addValueChangeListener { filterChangeListener() }

        /**
         * Removes the filter component from the [FilterBar] and stops listening for value changes.
         */
        fun unbind() {
            builder.filterBar.unregisterBinding(this)
        }

        /**
         * The filter component.
         */
        val filterComponent: Component get() = builder.filterComponent

        /**
         * Clears the [filterComponent] value - e.g. sets the empty [String] value `""` to a [TextField].
         */
        fun clearFilter() {
            (filterComponent as HasValue<*, *>).clear()
        }

        /**
         * Gradually builds the [TARGETFILTER] [Binding].
         * @param BEAN the type of bean present in the [Grid]
         * @param VALUE this [Binding] is able to extract a value of this type out of the [filterComponent].
         * @param TARGETFILTER we ultimately aim to get the filter of this type out of the [filterComponent].
         * For VoK it's usually [Filter].
         * @param filterFactory creates filters of type [TARGETFILTER]
         * @param filterComponent the Vaadin filter component, ultimately placed into the [Grid]'s [HeaderRow].
         * @param valueGetter retrieves the current value of type [VALUE] from the [filterComponent].
         */
        class Builder<BEAN : Any, VALUE : Any, TARGETFILTER : Any>(
                val filterFactory: FilterFactory<TARGETFILTER>,
                internal val filterBar: FilterBar<BEAN, TARGETFILTER>,
                val column: Grid.Column<BEAN>,
                internal val filterComponent: Component,
                internal val valueGetter: () -> VALUE?) : Serializable {
            val propertyName: String get() = requireNotNull(column.key) { "The column needs to have the property name as its key" }

            /**
             * Adds a converter to the chain which converts the value from [filterComponent]
             * (or rather from the previous [valueGetter]).
             * @param closure never receives nulls nor blank [String]s: nulls are automatically
             * short-circuited and converted to nulls of type [NEWVALUE]. Blank [String]s are
             * automatically treated as nulls.
             * @param NEWVALUE the new value produced by the new [Builder] (which is now the head of the conversion chain).
             * @return the new converter chain head
             */
            fun <NEWVALUE : Any> withConverter(closure: (VALUE) -> NEWVALUE?): Builder<BEAN, NEWVALUE, TARGETFILTER> {
                val chainedValueGetter: () -> NEWVALUE? = {
                    val prevValue: VALUE? = valueGetter()
                    val isBlankString: Boolean = (prevValue as? String)?.isBlank() ?: false
                    if (prevValue == null || isBlankString) null else closure(prevValue)
                }
                return Builder(filterFactory, filterBar, column, filterComponent, chainedValueGetter)
            }

            /**
             * Finalizes the [Binding]. Creates a [TARGETFILTER] which passes rows with value
             * equal to the value selected in the [TARGETFILTER] component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            fun eq(trim: Boolean = true): Binding<BEAN, TARGETFILTER> {
                val self: Builder<BEAN, VALUE, TARGETFILTER> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, TARGETFILTER, TARGETFILTER> = self.withConverter { filterFactory.eq(propertyName, it) }
                // now we can finalize the [Binding]
                return builder.bind()
            }

            /**
             * Finalizes the [Binding]. Creates a [TARGETFILTER] which passes rows with value
             * less-than or equal to the value selected in the [TARGETFILTER] component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            fun le(trim: Boolean = true): Binding<BEAN, TARGETFILTER> {
                val self: Builder<BEAN, VALUE, TARGETFILTER> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, TARGETFILTER, TARGETFILTER> = self.withConverter { filterFactory.le(propertyName, it) }
                // now we can finalize the binding
                return builder.bind()
            }

            /**
             * Finalizes the [Binding]. Creates a [TARGETFILTER] which passes rows with value
             * greater-than or equal to the value selected in the [TARGETFILTER] component.
             * @param trim if true (default) then strings are automatically trimmed.
             * @return the finalized [Binding].
             */
            fun ge(trim: Boolean = true): Binding<BEAN, TARGETFILTER> {
                val self: Builder<BEAN, VALUE, TARGETFILTER> = if (trim) trim() else this
                // first we need to have a converter, converting the component's value to a filter
                val builder: Builder<BEAN, TARGETFILTER, TARGETFILTER> = self.withConverter { filterFactory.ge(propertyName, it) }
                // now we can finalize the binding
                return builder.bind()
            }

            /**
             * Adds a trimming converter to the chain: if the value is a String then it's trimmed,
             * otherwise the value is passed as-is.
             *
             * No need to use this with [ilike] or [fullText] since those filters will
             * always perform trimming automatically.
             */
            fun trim(): Builder<BEAN, VALUE, TARGETFILTER> =
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
     * 1. Starts watching the [FILTER] component for changes and calls [updateFilter] on change.
     * 2. [configure]s the [FILTER] component
     * 3. Places the [FILTER] component into the [HeaderRow.HeaderCell].
     */
    internal fun finalizeBinding(binding: Binding<BEAN, FILTER>) {
        val reg: Registration = binding.addFilterChangeListener { updateFilter() }
        bindings[binding] = reg
        updateFilter()
        configure(binding.builder.filterComponent)
        headerRow.getCell(binding.builder.column).setComponent(binding.builder.filterComponent)
    }

    private fun unregisterBinding(binding: Binding<BEAN, FILTER>) {
        headerRow.getCell(binding.builder.column).setComponent(null)
        bindings.remove(binding)?.remove()
        updateFilter()
    }

    /**
     * Clears all [FILTER] components, which effectively removes any [FILTER]s.
     */
    fun clear() {
        bindings.keys.forEach { it.clearFilter() }
    }

    /**
     * Returns all currently active [Binding]s.
     */
    fun getBindings(): List<Binding<BEAN, FILTER>> = bindings.keys.toList()

    /**
     * Removes all [FILTER] components from this [FilterBar] and stops listening for value changes.
     */
    fun removeAllBindings() {
        bindings.keys.toList().forEach { it.unbind() }
    }

    /**
     * Return the [FILTER] [Binding] for given [Grid] [column]. Fails if no [FILTER]
     * [Binding] has been configured for given [column].
     */
    fun getBindingFor(column: Grid.Column<BEAN>): Binding<BEAN, FILTER> {
        val binding: Binding<BEAN, FILTER>? = bindings.keys.firstOrNull { it.builder.column == column }
        checkNotNull(binding) { "No binding for column ${column.key}: $column" }
        return binding
    }

    fun getBindingFor(property: KProperty1<BEAN, *>): Binding<BEAN, FILTER> =
            getBindingFor(grid.getColumnBy(property))

    /**
     * Configures every Vaadin UI [FILTER] [field]. By default:
     * * the width is set to 100%
     * * the clear button is made visible for [TextField], [TextArea], [IntegerField],
     *   [BigDecimalField], [EmailField], [PasswordField], [ComboBox], [DatePicker], and [TimePicker].
     * * [HasValueChangeMode.setValueChangeMode] is set to [ValueChangeMode.LAZY]: not to bombard the database with [ValueChangeMode.EAGER], but
     *   also not to wait until the focus is lost from the [FILTER] - not a good UX since the user types in something and waits and waits and waits with nothing going on.
     */
    protected open fun configure(field: Component) {
        (field as? HasSize)?.width = "100%"
        // lots of repetition. https://github.com/vaadin/flow/issues/8443 filed
        (field as? TextField)?.isClearButtonVisible = true
        (field as? TextArea)?.isClearButtonVisible = true
        (field as? IntegerField)?.isClearButtonVisible = true
        (field as? BigDecimalField)?.isClearButtonVisible = true
        (field as? EmailField)?.isClearButtonVisible = true
        (field as? PasswordField)?.isClearButtonVisible = true // okay this case is not really that useful :-D
        (field as? ComboBox<*>)?.isClearButtonVisible = true
        (field as? DatePicker)?.isClearButtonVisible = true
        (field as? TimePicker)?.isClearButtonVisible = true
        (field as? HasValueChangeMode)?.valueChangeMode = ValueChangeMode.LAZY
    }

    /**
     * Return the filtering component for given [column]. Fails if no [FILTER]
     * [Binding] has been configured for given [column].
     */
    fun getFilterComponent(column: Grid.Column<BEAN>): Component = getBindingFor(column).filterComponent

    /**
     * Return the filtering component for given [property]. Fails if no [FILTER]
     * [Binding] has been configured for given [property].
     */
    fun getFilterComponent(property: KProperty1<BEAN, *>): Component = getFilterComponent(grid.getColumnBy(property))
}

/**
 * Finalizes the [FilterBar.Binding]. A terminal operation; however, usually you wish to finalize
 * the [FilterBar.Binding] using [ilike] or other terminal operator functions.
 *
 * You are able to finalize the [FilterBar.Binding] only after you managed to configure [FilterBar.Binding.Builder]
 * to convert the field value to [FILTER].
 * @param BEAN the type of beans displayed in the [Grid]
 * @param FILTER the type of the filtering value accepted by the [com.vaadin.flow.data.provider.DataProvider].
 * @return the finalized [FilterBar.Binding], with component properly configured and placed into
 * the [HeaderRow].
 * @see [FilterBar.finalizeBinding] for more details.
 */
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, FILTER, FILTER>.bind(): FilterBar.Binding<BEAN, FILTER> {
    val binding: FilterBar.Binding<BEAN, FILTER> = FilterBar.Binding(this)
    filterBar.finalizeBinding(binding)
    return binding
}

/**
 * Finalizes the [FilterBar.Binding] and compares [String] values using the ILIKE operator.
 */
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, String, FILTER>.ilike(): FilterBar.Binding<BEAN, FILTER> {
    // first we need to have a converter, converting the component's value to a ILIKE filter
    val builder: FilterBar.Binding.Builder<BEAN, FILTER, FILTER> = withConverter { if (it.isBlank()) null else filterFactory.ilike(propertyName, it) }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which matches [Number]s in given range.
 */
@JvmName("numberIntervalInRange")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, NumberInterval<Double>, FILTER>.inRange(): FilterBar.Binding<BEAN, FILTER> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, FILTER, FILTER> = withConverter { it.toFilter(propertyName, filterFactory) }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which matches dates in given range.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the [FILTER] component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
@JvmName("dateIntervalInRange")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, DateInterval, FILTER>.inRange(fieldType: KClass<*>): FilterBar.Binding<BEAN, FILTER> =
        inRange(fieldType.java)

/**
 * Terminal operation which matches dates in given range.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the [FILTER] component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
@JvmName("dateIntervalInRange2")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, DateInterval, FILTER>.inRange(fieldType: Class<*>): FilterBar.Binding<BEAN, FILTER> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, FILTER, FILTER> = withConverter { it.toFilter(propertyName, filterFactory, fieldType) }
    // now we can finalize the binding
    return builder.bind()
}

/**
 * Terminal operation which matches dates in given range.
 * @param property the type of the property is used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the [FILTER] component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
inline fun <BEAN : Any, FILTER: Any, reified V> FilterBar.Binding.Builder<BEAN, DateInterval, FILTER>.inRange(property: KProperty1<BEAN, V>): FilterBar.Binding<BEAN, FILTER> =
        inRange(V::class)

/**
 * Terminal operation which performs full-text search.
 *
 * Please read the [FullTextFilter] documentation on how to properly prepare your database
 * for full-text search.
 */
fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, String, Filter<BEAN>>.fullText(): FilterBar.Binding<BEAN, Filter<BEAN>> {
    // first we need to have a converter, converting the component's value to a range filter
    val builder: FilterBar.Binding.Builder<BEAN, Filter<BEAN>, Filter<BEAN>> = withConverter<Filter<BEAN>> { if (it.isBlank()) null else FullTextFilter<BEAN>(propertyName, it) }
    // now we can finalize the binding
    return builder.bind()
}
