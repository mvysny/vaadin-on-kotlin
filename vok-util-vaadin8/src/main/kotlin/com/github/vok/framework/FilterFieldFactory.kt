package com.github.vok.framework

import com.github.vok.karibudsl.fillParent
import com.github.vok.karibudsl.w
import com.vaadin.data.BeanPropertySet
import com.vaadin.data.HasValue
import com.vaadin.data.PropertyDefinition
import com.vaadin.data.PropertySet
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.server.Resource
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.shared.ui.datefield.DateTimeResolution
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.Temporal
import java.util.*

/**
 * Produces filter fields and binds them to the [dataProvider], to automatically perform the filtering when the field is changed.
 *
 * Currently, the filter fields have to be attached to the Grid manually: you will have to create a special HeaderRow in a Grid, create a field for each column,
 * add the field to the HeaderRow, and finally, [bind] the field to the container. The [generateFilterComponents] can do that for you (it's located in other module), just call
 * ```
 * grid.appendHeaderRow().generateFilterComponents(grid)
 * ```
 *
 * Currently, only Vaadin Grid component is supported. Vaadin does not support attaching of the filter fields to a Vaadin Table.
 * Attaching filters to a Tepi FilteringTable
 * is currently not supported directly, but it may be done manually.
 * @property itemClass The bean on which the filtering will be performed, not null.
 * @property filterFactory produces the actual filter objects accepted by the [dataProvider].
 * @param T the type of beans handled by the Grid.
 * @param F the type of the filter accepted by the [dataProvider]
 * @author mvy, stolen from Teppo Kurki's FilterTable.
 */
abstract class FilterFieldFactory<T: Any, F>(protected val itemClass: Class<T>,
                                             val dataProvider: ConfigurableFilterDataProvider<T, F?, F?>,
                                             val filterFactory: FilterFactory<F>
) {
    /**
     * Current set of filters. Every filter component adds or removes stuff from this set on value change.
     */
    internal val filters = mutableSetOf<F>()

    protected val properties: PropertySet<T> = BeanPropertySet.get(itemClass)

    /**
     * Creates the filtering component for given bean property, or Grid column.
     * The component may not necessarily produce values of given data types - for example,
     * if the data type is a Double, the filtering component may produce a `NumberInterval<Double>`
     * object which mandates given value to be contained in a numeric range.
     *
     * [createFilter] is later used internally when the field's value changes, to construct a filter for given field.
     * @param property the [itemClass] property.
     * @return A field that can be assigned to the given fieldType and that is
     * *         capable of filtering given type of data. May return null if filtering of given data type with given field type is unsupported.
     */
    abstract fun <V> createField(property: PropertyDefinition<T, V?>): HasValue<V?>?

    protected fun getProperty(name: String): PropertyDefinition<T, *> =
            properties.getProperty(name).orElse(null) ?: throw IllegalArgumentException("$itemClass has no property $name; available properties: ${properties.properties.map { it.name }}")

    /**
     * Creates a new Container Filter based on given value.
     * @param value the value, may be null.
     * @param filterField the filter field itself
     * @param property the property
     * @return a filter, may be null if no filtering is needed or if the value indicates that the filtering is disabled for this column.
     */
    protected abstract fun <V> createFilter(value: V?, filterField: HasValue<V?>, property: PropertyDefinition<T, V?>): F?

    /**
     * Binds given filtering field to a container - starts filtering based on the contents of the field, and starts watching for field value changes.
     * @param field The field which provides the filtering values, not null. [createFilter] is used to convert
     * the field's value to a filter.
     * @param property The bean property on which the filtering will be performed, not null.
     */
    fun <V> bind(field: HasValue<V?>, property: PropertyDefinition<T, V?>) {
        val filterFieldWatcher = FilterFieldWatcher(field, property)
        field.addValueChangeListener(filterFieldWatcher)
    }

    /**
     * Listens on value change on given field and updates [ConfigurableFilterDataProvider.setFilter] accordingly.
     * @property field The field which provides the filtering values.
     * @property property The bean property on which the filtering will be performed.
     * @param V the value type
     */
    private inner class FilterFieldWatcher<V>(private val field: HasValue<V?>, private val property: PropertyDefinition<T, V?>) :
        HasValue.ValueChangeListener<V?> {

        /**
         * The current container filter, may be null if no filtering is currently needed because the
         * field's value indicates that the filtering is disabled for this column (e.g. the text filter is blank, the filter field is cleared, etc).
         */
        private var currentFilter: F? = null

        init {
            valueChange()
        }

        override fun valueChange(event: HasValue.ValueChangeEvent<V?>) {
            valueChange()
        }

        private fun valueChange(value: V? = field.value) {
            val newFilter = createFilter(value, field, property)
            if (newFilter != currentFilter) {
                if (currentFilter != null) {
                    filters.remove(currentFilter!!)
                    currentFilter = null
                }
                if (newFilter != null) {
                    currentFilter = newFilter
                    filters.add(newFilter)
                }
                dataProvider.setFilter(filterFactory.and(filters))
            }
        }
    }
}

/**
 * Provides default implementation for [FilterFieldFactory].
 * Supports filter fields for dates, numbers and strings.
 * @param T the type of beans produced by the [dataProvider]
 * @param F the type of the filter objects accepted by the [dataProvider].
 * @param clazz the class of the beans produced by the [dataProvider]
 * @param dataProvider provides bean instances.
 * @param filterFactory allows filter components to produce filters accepted by the [dataProvider].
 * @author mvy, stolen from Teppo Kurki's FilterTable.
 */
@Suppress("UNUSED_PARAMETER")
open class DefaultFilterFieldFactory<T: Any, F: Any>(clazz: Class<T>, dataProvider: ConfigurableFilterDataProvider<T, F?, F?>, filterFactory: FilterFactory<F>) : FilterFieldFactory<T, F>(clazz, dataProvider, filterFactory) {
    /**
     * If true, number filters will be shown as a popup, which allows the user to set eq, less-than and greater-than fields.
     * If false, a simple in-place editor will be shown, which only allows to enter the eq number.
     *
     * Default implementation always returns true.
     * @param property the bean property
     */
    protected fun isUsePopupForNumericProperty(property: PropertyDefinition<T, *>): Boolean = true

    override fun <V> createField(property: PropertyDefinition<T, V?>): HasValue<V?>? {
        val type = property.type.nonPrimitive
        val field: HasValue<*>
        if (type == java.lang.Boolean::class.java) {
            @Suppress("UNCHECKED_CAST")
            field = createBooleanField(property as PropertyDefinition<T, Boolean?>)
        } else if (type.isEnum) {
            field = createEnumField(type, property)
        } else if (Date::class.java.isAssignableFrom(type) || Temporal::class.java.isAssignableFrom(type)) {
            field = createDateField(property)
        } else if (Number::class.java.isAssignableFrom(type) && isUsePopupForNumericProperty(property)) {
            field = createNumericField(type, property)
        } else {
            field = createTextField(property)
        }
        field.apply {
            (this as Component).w = fillParent; (this as? HasValueChangeMode)?.valueChangeMode = ValueChangeMode.LAZY
        }
        @Suppress("UNCHECKED_CAST")
        return field as HasValue<V?>
    }

    protected fun getEnumFilterDisplayName(property: PropertyDefinition<T, *>, constant: Enum<*>): String? = null

    protected fun getEnumFilterIcon(property: PropertyDefinition<T, *>, constant: Enum<*>): Resource? = null

    private fun <V> createEnumField(type: Class<V?>, property: PropertyDefinition<T, V?>): HasValue<V?> = ComboBox<V?>().apply {
        setItems(*type.enumConstants)
        itemCaptionGenerator = ItemCaptionGenerator { e -> getEnumFilterDisplayName(property, e as Enum<*>) ?: e.name }
        addStyleName(ValoTheme.COMBOBOX_TINY)
    }

    protected fun createTextField(property: PropertyDefinition<T, *>): HasValue<*> = TextField().apply {
        addStyleName(ValoTheme.TEXTFIELD_TINY)
    }

    protected fun createDateField(property: PropertyDefinition<T, *>): DateFilterPopup {
        val popup = DateFilterPopup()
        if (property.type == LocalDate::class.java) {
            popup.resolution = DateTimeResolution.DAY
        }
        return popup
    }

    protected fun createNumericField(type: Class<*>, property: PropertyDefinition<T, *>) = NumberFilterPopup()

    /**
     * Don't forget that the returned field must be tri-state - true, false, null (to disable filtering).
     */
    protected fun createBooleanField(property: PropertyDefinition<T, Boolean?>): HasValue<Boolean?> = ComboBox<Boolean?>().apply {
        setItems(listOf(true, false))
        itemCaptionGenerator = ItemCaptionGenerator { b -> getBooleanFilterDisplayName(property, b!!) ?: b.toString() }
        itemIconGenerator = IconGenerator { b -> getBooleanFilterIcon(property, b!!) }
        addStyleName(ValoTheme.COMBOBOX_TINY)
    }

    protected fun getBooleanFilterIcon(property: PropertyDefinition<T, Boolean?>, value: Boolean): Resource? = null

    protected fun getBooleanFilterDisplayName(property: PropertyDefinition<T, Boolean?>, value: Boolean): String? = null

    @Suppress("UNCHECKED_CAST")
    override fun <V> createFilter(value: V?, filterField: HasValue<V?>, property: PropertyDefinition<T, V?>): F? = when {
        value is NumberInterval<*> -> value.toFilter(property.name, filterFactory)
        value is DateInterval -> value.toFilter(property.name, filterFactory, property.type)
        value is String && !value.isEmpty() -> generateGenericFilter<String>(filterField as HasValue<String?>, property as PropertyDefinition<T, String?>, value.trim())
        value is Enum<*> || value is Number || value is Boolean -> filterFactory.eq(property.name, value as Serializable)
        else -> null
    }

    protected fun <V: Serializable> generateGenericFilter(field: HasValue<V?>, property: PropertyDefinition<T, V?>, value: V): F? {
        /* Special handling for ComboBox (= enum properties) */
        if (field is ComboBox) {
            return filterFactory.eq(property.name, value)
        } else {
            return filterFactory.ilike(property.name, value.toString())
        }
    }
}
