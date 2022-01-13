package eu.vaadinonkotlin.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.NumberInterval
import com.github.mvysny.vokdataloader.Filter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.expect

class FilterComponentsTest : DynaTest({
    group("date/time comparison") {
        test("LocalDate.toFilter()") {
            data class Person(var dob: LocalDate? = null, var tob: LocalDateTime? = null)
            val d: LocalDate = LocalDate.of(2020, 4, 20)
            var filter: Filter<Person> = d.toFilter("dob", LocalDate::class.java)
            expect(true) { filter.test(Person(d)) }
            expect(false) { filter.test(Person(d.minusDays(1))) }
            expect(false) { filter.test(Person(d.plusDays(1))) }
            filter = d.toFilter("tob", LocalDateTime::class.java)
            expect(true) { filter.test(Person(null, LocalDateTime.of(d, LocalTime.of(1, 0)))) }
            expect(false) { filter.test(Person(null, LocalDateTime.of(d.minusDays(1), LocalTime.of(23, 0)))) }
            expect(false) { filter.test(Person(null, LocalDateTime.of(d.plusDays(1), LocalTime.of(0, 0)))) }
        }
    }

    group("number comparison") {
        test("double number range, bigdecimal values") {
            data class Person(var salary: BigDecimal)
            var filter: Filter<Person> = NumberInterval<Double>(5.0, 15.0).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(BigDecimal(10))) }
            expect(false) { filter.test(Person(BigDecimal(0))) }
            expect(false) { filter.test(Person(BigDecimal(20))) }
            filter = NumberInterval<Double>(null, 15.0).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(BigDecimal(10))) }
            expect(true) { filter.test(Person(BigDecimal(0))) }
            expect(false) { filter.test(Person(BigDecimal(20))) }
            filter = NumberInterval<Double>(5.0, null).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(BigDecimal(10))) }
            expect(false) { filter.test(Person(BigDecimal(0))) }
            expect(true) { filter.test(Person(BigDecimal(20))) }
            expect(null) { NumberInterval<Double>(null, null).toFilter("salary", DataLoaderFilterFactory<Person>()) }
        }
        test("double number range, long values") {
            data class Person(var salary: Long)
            var filter: Filter<Person> = NumberInterval<Double>(5.0, 15.0).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(10L)) }
            expect(false) { filter.test(Person(0L)) }
            expect(false) { filter.test(Person(20L)) }
            filter = NumberInterval<Double>(null, 15.0).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(10L)) }
            expect(true) { filter.test(Person(0L)) }
            expect(false) { filter.test(Person(20L)) }
            filter = NumberInterval<Double>(5.0, null).toFilter("salary", DataLoaderFilterFactory<Person>())!!
            expect(true) { filter.test(Person(10L)) }
            expect(false) { filter.test(Person(0L)) }
            expect(true) { filter.test(Person(20L)) }
        }
    }
})