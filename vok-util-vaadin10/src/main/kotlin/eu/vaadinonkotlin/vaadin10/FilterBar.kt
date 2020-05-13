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
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.HeaderRow
import com.vaadin.flow.component.textfield.TextField
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
 * Wraps a Grid [HeaderRow] as a filter bar, with additional functionality:
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
 * Wraps [HeaderRow] and helps you build filter components. Call [forField] to
 * configure filter for particular column, for example:
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
 * changes the value in the filter UI component, a new Grid filter of type [FILTER] is computed and
 * set to [Grid.getDataProvider].
 *
 * Every filter component is configured using the [configure] method.
 *
 * WARNING: WORK IN PROGRESS, THE API MAY CHANGE AT ANY TIME.
 * @param BEAN the type of items in the grid.
 * @param FILTER the type of the filters accepted by grid's [ConfigurableFilterDataProvider]. For VoK
 * it's usually [Filter].
 * @param grid the owner grid. It is expected that [Grid.getDataProvider] is of type [VokDataProvider]<BEAN>
 * (or [ConfigurableFilterDataProvider]<BEAN, FILTER, FILTER>).
 * @property headerRow the wrapped header row
 * @param filterFactory used to combine filter values when multiple filters are applied
 * (using the [FilterFactory.and] function).
 */
open class FilterBar<BEAN : Any, FILTER : Any>(
        val headerRow: HeaderRow,
        val grid: Grid<BEAN>,
        val filterFactory: FilterFactory<FILTER>
) : Serializable {

    private var currentFilter: FILTER? = null

    /**
     * Lists all currently registered bindings. The value is a registration of the
     * [Binding.addFilterChangeListener].
     */
    private val bindings: MutableMap<Binding<BEAN, FILTER>, Registration> = mutableMapOf()

    private fun applyFilterToGrid(filter: FILTER?) {
        @Suppress("UNCHECKED_CAST")
        (grid.dataProvider as ConfigurableFilterDataProvider<BEAN, FILTER, FILTER>).setFilter(filter)
    }

    /**
     * Computes a filter from all currently registered filter components, but doesn't
     * set it into [currentFilter].
     */
    private fun computeFilter(): FILTER? {
        val filters: List<FILTER> = bindings.keys.mapNotNull { it.getFilter() }
        return filterFactory.and(filters.toSet())
    }

    /**
     * Recomputes the most current filter from all filter components. If the
     * filter differs from the current one, applies it to the grid.
     */
    private fun updateFilter() {
        val newFilter: FILTER? = computeFilter()
        if (newFilter != currentFilter) {
            applyFilterToGrid(newFilter)
            currentFilter = newFilter
        }
    }

    /**
     * Starts configuring [component] to act as a filter component for given [column]:
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
            (component as HasValue<HasValue.ValueChangeEvent<VALUE?>, VALUE?>).value
        }
        return Binding.Builder(filterFactory, this, column, component as Component, fieldValueGetter)
    }

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
         * Removes the filter component from the filter bar and stops listening for value changes.
         */
        fun unbind() {
            builder.filterBar.unregisterBinding(this)
        }

        /**
         * The filter component.
         */
        val filterComponent: Component get() = builder.filterComponent

        /**
         * Clears the filter component - e.g. sets the empty string value `""` to a `TextField`.
         */
        fun clearFilter() {
            (filterComponent as HasValue<*, *>).clear()
        }

        /**
         * Gradually builds the filter binding.
         * @param BEAN the type of bean present in the Grid
         * @param VALUE this binding is able to extract a value of this type out of the [filterComponent].
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
             * @param closure never receives nulls nor blank strings: nulls are automatically
             * short-circuited and converted to nulls of type [NEWVALUE]. Blank strings are
             * automatically treated as nulls.
             * @param NEWVALUE the new value produced by the new builder (which is now the head of the conversion chain).
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
             * Finalizes the binding. Creates a filter which passes rows with value
             * equal to the value selected in the filter component.
             * @return the finalized binding.
             */
            fun eq(): Binding<BEAN, TARGETFILTER> = withConverter { filterFactory.eq(propertyName, it) } .bind()

            /**
             * Finalizes the binding. Creates a filter which passes rows with value
             * less-than or equal to the value selected in the filter component.
             * @return the finalized binding.
             */
            fun le(): Binding<BEAN, TARGETFILTER> = withConverter { filterFactory.le(propertyName, it) } .bind()

            /**
             * Finalizes the binding. Creates a filter which passes rows with value
             * greater-than or equal to the value selected in the filter component.
             * @return the finalized binding.
             */
            fun ge(): Binding<BEAN, TARGETFILTER> = withConverter { filterFactory.ge(propertyName, it) } .bind()
        }
    }

    /**
     * For internal purpose only.
     *
     * Finalizes given [binding]:
     * 1. Starts watching the filter component for changes and calls [updateFilter] on change.
     * 2. [configure]s the filter component
     * 3. Places the filter component into the header cell.
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
     * Clears all filter components, which effectively removes any filters.
     */
    fun clear() {
        bindings.keys.forEach { it.clearFilter() }
    }

    /**
     * Returns all currently active bindings.
     */
    fun getBindings(): List<Binding<BEAN, FILTER>> = bindings.keys.toList()

    /**
     * Removes all filter components from this filter bar and stops listening for value changes.
     */
    fun removeAllBindings() {
        bindings.keys.toList().forEach { it.unbind() }
    }

    /**
     * Return the filter [Binding] for given grid [column]. Fails if no filter
     * binding has been configured for given column.
     */
    fun getBindingFor(column: Grid.Column<BEAN>): Binding<BEAN, FILTER> {
        val binding: Binding<BEAN, FILTER>? = bindings.keys.firstOrNull { it.builder.column == column }
        checkNotNull(binding) { "No binding for column ${column.key}: $column" }
        return binding
    }

    fun getBindingFor(property: KProperty1<BEAN, *>): Binding<BEAN, FILTER> =
            getBindingFor(grid.getColumnBy(property))

    /**
     * Configures every Vaadin UI filter [field]. By default:
     * * the width is set to 100%
     * * the clear button is made visible for [TextField] and [ComboBox].
     * * [HasValueChangeMode.setValueChangeMode] is set to [ValueChangeMode.LAZY]: not to bombard the database with EAGER, but
     *   also not to wait until the focus is lost from the filter - not a good UX since the user types in something and waits and waits and waits with nothing going on.
     */
    open protected fun configure(field: Component) {
        (field as? HasSize)?.width = "100%"
        (field as? TextField)?.isClearButtonVisible = true
        (field as? ComboBox<*>)?.isClearButtonVisible = true
        (field as? HasValueChangeMode)?.valueChangeMode = ValueChangeMode.LAZY
    }

    /**
     * Return the filtering component for given [column]. Fails if no filter
     * binding has been configured for given column.
     */
    fun getFilterComponent(column: Grid.Column<BEAN>): Component = getBindingFor(column).filterComponent

    /**
     * Return the filtering component for given [property]. Fails if no filter
     * binding has been configured for given column.
     */
    fun getFilterComponent(property: KProperty1<BEAN, *>): Component = getFilterComponent(grid.getColumnBy(property))
}

/**
 * Finalizes the binding. A terminal operation; however, usually you wish to finalize
 * the binding using [ilike] or other terminal operator functions.
 * @param BEAN the type of beans displayed in the Grid
 * @param FILTER the type of the filtering value provided by the component,
 * e.g. [TextField] initially provides [String]s.
 * @return the finalized binding, with component properly configured and placed into
 * the header bar. See [FilterBar.finalizeBinding] for more details.
 */
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, FILTER, FILTER>.bind(): FilterBar.Binding<BEAN, FILTER> {
    val binding: FilterBar.Binding<BEAN, FILTER> = FilterBar.Binding(this)
    filterBar.finalizeBinding(binding)
    return binding
}

/**
 * Finalizes the binding and compares string values using the ILIKE operator.
 */
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, String, FILTER>.ilike(): FilterBar.Binding<BEAN, FILTER> =
        withConverter { if (it.isBlank()) null else filterFactory.ilike(propertyName, it) }
                .bind()

/**
 * Terminal operation which matches numbers in given range.
 */
@JvmName("numberIntervalInRange")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, NumberInterval<Double>, FILTER>.inRange(): FilterBar.Binding<BEAN, FILTER> =
        withConverter { it.toFilter(propertyName, filterFactory) }
                .bind()

/**
 * Terminal operation which matches dates in given range.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the filter component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
@JvmName("dateIntervalInRange")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, DateInterval, FILTER>.inRange(fieldType: KClass<*>): FilterBar.Binding<BEAN, FILTER> =
        inRange(fieldType.java)

/**
 * Terminal operation which matches dates in given range.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the filter component to a value
 * comparable with values coming from the underlying property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
@JvmName("dateIntervalInRange2")
fun <BEAN : Any, FILTER: Any> FilterBar.Binding.Builder<BEAN, DateInterval, FILTER>.inRange(fieldType: Class<*>): FilterBar.Binding<BEAN, FILTER> =
        withConverter { it.toFilter(propertyName, filterFactory, fieldType) }
                .bind()

/**
 * Terminal operation which matches dates in given range.
 * @param property the type of the property is used to convert [LocalDate] `from`/`to` values of the [DateInterval] coming from the filter component to a value
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
fun <BEAN : Any> FilterBar.Binding.Builder<BEAN, String, Filter<BEAN>>.fullText(): FilterBar.Binding<BEAN, Filter<BEAN>> =
        withConverter<Filter<BEAN>> { if (it.isBlank()) null else FullTextFilter<BEAN>(propertyName, it) }
                .bind()
