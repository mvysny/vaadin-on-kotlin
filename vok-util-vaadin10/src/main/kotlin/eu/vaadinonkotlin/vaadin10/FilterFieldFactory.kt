package eu.vaadinonkotlin.vaadin10

import eu.vaadinonkotlin.FilterFactory
import com.github.mvysny.karibudsl.v10.DateInterval
import com.github.mvysny.karibudsl.v10.DateRangePopup
import com.github.mvysny.vokdataloader.Filter
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.PropertyDefinition
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
import java.io.Serializable
import java.time.temporal.Temporal
import java.util.*

/**
 * Produces filter UI components for given bean property, and also
 * provides means to convert values from those UI components into actual filter values.
 *
 * You will usually use [VokDataProvider] which uses VoK-DataLoader [Filter] as filter type.
 *
 * See [FilterRow] for an example on how to populate Vaadin Grid with filters.
 *
 * @param T the type of beans handled by the Grid.
 * @param F the type of the filter accepted by the [com.vaadin.flow.data.provider.DataProvider]
 * You will usually use [VokDataProvider] which uses VoK-DataLoader [Filter] as filter type.
 * @author mvy
 */
interface FilterFieldFactory<T: Any, F> : Serializable {
    /**
     * Creates the filtering component for given bean property, or Grid column.
     * The component may not necessarily produce values of given data types - for example,
     * if the data type is a Double, the filtering component may produce a `NumberInterval<Double>`
     * object which mandates given value to be contained in a numeric range.
     *
     * [createFilter] is later used internally when the field's value changes, to construct a filter for given field.
     * @param property the [itemClass] property.
     * @param V the type of the value the property holds.
     * @return A field that can be assigned to the given fieldType and that is
     *         capable of filtering given type of data. May return null if filtering of given data type with given field type is unsupported.
     */
    fun <V> createField(property: PropertyDefinition<T, V?>): HasValue<*, V?>?

    /**
     * Creates a new filter of type [F] based on given value.
     * @param value the value, may be null.
     * @param filterField the filter field itself
     * @param property the property
     * @return a filter, may be null if no filtering is needed or if the value indicates that the filtering is disabled for this column.
     * The implementation may use [FilterFactory] to produce filters, or it may provide a completely custom solution.
     */
    fun <V> createFilter(value: V?, filterField: HasValue<*, V?>, property: PropertyDefinition<T, V?>): F?
}

/**
 * Provides default implementation for [FilterFieldFactory].
 * Supports filter fields for dates, numbers and strings.
 *
 * You can extend this class to add support for custom filters, but it's much better
 * to use the delegate pattern instead.
 * @param T the type of beans produced by the [com.vaadin.flow.data.provider.DataProvider]
 * @param F the type of the filter objects accepted by the [com.vaadin.flow.data.provider.DataProvider].
 * @param clazz the class of the beans produced by the [com.vaadin.flow.data.provider.DataProvider]
 * @param filterFactory allows filter components to produce filters accepted by the [com.vaadin.flow.data.provider.DataProvider].
 * @author mvy
 */
@Suppress("UNUSED_PARAMETER")
open class DefaultFilterFieldFactory<T: Any, F: Any>(clazz: Class<T>, val filterFactory: FilterFactory<F>) : FilterFieldFactory<T, F> {
    /**
     * If true, number filters will be shown as a popup, which allows the user to set eq, less-than and greater-than fields.
     * If false, a simple in-place editor will be shown, which only allows to enter the eq number.
     *
     * Default implementation always returns true.
     * @param property the bean property
     */
    protected open fun isUsePopupForNumericProperty(property: PropertyDefinition<T, *>): Boolean = true

    override fun <V> createField(property: PropertyDefinition<T, V?>): HasValue<*, V?>? {
        val type = property.type.nonPrimitive
        val field: HasValue<*, *>
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
            (this as? HasValueChangeMode)?.valueChangeMode = ValueChangeMode.ON_BLUR
        }
        @Suppress("UNCHECKED_CAST")
        return field as HasValue<*, V?>
    }

    protected open fun getEnumFilterDisplayName(property: PropertyDefinition<T, *>, constant: Enum<*>): String? = null

    protected open fun getEnumFilterIcon(property: PropertyDefinition<T, *>, constant: Enum<*>): Icon? = null

    private fun <V> createEnumField(type: Class<V?>, property: PropertyDefinition<T, V?>): HasValue<*, V?> = ComboBox<V?>().apply {
        setItems(*type.enumConstants)
        setItemLabelGenerator { item -> getEnumFilterDisplayName(property, item as Enum<*>) ?: item.name }
    }

    protected open fun createTextField(property: PropertyDefinition<T, *>): HasValue<*, *> = TextField()

    protected open fun createDateField(property: PropertyDefinition<T, *>): DateRangePopup = DateRangePopup()

    protected open fun createNumericField(type: Class<*>, property: PropertyDefinition<T, *>) = NumberFilterPopup()

    /**
     * Don't forget that the returned field must be tri-state - true, false, null (to disable filtering).
     */
    protected open fun createBooleanField(property: PropertyDefinition<T, Boolean?>): HasValue<*, Boolean?> = ComboBox<Boolean?>().apply {
        setItems(listOf(true, false))
        setItemLabelGenerator { item -> getBooleanFilterDisplayName(property, item!!) ?: item.toString() }
    }

    protected open fun getBooleanFilterDisplayName(property: PropertyDefinition<T, Boolean?>, value: Boolean): String? = null

    @Suppress("UNCHECKED_CAST")
    override fun <V> createFilter(value: V?, filterField: HasValue<*, V?>, property: PropertyDefinition<T, V?>): F? = when {
        value is NumberInterval<*> -> value.toFilter(property.name, filterFactory)
        value is DateInterval -> value.toFilter(property.name, filterFactory, property.type)
        value is String && value.isNotEmpty() -> generateGenericFilter<String>(filterField as HasValue<*, String?>, property as PropertyDefinition<T, String?>, value.trim())
        value is Enum<*> || value is Number || value is Boolean -> filterFactory.eq(property.name, value as Serializable)
        else -> null
    }

    protected open fun <V: Serializable> generateGenericFilter(field: HasValue<*, V?>, property: PropertyDefinition<T, V?>, value: V): F? {
        /* Special handling for ComboBox (= enum properties) */
        return if (field is ComboBox) {
            filterFactory.eq(property.name, value)
        } else {
            filterFactory.ilike(property.name, value.toString())
        }
    }
}
