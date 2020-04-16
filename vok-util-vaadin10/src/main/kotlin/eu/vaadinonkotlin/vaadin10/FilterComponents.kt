package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.karibudsl.v10.DateInterval
import com.github.mvysny.karibudsl.v10.browserTimeZone
import com.vaadin.flow.component.combobox.ComboBox
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
 * Creates a filter which accepts dates Takes this and converts it into a filter `propertyName in this`.
 * @param fieldType used to convert [LocalDate] `from`/`to` values of this range to a value
 * comparable with values coming from [propertyName]. Supports [LocalDate],
 * [LocalDateTime], [Instant], [Date] and [Calendar].
 * @throws IllegalArgumentException if [fieldType] is of unsupported type.
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

/**
 * A very simple [ComboBox] with two pre-filled values: `true` and `false`.
 */
open class BooleanComboBox : ComboBox<Boolean>(null, true, false)

/**
 * Creates a very simple [ComboBox] with all enum constants as items. Perfect for
 * filters for enum-based Grid columns.
 * @param E the enum type
 * @param items options in the combo box, defaults to all constants of [E].
 */
inline fun <reified E: Enum<E>> enumComboBox(
        items: List<E> = E::class.java.enumConstants.toList()
): ComboBox<E?> = ComboBox<E?>().apply {
    setItems(items)
    setItemLabelGenerator { item: E? -> item?.name ?: "" }
}
