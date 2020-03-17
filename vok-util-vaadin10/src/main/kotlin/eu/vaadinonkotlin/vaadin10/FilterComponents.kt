package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.karibudsl.v10.DateInterval
import com.github.mvysny.karibudsl.v10.browserTimeZone
import com.github.mvysny.vokdataloader.Filter
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.shared.Registration
import eu.vaadinonkotlin.FilterFactory
import eu.vaadinonkotlin.toDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private fun <T : Comparable<T>, F> T.legeFilter(propertyName: String, filterFactory: FilterFactory<F>, isLe: Boolean): F =
        if (isLe) filterFactory.le(propertyName, this) else filterFactory.ge(propertyName, this)

/**
 * Takes `this` and converts it into a filter `propertyName <= this` or `propertyName >= this`, based
 * on the value of [isLe].
 * @param fieldType converts `this` value to a value compatible with this type. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
private fun <F> LocalDate.toFilter(
        propertyName: String,
        filterFactory: FilterFactory<F>,
        fieldType: Class<*>,
        isLe: Boolean
): F {
    if (fieldType == LocalDate::class.java) {
        return legeFilter(propertyName, filterFactory, isLe)
    }
    val dateTime: LocalDateTime = if (isLe) plusDays(1).atStartOfDay().minusSeconds(1) else atStartOfDay()
    if (fieldType == LocalDateTime::class.java) {
        return dateTime.legeFilter(propertyName, filterFactory, isLe)
    }
    val instant: Instant = dateTime.atZone(browserTimeZone).toInstant()
    if (fieldType == Instant::class.java) {
        return instant.legeFilter(propertyName, filterFactory, isLe)
    }
    if (Date::class.java.isAssignableFrom(fieldType)) {
        return instant.toDate.legeFilter(propertyName, filterFactory, isLe)
    }
    if (Calendar::class.java.isAssignableFrom(fieldType)) {
        val cal: Calendar = Calendar.getInstance()
        cal.time = instant.toDate
        return cal.legeFilter(propertyName, filterFactory, isLe)
    }
    throw IllegalArgumentException("Parameter fieldType: invalid value ${fieldType}: unsupported date type, can not compare")
}

/**
 * Takes `this` and converts it into a filter `propertyName in this`.
 * @param fieldType converts `this` value to a value compatible with this type. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
fun <F : Any> DateInterval.toFilter(
        propertyName: String,
        filterFactory: FilterFactory<F>,
        fieldType: Class<*>
): F? {
    val filters: List<F> = listOfNotNull(
            from?.toFilter(propertyName, filterFactory, fieldType, false),
            to?.toFilter(propertyName, filterFactory, fieldType, true)
    )
    return filterFactory.and(filters.toSet())
}


data class ValueChangeEventImpl<V>(
        private val hasValue: HasValue<*, V>,
        private val isFromClient: Boolean,
        private val oldValue: V?,
        private val value: V?
) : HasValue.ValueChangeEvent<V> {
    override fun getHasValue(): HasValue<*, V> = hasValue

    override fun getOldValue(): V? = oldValue

    override fun isFromClient(): Boolean = isFromClient

    override fun getValue(): V? = value
}

/**
 * A [HasValue] which produces instances of VOK [Filter]. Used to generate [Filter]
 * instances out of filter components, typically placed in the Grid Header Bar.
 *
 * @todo mavi more documentation, examples, etc.
 */
typealias HasFilterValue<B> = HasValue<HasValue.ValueChangeEvent<Filter<B>>, Filter<B>>

abstract class FilterComponentAdapter<V : Any, B : Any>(
        val delegate: HasValue<out HasValue.ValueChangeEvent<V>, V>
) : HasFilterValue<B> {

    override fun setValue(value: Filter<B>?) {
        require(value == null) { "The adapter doesn't support displaying filters: $value" }
        delegate.clear()
    }

    override fun setReadOnly(readOnly: Boolean) {
        delegate.isReadOnly = readOnly
    }

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        delegate.isRequiredIndicatorVisible = requiredIndicatorVisible
    }

    override fun isReadOnly(): Boolean = delegate.isReadOnly

    override fun isRequiredIndicatorVisible(): Boolean = delegate.isRequiredIndicatorVisible

    protected abstract fun convertToFilter(value: V?): Filter<B>?

    override fun getValue(): Filter<B>? = convertToFilter(delegate.value)

    override fun addValueChangeListener(listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<Filter<B>>>): Registration {
        val reg: Registration = delegate.addValueChangeListener { e: HasValue.ValueChangeEvent<V> ->
            val newEvent: HasValue.ValueChangeEvent<Filter<B>> = ValueChangeEventImpl(this,
                    e.isFromClient,
                    convertToFilter(e.oldValue),
                    convertToFilter(e.value))
            listener.valueChanged(newEvent)
        }
        return Registration { reg.remove() }
    }
}

/**
 * Returns a [HasFilterValue] view of a [NumberFilterPopup]. See [HasFilterValue] for more info.
 */
fun <B : Any> HasValue<*, NumberInterval<Double>>.asFilterValue(propertyName: String): HasFilterValue<B> =
        object : FilterComponentAdapter<NumberInterval<Double>, B>(this) {
            override fun convertToFilter(value: NumberInterval<Double>?): Filter<B>? =
                    value?.toFilter(propertyName, DataLoaderFilterFactory())
        }

/**
 * Returns a [HasFilterValue] view of a DateRangePopup. See [HasFilterValue] for more info.
 */
@JvmName("dateIntervalAsFilterValue")
fun <B : Any> HasValue<*, DateInterval>.asFilterValue(propertyName: String, fieldType: Class<*>): HasFilterValue<B> =
        object : FilterComponentAdapter<DateInterval, B>(this) {
            override fun convertToFilter(value: DateInterval?): Filter<B>? =
                    value?.toFilter(propertyName, DataLoaderFilterFactory(), fieldType)
        }
