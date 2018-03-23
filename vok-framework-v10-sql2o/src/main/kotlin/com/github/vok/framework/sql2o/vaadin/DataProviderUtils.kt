package com.github.vok.framework.sql2o.vaadin

import com.github.vokorm.Filter
import com.github.vokorm.SqlWhereBuilder
import com.github.vokorm.and
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.function.SerializablePredicate

/**
 * A data provider with configurable filter - use [setFilter] to set your custom filter. Note that user-provided filter value in Grid
 * will overwrite this filter; if you want an unremovable filter use [withFilter].
 */
typealias VokDataProvider<T> = ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return data provider which can be configured to always apply given filter.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.withConfigurableFilter2() : VokDataProvider<T> =
    withConfigurableFilter({ f1: Filter<T>?, f2: Filter<T>? -> listOf(f1, f2).filterNotNull().toSet().and() })

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will chain the data providers and restrict the rows further.
 * @param other applies this filter
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.withFilter(other: Filter<T>) : VokDataProvider<T> =
    withConfigurableFilter2().apply {
        // wrap the current DP so that we won't change the filter
        setFilter(other)
        // wrap the DP again so that nobody will change our filter.
    }.withConfigurableFilter2()

// since SerializablePredicate is Vaadin-built-in interface which our Filters do not use, since Vaadin 8 has different SerializablePredicate class
// than Vaadin 10 and we would have to duplicate filters just because a dumb interface.
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withVOKFilterAdapter() : VokDataProvider<T> =
    withConvertedFilter<Filter<T>?> {
        f -> if (f == null) null else SerializablePredicate { f.test(it) }
    }.withConfigurableFilter2()

/**
 * Produces a new data provider with unremovable [filter] which restricts rows returned by the receiver data provider.
 *
 * Invoking this method multiple times will chain the data providers and restrict the rows further.
 * @return a [VokDataProvider]; setting the [ConfigurableFilterDataProvider.setFilter] won't overwrite the filter specified in this method.
 */
@JvmName("withFilterAndConvert")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(other: Filter<T>) : VokDataProvider<T> =
    withVOKFilterAdapter().withFilter(other)

/**
 * Produces a new data provider with unremovable filter which restricts rows returned by the receiver data provider.
 * Allows you to write
 * expressions like this: `Person.dataProvider.withFilter { Person::age lt 25 }`
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 * @return a [VokDataProvider]; setting the [ConfigurableFilterDataProvider.setFilter] won't overwrite the filter specified in this method.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.withFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) : VokDataProvider<T> =
    withFilter(block(SqlWhereBuilder()))

@JvmName("andVaadin2")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) : VokDataProvider<T> =
    withVOKFilterAdapter().withFilter(block)

/**
 * Creates a filter programatically: `filter { Person::age lt 25 }`
 */
fun <T: Any> filter(block: SqlWhereBuilder<T>.()-> Filter<T>): Filter<T> = block(SqlWhereBuilder())

/**
 * Removes the original filter and sets the new filter. Allows you to write
 * expressions like this: `sqlDataProvider<Person>().setFilter { Person::age lt 25 }`.
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will overwrite the previous filter.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>.setFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) {
    setFilter(block(SqlWhereBuilder()))
}
