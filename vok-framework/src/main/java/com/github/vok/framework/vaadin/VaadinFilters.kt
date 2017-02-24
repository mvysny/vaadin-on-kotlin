package com.github.vok.framework.vaadin

import com.vaadin.data.Binder
import com.vaadin.data.HasValue
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.server.Resource
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.shared.ui.datefield.DateTimeResolution
import com.vaadin.ui.*
import com.vaadin.ui.components.grid.HeaderRow
import java.io.Serializable
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class NumberInterval<T : Number>(var lessThanValue: T?, var greaterThanValue: T?, var equalsValue: T?) : Serializable {
    fun toFilter(field: String): JPAFilter? {
        if (equalsValue != null) return EqFilter(field, equalsValue)
        if (lessThanValue != null && greaterThanValue != null) {
            return setOf(LtFilter(field, lessThanValue!!), GtFilter(field, greaterThanValue!!)).and()
        }
        if (lessThanValue != null) return LtFilter(field, lessThanValue!!)
        if (greaterThanValue != null) return GtFilter(field, greaterThanValue!!)
        return null
    }

    val isEmpty: Boolean
        get() = lessThanValue == null && greaterThanValue == null && equalsValue == null
}

/**
 * A filter component which allows the user to filter numeric value which is greater than, equal, or less than a value which user enters.
 * Stolen from Teppo Kurki's FilterTable.
 */
class NumberFilterPopup : CustomField<NumberInterval<Double>?>() {

    private lateinit var ok: Button
    private lateinit var reset: Button
    private lateinit var ltInput: TextField
    private lateinit var gtInput: TextField
    private lateinit var eqInput: TextField
    private var boundValue: NumberInterval<Double> = NumberInterval(null, null, null)
    @Suppress("UNCHECKED_CAST")
    private val binder: Binder<NumberInterval<Double>> = Binder(NumberInterval::class.java as Class<NumberInterval<Double>>).apply { bean = boundValue }
    private var internalValue: NumberInterval<Double>? = null

    override fun initContent(): Component? {
        return PopupView(SimpleContent.EMPTY).apply {
            w = fillParent; minimizedValueAsHTML = "All"
            gridLayout(2, 4) {
                isSpacing = true
                setMargin(true)
                setSizeUndefined()
                label("<")
                ltInput = textField {
                    bind(binder).stringToDouble().bind(NumberInterval<Double>::lessThanValue)
                    placeholder = "Less than"
                }
                label("=")
                eqInput = textField {
                    bind(binder).stringToDouble().bind(NumberInterval<Double>::equalsValue)
                    placeholder = "Equal to"
                    addValueChangeListener {
                        gtInput.isEnabled = isEmpty
                        ltInput.isEnabled = isEmpty
                    }
                }
                label(">")
                gtInput = textField {
                    bind(binder).stringToDouble().bind(NumberInterval<Double>::greaterThanValue)
                    placeholder = "Greater than"
                }
                val buttons = HorizontalLayout().apply {
                    w = fillParent
                    ok = button("Ok") {
                        expandRatio = 1f
                        alignment = Alignment.MIDDLE_RIGHT
                        onLeftClick {
                            value = boundValue.copy()
                            isPopupVisible = false
                        }
                    }
                    reset = button("Reset", {
                        value = null
                        isPopupVisible = false
                    })
                }
                addComponent(buttons, 0, 3, 1, 3)
            }
        }
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        ok.isEnabled = !readOnly
        reset.isEnabled = !readOnly
        ltInput.isEnabled = !readOnly
        gtInput.isEnabled = !readOnly
        eqInput.isEnabled = !readOnly
    }

    private fun updateCaption() {
        val content = content as PopupView
        val value = value
        if (value == null || value.isEmpty) {
            content.minimizedValueAsHTML = "All"
        } else {
            if (value.equalsValue != null) {
                content.minimizedValueAsHTML = "[x] = ${value.equalsValue}"
            } else if (value.greaterThanValue != null && value.lessThanValue != null) {
                content.minimizedValueAsHTML = "${value.greaterThanValue} < [x] < ${value.lessThanValue}"
            } else if (value.greaterThanValue != null) {
                content.minimizedValueAsHTML = "[x] > ${value.greaterThanValue}"
            } else if (value.lessThanValue != null) {
                content.minimizedValueAsHTML = "[x] < ${value.lessThanValue}"
            }
        }
    }

    override fun doSetValue(value: NumberInterval<Double>?) {
        boundValue = value?.copy() ?: NumberInterval<Double>(null, null, null)
        internalValue = value?.copy()
        gtInput.isEnabled = boundValue.equalsValue == null
        ltInput.isEnabled = boundValue.equalsValue == null
        binder.bean = boundValue
        updateCaption()
    }

    override fun getValue() = internalValue?.copy()
}

/**
 * Returns the type of the values present in this property.
 * @return the type, auto-converted from primitive type to corresponding boxed type, never null.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun KProperty<*>.getValueClass(): Class<*> {
    val clazz = (returnType as KClass<*>).java
    return when(clazz) {
        Integer.TYPE -> Integer::class.java
        java.lang.Long.TYPE -> Long::class.java
        java.lang.Float.TYPE -> Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Short.TYPE -> Short::class.java
        java.lang.Byte.TYPE -> Byte::class.java
        else -> clazz
    }
}

/**
 * Converts this class to its non-primitive counterpart. For example, converts `int.class` to `Integer.class`.
 * @return converts class of primitive type to appropriate non-primitive class; other classes are simply returned as-is.
 */
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
 * Produces filter fields and binds them to the container, to automatically perform the filtering itself.
 *
 * Currently, the filter fields have to be attached to the Grid manually: you will have to create a special HeaderRow in a Grid, create a field for each column,
 * add the field to the HeaderRow, and finally, [bind] the field to the container. [generateFilterComponents] can do that for you, just call
 * ```
 * grid.appendHeaderRow().generateFilterComponents(grid)
 * ```
 *
 * Currently, Vaadin does not support attaching of the filter fields to a vanilla Vaadin Table. Attaching filters to a Tepi FilteringTable
 * is currently not supported directly, but it may be done manually.
 * @property clazz The bean on which the filtering will be performed, not null.
 * @param T the type of beans handled by the Grid.
 * @author mvy, stolen from Teppo Kurki's FilterTable.
 */
abstract class FilterFieldFactory<T: Any>(protected val clazz: KClass<T>, val dataProvider: ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?>) {

    internal val filters = mutableSetOf<JPAFilter>()

    /**
     * Creates the filtering component. The component may not necessarily produce values of given data types - for example,
     * if the data type is a Double, the filtering component may produce a DoubleRange object which requires given value to be contained in a numeric range.
     *
     * [createFilter] is later used internally, to construct a filter for given field.
     * @param property the [clazz] property.
     * @return A field that can be assigned to the given fieldType and that is
     * *         capable of filtering given type of data. May return null if filtering of given data type with given field type is unsupported.
     */
    abstract fun <T> createField(property: KProperty<T>): HasValue<T>?

    protected fun getProperty(name: String) = clazz.members.filterIsInstance<KProperty<*>>().first { it.name == name }

    /**
     * Creates a new Container Filter based on given value.
     * @param value the value, may be null.
     * @param filterField the filter field itself
     * @param property the property
     * @return a filter, may be null if no filtering is needed or if the value indicates that the filtering is disabled for this column.
     */
    protected abstract fun <T> createFilter(value: T?, filterField: HasValue<T>, property: KProperty<T>): JPAFilter?

    /**
     * Binds given filtering field to a container - starts filtering based on the contents of the field, and starts watching for field value changes.
     * @param field The field which provides the filtering values, not null. [createFilter] is used to convert
     * the field's value to a filter.
     * @param property The bean property on which the filtering will be performed, not null.
     */
    fun <T> bind(field: HasValue<T>, property: KProperty<T>) {
        val filterFieldWatcher = FilterFieldWatcher(field, property)
        field.addValueChangeListener(filterFieldWatcher)
    }

    /**
     * Listens on value change on given field and updates [ConfigurableFilterDataProvider.setFilter] accordingly.
     * @property field The field which provides the filtering values.
     * @property property The bean property on which the filtering will be performed.
     * @param V the value type
     */
    private inner class FilterFieldWatcher<V>(private val field: HasValue<V>, private val property: KProperty<V>) : HasValue.ValueChangeListener<V> {

        /**
         * The current container filter, may be null if no filtering is currently needed because the
         * field's value indicates that the filtering is disabled for this column (e.g. the text filter is blank, the filter field is cleared, etc).
         */
        private var currentFilter: JPAFilter? = null

        init {
            valueChange()
        }

        override fun valueChange(event: HasValue.ValueChangeEvent<V>?) {
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
                dataProvider.setFilter(filters.and())
            }
        }
    }
}


data class DateInterval(var from: LocalDateTime?, var to: LocalDateTime?) : Serializable {
    val isEmpty: Boolean
        get() = from == null && to == null

    fun toFilter(field: String): JPAFilter? {
        if (isEmpty) return null
        var actualFrom = from
        var actualTo = to
//        val type = container.getType(propertyId)
//        if (type == java.sql.Date::class.java) {
//            actualFrom = if (actualFrom == null) null else java.sql.Date(actualFrom.time)
//            actualTo = if (actualTo == null) null else java.sql.Date(actualTo.time)
//        } else if (type == Timestamp::class.java) {
//            actualFrom = if (actualFrom == null) null else Timestamp(actualFrom.time)
//            actualTo = if (actualTo == null) null else Timestamp(actualTo.time)
//        }
        if (actualFrom != null && actualTo != null) {
            return setOf(Ge2Filter(field, actualFrom), Le2Filter(field, actualTo)).and()
        } else if (actualFrom != null) {
            return Ge2Filter(field, actualFrom)
        } else {
            return Le2Filter(field, actualTo!!)
        }
    }
}

class DateFilterPopup: CustomField<DateInterval?>() {
    private val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(UI.getCurrent().locale ?: Locale.getDefault())
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
        if (value == null || value.isEmpty) {
            content.minimizedValueAsHTML = "All"
        } else {
            content.minimizedValueAsHTML = "${format(fromField.value)} - ${format(toField.value)}"
        }
    }

    private fun truncateDate(date: LocalDateTime?, resolution: DateTimeResolution, start: Boolean): LocalDateTime? {
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
            w = fillParent; minimizedValueAsHTML = "All"
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
 * @author mvy, stolen from Teppo Kurki's FilterTable.
 */
class DefaultFilterFieldFactory<T: Any>(clazz: KClass<T>, dataProvider: ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?>) : FilterFieldFactory<T>(clazz, dataProvider) {
    /**
     * If true, number filters will be shown as a popup, which allows the user to set eq, less-than and greater-than fields.
     * If false, a simple in-place editor will be shown, which only allows to enter the eq number.
     *
     * Default implementation always returns true.
     * @param property the bean property
     */
    protected fun isUsePopupForNumericProperty(property: KProperty<*>): Boolean = true

    override fun <T> createField(property: KProperty<T>): HasValue<T>? {
        val type = property.getValueClass() as Class<T>
        val field: HasValue<*>
        if (type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java) {
            field = createBooleanField(property as KProperty<Boolean>)
        } else if (type.isEnum) {
            field = createEnumField(type, property)
        } else if (type == Date::class.java || type == Timestamp::class.java
                || type == java.sql.Date::class.java || Temporal::class.java.isAssignableFrom(type)) {
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
        return field as HasValue<T>
    }

    protected fun getEnumFilterDisplayName(property: KProperty<*>, constant: Enum<*>): String? = null

    protected fun getEnumFilterIcon(property: KProperty<*>, constant: Enum<*>): Resource? = null

    private fun <T> createEnumField(type: Class<T>, property: KProperty<T>): HasValue<*> {
        val enumSelect = ComboBox<T>()
        enumSelect.setItems(*type.enumConstants)
        enumSelect.itemCaptionGenerator = ItemCaptionGenerator { e -> getEnumFilterDisplayName(property, e as Enum<*>) ?: e.name }
        return enumSelect
    }

    protected fun createTextField(property: KProperty<*>): HasValue<*> = TextField()

    protected fun createDateField(property: KProperty<*>): DateFilterPopup = DateFilterPopup()

    protected fun createNumericField(type: Class<*>, propertyId: Any?) = NumberFilterPopup()

    /**
     * Don't forget that the returned field must be tri-state - true, false, null (to disable filtering).
     */
    protected fun createBooleanField(property: KProperty<Boolean>): HasValue<Boolean> {
        val booleanSelect = ComboBox<Boolean>()
        booleanSelect.setItems(listOf(true, false))
        booleanSelect.itemCaptionGenerator = ItemCaptionGenerator { b -> getBooleanFilterDisplayName(property, b) ?: b.toString() }
        booleanSelect.itemIconGenerator = IconGenerator { b -> getBooleanFilterIcon(property, b) }
        return booleanSelect
    }

    protected fun getBooleanFilterIcon(property: KProperty<Boolean>, value: Boolean): Resource? = null

    protected fun getBooleanFilterDisplayName(property: KProperty<Boolean>, value: Boolean): String? = null

    override fun <T> createFilter(value: T?, filterField: HasValue<T>, property: KProperty<T>): JPAFilter? = when {
        value is NumberInterval<*> -> value.toFilter(property.name)
        value is DateInterval -> value.toFilter(property.name)
        value is String && !value.isEmpty() -> generateGenericFilter<String>(filterField as HasValue<String>, property as KProperty<String>, value.trim())
        else -> null
    }

    protected fun <T: Serializable> generateGenericFilter(field: HasValue<T>, property: KProperty<T>, value: T): JPAFilter? {
        /* Special handling for ComboBox (= enum properties) */
        if (field is ComboBox) {
            return EqFilter(property.name, value)
        } else {
            return LikeFilter(property.name, "$value%")
        }
    }
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 */
fun <T: Any> HeaderRow.generateFilterComponents(grid: Grid<T>, itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T> = DefaultFilterFieldFactory(itemClass, grid.dataProvider as ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?>)) {
    val propertyIds = itemClass.members.filterIsInstance<KProperty<*>>().associateBy { it.name }
    for (propertyId in grid.columns.map { it.id }) {
        val property = propertyIds[propertyId]
        val field = if (property == null) null else filterFieldFactory.createField(property)
        val cell = getCell(propertyId)
        if (field == null) {
            cell.text = null  // this also removes the cell from the row
        } else {
            filterFieldFactory.bind(field, property!!)
            cell.component = field as Component
        }
    }
}
