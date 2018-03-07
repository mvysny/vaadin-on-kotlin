package com.github.vok.framework

import com.github.vok.karibudsl.*
import com.vaadin.data.*
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.server.Page
import com.vaadin.server.Resource
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.shared.ui.datefield.DateTimeResolution
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.util.*

/**
 * Used by filter components (such as [NumberInterval]) to create actual filter objects. The filters are expected to have properly
 * implemented [Any.equals], [Any.hashCode] and [Any.toString], to allow for filter expression simplification (e.g. to remove non-unique
 * filters from AND or OR expressions).
 *
 * The filter objects produced by this factory will be passed into the [com.vaadin.data.provider.DataProvider] in the [com.vaadin.data.provider.Query] object.
 *
 * @param F the type of the filter objects. Every database access type may have different filters: for example VoK-ORM
 */
interface FilterFactory<F> : Serializable {
    /**
     * ANDs given set of filters and returns a new filter.
     * @param filters set of filters, may be empty.
     * @return a filter which ANDs given filter set; returns `null` when the filter set is empty.
     */
    fun and(filters: Set<F>): F?
    /**
     * ORs given set of filters and returns a new filter.
     * @param filters set of filters, may be empty.
     * @return a filter which ORs given filter set; returns `null` when the filter set is empty.
     */
    fun or(filters: Set<F>): F?
    /**
     * Creates a filter which matches the value of given [propertyName] to given [value].
     */
    fun eq(propertyName: String, value: Any): F
    /**
     * Creates a filter which accepts only such values of given [propertyName] which are less than or equal to given [value].
     */
    fun le(propertyName: String, value: Any): F
    /**
     * Creates a filter which accepts only such values of given [propertyName] which are greater than or equal to given [value].
     */
    fun ge(propertyName: String, value: Any): F
    /**
     * Creates a filter which performs a case-insensitive substring matching of given [propertyName] to given [value].
     * @param value not empty; matching rows must contain this string. To emit SQL LIKE statement you need to prepend and append '%' to this string.
     */
    fun ilike(propertyName: String, value: String): F

    /**
     * Creates a filter which accepts only such values of given [propertyName] which are greater than or equal to given [min] and less than or equal to given [max].
     */
    fun between(propertyName: String, min: Any, max: Any): F = when {
        min == max -> eq(propertyName, min)
        else -> and(setOf(ge(propertyName, min), le(propertyName, max)))!!
    }
}

/**
 * A potentially open numeric range. If both [min] and [max] are `null`, then the interval accepts any number.
 * @property max the maximum accepted value, inclusive. If `null` then the numeric range has no upper limit.
 * @property min the minimum accepted value, inclusive. If `null` then the numeric range has no lower limit.
 */
data class NumberInterval<T : Number>(var max: T?, var min: T?) : Serializable {

    /**
     * Creates a filter out of this interval, using given [filterFactory].
     * @return a filter which matches the same set of numbers as this interval. Returns `null` for universal set interval.
     */
    fun <F> toFilter(propertyName: String, filterFactory: FilterFactory<F>): F? {
        if (isSingleItem) return filterFactory.eq(propertyName, max!!)
        if (max != null && min != null) {
            return filterFactory.between(propertyName, min!!, max!!)
        }
        if (max != null) return filterFactory.le(propertyName, max!!)
        if (min != null) return filterFactory.ge(propertyName, min!!)
        return null
    }

    /**
     * True if the interval consists of single number only.
     */
    val isSingleItem: Boolean
        get() = max != null && min != null && max == min

    /**
     * True if the interval includes all possible numbers (both [min] and [max] are `null`).
     */
    val isUniversalSet: Boolean
        get() = max == null && min == null
}

/**
 * A filter component which allows the user to filter numeric value which is greater than, equal, or less than a value which user enters.
 * Stolen from Teppo Kurki's FilterTable.
 */
class NumberFilterPopup : CustomField<NumberInterval<Double>?>() {

    private lateinit var ltInput: TextField
    private lateinit var gtInput: TextField
    @Suppress("UNCHECKED_CAST")
    private val binder: Binder<NumberInterval<Double>> = Binder(NumberInterval::class.java as Class<NumberInterval<Double>>).apply { bean = NumberInterval(null, null) }
    private var internalValue: NumberInterval<Double>? = null

    override fun initContent(): Component? {
        return PopupView(SimpleContent.EMPTY).apply {
            w = fillParent; minimizedValueAsHTML = "All"; isHideOnMouseOut = false
            verticalLayout {
                w = wrapContent
                horizontalLayout {
                    gtInput = textField {
                        placeholder = "at least"
                        w = 100.px
                        bind(binder).toDouble().bind(NumberInterval<Double>::min)
                    }
                    label("..") {
                        w = wrapContent
                    }
                    ltInput = textField {
                        placeholder = "at most"
                        w = 100.px
                        bind(binder).toDouble().bind(NumberInterval<Double>::max)
                    }
                }
                horizontalLayout {
                    alignment = Alignment.MIDDLE_RIGHT
                    button("Clear") {
                        onLeftClick {
                            binder.fields.forEach { it.clear() }
                            value = null
                            isPopupVisible = false
                        }
                    }
                    button("Ok") {
                        onLeftClick {
                            value = binder.bean.copy()
                            isPopupVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        ltInput.isEnabled = !readOnly
        gtInput.isEnabled = !readOnly
    }

    private fun updateCaption() {
        val content = content as PopupView
        val value = value
        if (value == null || value.isUniversalSet) {
            content.minimizedValueAsHTML = "All"
        } else {
            if (value.isSingleItem) {
                content.minimizedValueAsHTML = "[x] = ${value.max}"
            } else if (value.min != null && value.max != null) {
                content.minimizedValueAsHTML = "${value.min} < [x] < ${value.max}"
            } else if (value.min != null) {
                content.minimizedValueAsHTML = "[x] >= ${value.min}"
            } else if (value.max != null) {
                content.minimizedValueAsHTML = "[x] <= ${value.max}"
            }
        }
    }

    override fun doSetValue(value: NumberInterval<Double>?) {
        internalValue = value?.copy()
        binder.bean = value?.copy() ?: NumberInterval<Double>(null, null)
        updateCaption()
    }

    override fun getValue() = internalValue?.copy()
}

/**
 * Converts this class to its non-primitive counterpart. For example, converts `int.class` to `Integer.class`.
 * @return converts class of primitive type to appropriate non-primitive class; other classes are simply returned as-is.
 */
@Suppress("UNCHECKED_CAST")
val <T> Class<T>.nonPrimitive: Class<T> get() = when(this) {
    Integer.TYPE -> Integer::class.java as Class<T>
    java.lang.Long.TYPE -> Long::class.java as Class<T>
    java.lang.Float.TYPE -> Float::class.java as Class<T>
    java.lang.Double.TYPE -> java.lang.Double::class.java as Class<T>
    java.lang.Short.TYPE -> Short::class.java as Class<T>
    java.lang.Byte.TYPE -> Byte::class.java as Class<T>
    else -> this
}

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
                                             val filterFactory: FilterFactory<F>) {
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
    private inner class FilterFieldWatcher<V>(private val field: HasValue<V?>, private val property: PropertyDefinition<T, V?>) : HasValue.ValueChangeListener<V?> {

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
 * A potentially open date range. If both [from] and [to] are `null`, then the interval accepts any date.
 * @property to the maximum accepted value, inclusive. If `null` then the date range has no upper limit.
 * @property from the minimum accepted value, inclusive. If `null` then the date range has no lower limit.
 */
data class DateInterval(var from: LocalDateTime?, var to: LocalDateTime?) : Serializable {
    /**
     * True if the interval includes all possible numbers (both [from] and [to] are `null`).
     */
    val isUniversalSet: Boolean
        get() = from == null && to == null

    private fun <T: Comparable<T>, F> T.legeFilter(propertyName: String, filterFactory: FilterFactory<F>, isLe: Boolean): F =
            if (isLe) filterFactory.le(propertyName, this) else filterFactory.ge(propertyName, this)

    private fun <F> LocalDateTime.toFilter(propertyName: String, filterFactory: FilterFactory<F>, fieldType: Class<*>, isLe: Boolean): F {
        return when (fieldType) {
            LocalDateTime::class.java -> legeFilter(propertyName, filterFactory, isLe)
            LocalDate::class.java -> toLocalDate().legeFilter(propertyName, filterFactory, isLe)
            else -> {
                val zoneOffset = ZoneOffset.ofTotalSeconds(Page.getCurrent().webBrowser.timezoneOffset / 1000)
                toInstant(zoneOffset).toDate.legeFilter(propertyName, filterFactory, isLe)
            }
        }
    }

    fun <F: Any> toFilter(propertyName: String, filterFactory: FilterFactory<F>, fieldType: Class<*>): F? {
        val filters = listOf(from?.toFilter(propertyName, filterFactory, fieldType, false), to?.toFilter(propertyName, filterFactory, fieldType, true)).filterNotNull()
        return filterFactory.and(filters.toSet())
    }
}

/**
 * A filter component which allows the user to specify a date range.
 */
class DateFilterPopup: CustomField<DateInterval?>() {
    private val formatter get() = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(UI.getCurrent().locale ?: Locale.getDefault())
    private lateinit var fromField: InlineDateTimeField
    private lateinit var toField: InlineDateTimeField
    private lateinit var set: Button
    private lateinit var clear: Button
    /**
     * The desired resolution of this filter popup, defaults to [DateTimeResolution.MINUTE].
     */
    var resolution: DateTimeResolution
        get() = fromField.resolution
        set(value) {
            fromField.resolution = value
            toField.resolution = value
        }

    private var internalValue: DateInterval? = null

    init {
        styleName = "datefilterpopup"
        // force initcontents so that fromField and toField are initialized and one can set resolution to them
        content
    }

    override fun doSetValue(value: DateInterval?) {
        internalValue = value?.copy()
        fromField.value = internalValue?.from
        toField.value = internalValue?.to
        updateCaption()
    }

    override fun getValue() = internalValue?.copy()

    private fun format(date: LocalDateTime?) = if (date == null) "" else formatter.format(date)

    private fun updateCaption() {
        val content = content as PopupView
        val value = value
        if (value == null || value.isUniversalSet) {
            content.minimizedValueAsHTML = "All"
        } else {
            content.minimizedValueAsHTML = "${format(fromField.value)} - ${format(toField.value)}"
        }
    }

    private fun truncateDate(date: LocalDateTime?, resolution: DateTimeResolution, start: Boolean): LocalDateTime? {
        @Suppress("NAME_SHADOWING")
        var date = date ?: return null
        for (res in DateTimeResolution.values().slice(0..resolution.ordinal - 1)) {
            if (res == DateTimeResolution.SECOND) {
                date = date.withSecond(if (start) 0 else 59)
            } else if (res == DateTimeResolution.MINUTE) {
                date = date.withMinute(if (start) 0 else 59)
            } else if (res == DateTimeResolution.HOUR) {
                date = date.withHour(if (start) 0 else 23)
            } else if (res == DateTimeResolution.DAY) {
                date = date.withDayOfMonth(if (start) 1 else date.toLocalDate().lengthOfMonth())
            } else if (res == DateTimeResolution.MONTH) {
                date = date.withMonth(if (start) 1 else 12)
            }
        }
        return date
    }

    override fun initContent(): Component? {
        return PopupView(SimpleContent.EMPTY).apply {
            w = fillParent; minimizedValueAsHTML = "All"; isHideOnMouseOut = false
            verticalLayout {
                styleName = "datefilterpopupcontent"; setSizeUndefined(); isSpacing = true; isMargin = true
                horizontalLayout {
                    isSpacing = true
                    fromField = inlineDateTimeField()
                    toField = inlineDateTimeField()
                }
                horizontalLayout {
                    alignment = Alignment.BOTTOM_RIGHT
                    isSpacing = true
                    set = button("Set", {
                        value = DateInterval(truncateDate(fromField.value, resolution, true), truncateDate(toField.value, resolution, false))
                        isPopupVisible = false
                    })
                    clear = button("Clear", {
                        value = null
                        isPopupVisible = false
                    })
                }
            }
        }
    }

    override fun attach() {
        super.attach()
        fromField.locale = locale
        toField.locale = locale
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        set.isEnabled = !readOnly
        clear.isEnabled = !readOnly
        fromField.isEnabled = !readOnly
        toField.isEnabled = !readOnly
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
