package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributools.BrowserTimeZone
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.condition.Expression
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateInterval
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberInterval
import eu.vaadinonkotlin.toDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private fun Property<Any>.legeFilter(value: Any, isLe: Boolean): Condition =
        if (isLe) le(value) else ge(value)

/**
 * Creates a filter which matches all datetimes within given day.
 *
 * Note: [BrowserTimeZone] is used when comparing [LocalDate] with [Instant], [Date] and [Calendar] instances.
 */
public fun <T: Any> LocalDate.toFilter(propertyName: String,
                       fieldType: Class<*>): Filter<T> =
        DateInterval.of(this).toFilter<Filter<T>>(propertyName, DataLoaderFilterFactory(), fieldType)!!

/**
 * Takes `this` and converts it into a filter `propertyName <= this` or `propertyName >= this`, based
 * on the value of [isLe].
 *
 * Note: [BrowserTimeZone] is used when comparing [LocalDate] with [Instant], [Date] and [Calendar] instances.
 * @param fieldType converts `this` value to a value compatible with this type. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 */
private fun Property<Any>.lege(isLe: Boolean, date: LocalDate?): Condition {
    if (date == null) return Condition.NO_CONDITION
    val fieldType = this.valueType
    if (fieldType == LocalDate::class.java) {
        return legeFilter(date, isLe)
    }
    val dateTime: LocalDateTime = if (isLe) date.plusDays(1).atStartOfDay().minusSeconds(1) else date.atStartOfDay()
    if (fieldType == LocalDateTime::class.java) {
        return legeFilter(dateTime, isLe)
    }
    val instant: Instant = dateTime.atZone(BrowserTimeZone.get).toInstant()
    if (fieldType == Instant::class.java) {
        return legeFilter(instant, isLe)
    }
    if (Date::class.java.isAssignableFrom(fieldType)) {
        return legeFilter(instant.toDate, isLe)
    }
    if (Calendar::class.java.isAssignableFrom(fieldType)) {
        val cal: Calendar = Calendar.getInstance()
        cal.time = instant.toDate
        return legeFilter(cal, isLe)
    }
    throw IllegalArgumentException("Parameter fieldType: invalid value ${fieldType}: unsupported date type, can not compare")
}

/**
 * Creates a filter which accepts datetime-like values. Takes this and converts it into a filter `propertyName in this`.
 *
 * Note: [BrowserTimeZone] is used when comparing [LocalDate] with [Instant], [Date] and [Calendar] instances.
 *
 * [Property.getValueType] is used to convert [LocalDate] `from`/`to` values of this range to a value
 * comparable with values coming from this property. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 * @throws IllegalArgumentException if [Property.getValueType] is of unsupported type.
 */
public fun Property<*>.between(dateInterval: DateInterval): Condition {
    val f1 = (this as Property<Any>).lege(false, dateInterval.start)
    val f2 = this.lege(true, dateInterval.endInclusive)
    return f1.and(f2)
}

public fun <N> Expression<N>.between(numberInterval: NumberInterval<N>): Condition where N: Number, N: Comparable<N> = when {
    numberInterval.isSingleItem -> eq(numberInterval.endInclusive!!)
    numberInterval.endInclusive != null || numberInterval.start != null -> between(numberInterval.start, numberInterval.endInclusive)
    else -> Condition.NO_CONDITION
}
