package com.github.vok.framework.flow

import com.github.vok.framework.FilterFactory
import com.github.vok.framework.toDate
import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.AbstractCompositeField
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A potentially open numeric range. If both [min] and [max] are `null`, then the interval accepts any number.
 * @property min the minimum accepted value, inclusive. If `null` then the numeric range has no lower limit.
 * @property max the maximum accepted value, inclusive. If `null` then the numeric range has no upper limit.
 */
data class NumberInterval<T : Number>(var min: T?, var max: T?) : Serializable {

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
 * Only shows a single button as its contents. When the button is clicked, it opens a dialog and allows the user to specify a range
 * of numbers. When the user sets the values, the dialog is
 * hidden and the number range is set as the value of the popup.
 *
 * The current numeric range is also displayed as the caption of the button.
 */
class NumberFilterPopup: AbstractCompositeField<Button, NumberFilterPopup, NumberInterval<Double>>(null) {
    private lateinit var ltInput: TextField
    private lateinit var gtInput: TextField
    private val binder: Binder<NumberInterval<Double>> = Binder(NumberInterval::class.java as Class<NumberInterval<Double>>).apply { bean = NumberInterval(null, null) }
    private lateinit var set: Button
    private lateinit var clear: Button
    private val dialog = Dialog()
    private val content = Button()

    init {
        dialog.apply {
            isCloseOnEsc = true
            isCloseOnOutsideClick = true
            addOpenedChangeListener({
                if (!isOpened) {
                    element.removeFromParent();
                }
            })
            verticalLayout {
                horizontalLayout {
                    gtInput = textField {
                        placeholder = voki18n["filter.atleast"]
                        bind(binder).toDouble().bind(NumberInterval<Double>::min)
                    }
                    text("..")
                    ltInput = textField {
                        placeholder = voki18n["filter.atmost"]
                        bind(binder).toDouble().bind(NumberInterval<Double>::max)
                    }
                }
                horizontalLayout {
                    set = button(voki18n["filter.set"]) {
                        onLeftClick {
                            val value = binder.bean.copy()
                            setModelValue(if (value.isUniversalSet) null else value, true)
                            updateCaption()
                            dialog.close()
                        }
                    }
                    clear = button(voki18n["filter.clear"]) {
                        onLeftClick {
                            binder.fields.forEach { it.clear() }
                            setModelValue(null, true)
                            updateCaption()
                            dialog.close()
                        }
                    }
                }
            }
        }
        content.apply {
            onLeftClick {
                dialog.isOpened = !dialog.isOpened
            }
        }
        updateCaption()
    }

    override fun setPresentationValue(newPresentationValue: NumberInterval<Double>?) {
        binder.bean = newPresentationValue?.copy() ?: NumberInterval<Double>(null, null)
        updateCaption()
    }

    private fun updateCaption() {
        val value = value
        if (value == null || value.isUniversalSet) {
            content.text = voki18n["filter.all"]
        } else {
            if (value.isSingleItem) {
                content.text = "[x] = ${value.max}"
            } else if (value.min != null && value.max != null) {
                content.text = "${value.min} ≤ [x] ≤ ${value.max}"
            } else if (value.min != null) {
                content.text = "[x] ≥ ${value.min}"
            } else if (value.max != null) {
                content.text = "[x] ≤ ${value.max}"
            }
        }
    }

    override fun setReadOnly(readOnly: Boolean) {
        set.isEnabled = !readOnly
        clear.isEnabled = !readOnly
        ltInput.isEnabled = !readOnly
        gtInput.isEnabled = !readOnly
    }

    override fun initContent(): Button = content

    override fun isReadOnly(): Boolean = !ltInput.isEnabled

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        ltInput.isRequiredIndicatorVisible = requiredIndicatorVisible
        gtInput.isRequiredIndicatorVisible = requiredIndicatorVisible
    }

    override fun isRequiredIndicatorVisible(): Boolean = ltInput.isRequiredIndicatorVisible
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

private fun <T: Comparable<T>, F> T.legeFilter(propertyName: String, filterFactory: FilterFactory<F>, isLe: Boolean): F =
    if (isLe) filterFactory.le(propertyName, this) else filterFactory.ge(propertyName, this)

private fun <F> LocalDate.toFilter(propertyName: String, filterFactory: FilterFactory<F>, fieldType: Class<*>, isLe: Boolean): F {
    val dateTime: LocalDateTime = if (isLe) plusDays(1).atStartOfDay().minusSeconds(1) else atStartOfDay()
    return when (fieldType) {
        LocalDateTime::class.java -> dateTime.legeFilter(propertyName, filterFactory, isLe)
        LocalDate::class.java -> legeFilter(propertyName, filterFactory, isLe)
        else -> {
            dateTime.atZone(browserTimeZone).toInstant().toDate.legeFilter(propertyName, filterFactory, isLe)
        }
    }
}

fun <F: Any> DateInterval.toFilter(propertyName: String, filterFactory: FilterFactory<F>, fieldType: Class<*>): F? {
    val filters = listOfNotNull(from?.toFilter(propertyName, filterFactory, fieldType, false), to?.toFilter(propertyName, filterFactory, fieldType, true))
    return filterFactory.and(filters.toSet())
}
