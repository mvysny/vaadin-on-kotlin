package com.github.vok.framework.vaadin

import com.vaadin.data.Binder
import com.vaadin.ui.*
import java.io.Serializable

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
                    bind(binder).stringToDouble().bind("lessThanValue")
                    placeholder = "Less than"
                }
                label("=")
                eqInput = textField {
                    bind(binder).stringToDouble().bind("equalsValue")
                    placeholder = "Equal to"
                    addValueChangeListener {
                        gtInput.isEnabled = isEmpty
                        ltInput.isEnabled = isEmpty
                    }
                }
                label(">")
                gtInput = textField {
                    bind(binder).stringToDouble().bind("greaterThanValue")
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
//
///**
// * Produces filter fields and binds them to the container, to automatically perform the filtering itself.
// *
// * Currently, the filter fields have to be attached to the Grid manually: you will have to create a special HeaderRow in a Grid, create a field for each column (propertyId),
// * add the field to the HeaderRow, and finally, [bind][.bind] the field to the container.
// *
// * Currently, Vaadin does not support attaching of the filter fields to a vanilla Vaadin Table. Attaching filters to a Tepi FilteringTable
// * is currently not supported directly, but it may be done manually.
// * @property container The container on which the filtering will be performed, not null.
// * @author mvy, stolen from Teppo Kurki's FilterTable.
// */
//abstract class FilterFieldFactory(protected val container: Container.Filterable) {
//    /**
//     * Creates the filtering component. The component may not necessarily produce values of given data types - for example,
//     * if the data type is a Double, the filtering component may produce a DoubleRange object which requires given value to be contained in a numeric range.
//     *
//     * [createFilter] is later used internally, to construct a filter for given field.
//     * @param propertyId the column identifier, not null.
//     * @return A field that can be assigned to the given fieldType and that is
//     * *         capable of filtering given type of data. May return null if filtering of given data type with given field type is unsupported.
//     */
//    abstract fun createField(propertyId: Any?): Field<*>?
//
//    /**
//     * Returns the type of the values present in given column.
//     * @param propertyId the column id, not null
//     * @return the type, auto-converted from primitive type to corresponding boxed type, never null.
//     */
//    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
//    protected fun getValueClass(propertyId: Any?): Class<*> {
//        val clazz = container.getType(propertyId)
//        return when(clazz) {
//            Integer.TYPE -> Integer::class.java
//            java.lang.Long.TYPE -> Long::class.java
//            java.lang.Float.TYPE -> Float::class.java
//            java.lang.Double.TYPE -> java.lang.Double::class.java
//            java.lang.Short.TYPE -> Short::class.java
//            java.lang.Byte.TYPE -> Byte::class.java
//            else -> clazz
//        }
//    }
//
//    /**
//     * Creates a new Container Filter based on given value.
//     * @param value the value, may be null.
//     * @param filterField the filter field itself
//     * @param propertyId the property ID of the container's column
//     * @return a filter, may be null if no filtering is needed or if the value indicates that the filtering is disabled for this column.
//     */
//    protected abstract fun createFilter(value: Any?, filterField: Field<*>, propertyId: Any?): Container.Filter?
//
//    /**
//     * Binds given filtering field to a container - starts filtering based on the contents of the field, and starts watching for field value changes.
//     * @param field The field which provides the filtering values, not null. [.createFilter] is used to convert
//     * the field's value to a filter.
//     * @param propertyId The column (property) ID of the container, on which the filtering will be performed, not null.
//     */
//    fun bind(field: Field<*>, propertyId: Any?) {
//        val filterFieldWatcher = FilterFieldWatcher(field, propertyId)
//        if (field is AbstractTextField) {
//            field.addTextChangeListener(filterFieldWatcher)
//        } else {
//            field.addValueChangeListener(filterFieldWatcher)
//        }
//    }
//
//    /**
//     * @property field The field which provides the filtering values, not null.
//     * @property propertyId The column (property) ID of the container, on which the filtering will be performed, not null.
//     */
//    private inner class FilterFieldWatcher(private val field: Field<*>, private val propertyId: Any?) : Property.ValueChangeListener, FieldEvents.TextChangeListener {
//
//        /**
//         * The current container filter, may be null if no filtering is currently needed because the
//         * field's value indicates that the filtering is disabled for this column (e.g. the text filter is blank, the filter field is cleared, etc).
//         */
//        private var currentFilter: Container.Filter? = null
//
//        init {
//            valueChange()
//        }
//
//        override fun valueChange(event: Property.ValueChangeEvent) {
//            valueChange()
//        }
//
//        private fun valueChange(value: Any? = field.value) {
//            val newFilter = createFilter(value, field, propertyId)
//            if (newFilter != currentFilter) {
//                if (currentFilter != null) {
//                    container.removeContainerFilter(currentFilter)
//                    currentFilter = null
//                }
//                if (newFilter != null) {
//                    container.addContainerFilter(newFilter)
//                    currentFilter = newFilter
//                }
//            }
//        }
//
//        override fun textChange(event: FieldEvents.TextChangeEvent) {
//            valueChange(event.text)
//        }
//    }
//}
//
//data class DateInterval(val from: Date?, val to: Date?) : Serializable {
//    val isEmpty: Boolean
//        get() = from == null && to == null
//
//    fun toFilter(container: Container.Filterable, propertyId: Any?): Container.Filter? {
//        if (isEmpty) return null
//        var actualFrom = from
//        var actualTo = to
//        val type = container.getType(propertyId)
//        if (type == java.sql.Date::class.java) {
//            actualFrom = if (actualFrom == null) null else java.sql.Date(actualFrom.time)
//            actualTo = if (actualTo == null) null else java.sql.Date(actualTo.time)
//        } else if (type == Timestamp::class.java) {
//            actualFrom = if (actualFrom == null) null else Timestamp(actualFrom.time)
//            actualTo = if (actualTo == null) null else Timestamp(actualTo.time)
//        }
//        if (actualFrom != null && actualTo != null) {
//            return Between(propertyId, actualFrom, actualTo)
//        } else if (actualFrom != null) {
//            return Compare.GreaterOrEqual(propertyId, actualFrom)
//        } else {
//            return Compare.LessOrEqual(propertyId, actualTo)
//        }
//    }
//}
//
//class DateFilterPopup: CustomField<DateInterval?>() {
//    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, UI.getCurrent().locale ?: Locale.getDefault())
//    private lateinit var fromField: DateField
//    private lateinit var toField: DateField
//    private lateinit var set: Button
//    private lateinit var clear: Button
//    val resolution: Resolution = Resolution.DAY
//
//    init {
//        styleName = "datefilterpopup"
//    }
//
//    override fun setValue(newFieldValue: DateInterval?) {
//        fromField.value = newFieldValue?.from
//        toField.value = newFieldValue?.to
//        super.setValue(newFieldValue)
//        updateCaption()
//    }
//
//    private fun format(date: Date?) = if (date == null) "" else dateFormat.format(date)
//
//    private fun updateCaption() {
//        val content = content as PopupView
//        val value = value
//        if (value == null || value.isEmpty) {
//            content.minimizedValueAsHTML = "All"
//        } else {
//            content.minimizedValueAsHTML = "${format(fromField.value)} - ${format(toField.value)}"
//        }
//    }
//
//    private fun truncateDate(date: Date?, resolution: Resolution, start: Boolean): Date? {
//        if (date == null) {
//            return null
//        }
//        val cal = Calendar.getInstance(locale ?: UI.getCurrent().locale ?: Locale.getDefault())
//        cal.time = date
//        cal.set(Calendar.MILLISECOND, if (start) 0 else 999)
//        for (res in Resolution.getResolutionsLowerThan(resolution)) {
//            if (res == Resolution.SECOND) {
//                cal.set(Calendar.SECOND, if (start) 0 else 59)
//            } else if (res == Resolution.MINUTE) {
//                cal.set(Calendar.MINUTE, if (start) 0 else 59)
//            } else if (res == Resolution.HOUR) {
//                cal.set(Calendar.HOUR_OF_DAY, if (start) 0 else 23)
//            } else if (res == Resolution.DAY) {
//                cal.set(Calendar.DAY_OF_MONTH,
//                        if (start) 1 else cal.getActualMaximum(Calendar.DAY_OF_MONTH))
//            } else if (res == Resolution.MONTH) {
//                cal.set(Calendar.MONTH,
//                        if (start) 0 else cal.getActualMaximum(Calendar.MONTH))
//            }
//        }
//        return cal.time
//    }
//
//    override fun initContent(): Component? {
//        return PopupView(SimpleContent.EMPTY).apply {
//            w = fillParent; minimizedValueAsHTML = "All"
//            verticalLayout {
//                styleName = "datefilterpopupcontent"; setSizeUndefined(); isSpacing = true; isMargin = true
//                horizontalLayout {
//                    isSpacing = true
//                    fromField = inlineDateField()
//                    toField = inlineDateField()
//                }
//                horizontalLayout {
//                    alignment = Alignment.BOTTOM_RIGHT
//                    isSpacing = true
//                    set = button("Set", {
//                        value = DateInterval(truncateDate(fromField.value, resolution, true), truncateDate(toField.value, resolution, false))
//                        isPopupVisible = false
//                    })
//                    clear = button("Clear", {
//                        value = null
//                        isPopupVisible = false
//                    })
//                }
//            }
//        }
//    }
//
//    override fun attach() {
//        super.attach()
//        fromField.locale = locale
//        toField.locale = locale
//    }
//
//    override fun getType(): Class<out DateInterval> {
//        return DateInterval::class.java
//    }
//
//    override fun setReadOnly(readOnly: Boolean) {
//        super.setReadOnly(readOnly)
//        set.isEnabled = !readOnly
//        clear.isEnabled = !readOnly
//        fromField.isEnabled = !readOnly
//        toField.isEnabled = !readOnly
//    }
//}
//
///**
// * Provides default implementation for [FilterFieldFactory].
// * Supports filter fields for dates, numbers and strings.
// * @author mvy, stolen from Teppo Kurki's FilterTable.
// */
//class DefaultFilterFieldFactory(container: Container.Filterable) : FilterFieldFactory(container) {
//    /**
//     * If true, number filters will be shown as a popup, which allows the user to set eq, less-than and greater-than fields.
//     * If false, a simple in-place editor will be shown, which only allows to enter the eq number.
//     *
//     * Default implementation always returns true.
//     * @param propertyId column id
//     */
//    protected fun isUsePopupForNumericProperty(propertyId: Any?): Boolean = true
//
//    override fun createField(propertyId: Any?): Field<*> {
//        val type = getValueClass(propertyId)
//        val field: AbstractField<*>
//        if (type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java) {
//            field = createBooleanField(propertyId)
//        } else if (type.isEnum) {
//            field = createEnumField(type, propertyId)
//        } else if (type == Date::class.java || type == Timestamp::class.java
//                || type == java.sql.Date::class.java) {
//            field = createDateField(propertyId)
//        } else if (Number::class.java.isAssignableFrom(type) && isUsePopupForNumericProperty(propertyId)) {
//            field = createNumericField(type, propertyId)
//        } else {
//            field = createTextField(propertyId)
//        }
//        field.apply {
//            w = fillParent; isImmediate = true
//        }
//        return field
//    }
//
//    protected fun getEnumFilterDisplayName(propertyId: Any?, constant: Enum<*>): String? = null
//
//    protected fun getEnumFilterIcon(propertyId: Any?, constant: Enum<*>): Resource? = null
//
//    private fun createEnumField(type: Class<*>, propertyId: Any?): AbstractField<Any> {
//        val enumSelect = ComboBox()
//        val nullItem = enumSelect.addItem()
//        enumSelect.nullSelectionItemId = nullItem
//        enumSelect.setItemCaption(nullItem, "")
//        /* Add items from enumeration */
//        for (o in type.enumConstants) {
//            enumSelect.addItem(o, getEnumFilterDisplayName(propertyId, o as Enum<*>) ?: o.name)
//            enumSelect.setItemIcon(o, getEnumFilterIcon(propertyId, o))
//        }
//        return enumSelect
//    }
//
//    private fun createTextField(propertyId: Any?): AbstractField<*> {
//        val textField = TextField()
//        textField.nullRepresentation = ""
//        return textField
//    }
//
//    protected fun createDateField(propertyId: Any?): DateFilterPopup = DateFilterPopup()
//
//    protected fun createNumericField(type: Class<*>, propertyId: Any?) = NumberFilterPopup()
//
//    private fun createBooleanField(propertyId: Any?): AbstractField<*> {
//        val booleanSelect = ComboBox()
//        booleanSelect.addItem(true, getBooleanFilterDisplayName(propertyId, true) ?: "true")
//        booleanSelect.addItem(false, getBooleanFilterDisplayName(propertyId, false) ?: "false")
//        /* Add possible 'view all' item */
//        val nullItem = booleanSelect.addItem()
//        booleanSelect.nullSelectionItemId = nullItem
//        booleanSelect.setItemCaption(nullItem, "")
//        booleanSelect.setItemIcon(true, getBooleanFilterIcon(propertyId, true))
//        booleanSelect.setItemIcon(false, getBooleanFilterIcon(propertyId, false))
//        return booleanSelect
//    }
//
//    protected fun getBooleanFilterIcon(propertyId: Any?, value: Boolean): Resource? = null
//
//    protected fun getBooleanFilterDisplayName(propertyId: Any?, value: Boolean): String? = null
//
//    override fun createFilter(value: Any?, filterField: Field<*>, propertyId: Any?): Container.Filter? = when {
//        value is NumberInterval<*> -> value.toFilter(propertyId)
//        value is DateInterval -> value.toFilter(container, propertyId)
//        value is String && !value.isEmpty() -> generateGenericFilter(filterField, propertyId, value.trim())
//        else -> null
//    }
//
//    protected fun generateGenericFilter(field: Property<*>, propertyId: Any?, value: Any): Container.Filter {
//        /* Special handling for ComboBox (= enum properties) */
//        if (field is ComboBox) {
//            return Compare.Equal(propertyId, value)
//        } else {
//            return SimpleStringFilter(propertyId, value.toString(), true, false)
//        }
//    }
//}
//
///**
// * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
// * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
// * @param grid the owner grid.
// * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
// */
//fun Grid.HeaderRow.generateFilterComponents(grid: Grid, filterFieldFactory: FilterFieldFactory = DefaultFilterFieldFactory(grid.containerDataSource as Container.Filterable)) {
//    for (propertyId in grid.containerDataSource.containerPropertyIds) {
//        val field = if (grid.containerDataSource.isGenerated(propertyId)) null else filterFieldFactory.createField(propertyId)
//        val cell = getCell(propertyId)
//        if (field == null) {
//            cell.text = null  // this also removes the cell from the row
//        } else {
//            filterFieldFactory.bind(field, propertyId)
//            cell.component = field
//        }
//    }
//}
