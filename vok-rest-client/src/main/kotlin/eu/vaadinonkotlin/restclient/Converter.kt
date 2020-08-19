package eu.vaadinonkotlin.restclient

import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.util.*

/**
 * Converter which converts from [F] to [T].
 * @author mavi
 */
public interface Converter<F, T> {
    public fun convert(value: F): T
}

/**
 * Converts values of different types properly to String, so that they can be consumed by the REST endpoints.
 * The default implementation uses the following algorithm:
 * * Converts all [Number] to their decimal format with comma as the decimal separator, e.g. "54" or "2.25"
 * * Convert all Date-like objects such as [Date], [LocalDate] and [LocalDateTime] to the UTC Epoch long (number of milliseconds since January 1, 1970, 00:00:00 GMT in UTC).
 * * Fails for everything else.
 * @param zoneId used to convert [LocalDate] and [LocalDateTime] to UTC Epoch.
 */
public open class QueryParameterConverter(public val zoneId: ZoneId = ZoneId.systemDefault()) : Converter<Any, String> {
    protected fun convertNumber(number: Number): String = when(number) {
        is Int, is Short, is Byte, is Long, is BigInteger -> number.toString()
        is BigDecimal -> number.stripTrailingZeros().toPlainString()
        is Float -> convertNumber(number.toDouble())
        is Double -> convertNumber(number.toBigDecimal())
        else -> throw IllegalArgumentException("$number of type ${number.javaClass} is not supported")
    }

    override fun convert(value: Any): String = when(value) {
        is String -> value
        is Number -> convertNumber(value)
        is Date -> value.time.toString()
        is LocalDate -> convert(LocalDateTime.of(value, LocalTime.MIN))
        is LocalDateTime -> convert(value.atZone(zoneId))
        is ZonedDateTime -> convert(value.toInstant())
        is Instant -> value.toEpochMilli().toString()
        else -> throw IllegalArgumentException("$value of type ${value.javaClass} is not supported")
    }
}
