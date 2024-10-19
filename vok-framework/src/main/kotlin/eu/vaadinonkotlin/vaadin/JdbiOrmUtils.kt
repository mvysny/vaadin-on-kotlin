package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributools.BrowserTimeZone
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.condition.Condition
import com.gitlab.mvysny.jdbiorm.condition.Expression
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateInterval
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberInterval
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
public fun Property<*>.between(dateInterval: DateInterval): Condition = dateInterval.contains(this, BrowserTimeZone.get)

public fun <N> Expression<N?>.between(numberInterval: NumberInterval<N>): Condition where N: Number, N: Comparable<N> =
    numberInterval.contains(this)
