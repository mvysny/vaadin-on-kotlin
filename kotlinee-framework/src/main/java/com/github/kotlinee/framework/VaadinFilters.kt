package com.github.kotlinee.framework

import com.vaadin.data.Container
import com.vaadin.data.util.converter.Converter
import com.vaadin.data.util.converter.StringToDoubleConverter
import com.vaadin.data.util.filter.And
import com.vaadin.data.util.filter.Compare
import com.vaadin.ui.*
import java.io.Serializable

data class NumberInterval<T: Number>(val lessThanValue: T?, val greaterThanValue: T?, val equalsValue: T?) : Serializable {
    fun toFilter(propertyId: Any?): Container.Filter? {
        if (equalsValue != null) return Compare.Equal(propertyId, equalsValue)
        if (lessThanValue != null && greaterThanValue != null) {
            return And(Compare.Less(propertyId, lessThanValue), Compare.Greater(propertyId, greaterThanValue))
        }
        if (lessThanValue != null) return Compare.Less(propertyId, lessThanValue)
        if (greaterThanValue != null) return Compare.Greater(propertyId, greaterThanValue)
        return null
    }
}

/**
 * A filter component which allows the user to filter numeric value which is greater than, equal, or less than a value which user enters.
 */
class NumberFilterPopup: CustomField<NumberInterval<Double>>() {
    override fun getType(): Class<out NumberInterval<Double>>? = NumberInterval::class.java as Class<NumberInterval<Double>>

    private lateinit var ok: Button
    private lateinit var reset: Button
    private lateinit var ltInput: TextField
    private lateinit var gtInput: TextField
    private lateinit var eqInput: TextField

    override fun initContent(): Component? {
        return PopupView(SimpleContent.EMPTY).apply {
            minimizedValueAsHTML = "all"
            gridLayout(2, 4) {
                isSpacing = true
                setMargin(true)
                setSizeUndefined()
                label("<")
                ltInput = textField {
                    setConverter(StringToDoubleConverter())
                    inputPrompt = "Less than"
                }
                label("=")
                eqInput = textField {
                    setConverter(StringToDoubleConverter())
                    inputPrompt = "Equal to"
                }
                label(">")
                gtInput = textField {
                    setConverter(StringToDoubleConverter())
                    inputPrompt = "Greater than"
                }
                val buttons = HorizontalLayout().apply {
                    setWidthFull()
                    ok = button("Ok") {
                        expandRatio = 1f
                        alignment = Alignment.MIDDLE_RIGHT
                        setLeftClickListener {
                            try {
                                this@NumberFilterPopup.value = NumberInterval<Double>(ltInput.convertedValue as Double?,
                                        gtInput.convertedValue as Double?, eqInput.convertedValue as Double?)
                                isPopupVisible = false
                            } catch (ex: Converter.ConversionException) {
                                // no need to log this - it is sufficient that the conversion error messages are already shown
                                // next to component labels
                            }
                        }
                    }
                    reset = button("Reset") {
                        this@NumberFilterPopup.value = null
                        isPopupVisible = false
                    }
                }
                addComponent(buttons, 0, 3, 1, 3)
            }
        }
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        ok.setEnabled(!readOnly)
        reset.setEnabled(!readOnly)
        ltInput.isEnabled = !readOnly
        gtInput.isEnabled = !readOnly
        eqInput.isEnabled = !readOnly
    }

}