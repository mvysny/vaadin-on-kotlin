package eu.vaadinonkotlin.restclient

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.test.expect

class QueryParameterConverterTest {
    private val c = QueryParameterConverter(ZoneId.of("UTC"))

    @Test fun numbers() {
        expect("2") { c.convert(2) }
        expect("25") { c.convert(25.toByte()) }
        expect("-125") { c.convert((-125).toLong()) }
        expect("23423") { c.convert(23423.toShort()) }
        expect("23423") { c.convert(BigInteger("23423")) }
        expect("2.25") { c.convert(BigDecimal("2.25")) }
        expect("-2.25") { c.convert(BigDecimal("-2.25000")) }
        expect("45") { c.convert(45.toFloat()) }
        expect("45") { c.convert(45.toDouble()) }
    }

    @Test fun string() {
        expect("foobar") { c.convert("foobar") }
    }

    @Test fun dates() {
        expect("1234525") { c.convert(Date(1234525)) }
        expect("1234525") { c.convert(java.sql.Date(1234525)) }
        expect("1234525") { c.convert(java.sql.Timestamp(1234525)) }
        expect("318384000000") { c.convert(LocalDate.of(1980, 2, 3)) }
        expect("318403351000") { c.convert(LocalDateTime.of(1980, 2, 3, 5, 22, 31)) }
    }
}
