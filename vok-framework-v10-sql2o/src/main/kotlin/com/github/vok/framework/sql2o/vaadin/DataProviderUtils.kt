package com.github.vok.framework.sql2o.vaadin

import com.github.vokorm.Filter
import com.github.vokorm.SqlWhereBuilder
import com.github.vokorm.and
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.function.SerializablePredicate

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return data provider which can be configured to always apply given filter.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.withConfigurableFilter2() : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withConfigurableFilter({ f1: Filter<T>?, f2: Filter<T>? -> listOf(f1, f2).filterNotNull().toSet().and() })

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will chain the data providers and restrict the rows further.
 * @param other applies this filter
 */
fun <T: Any> ConfigurableFilterDataProvider<T, in Filter<T>?, Filter<T>?>.withFilter(other: Filter<T>) : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withConfigurableFilter2().apply {
        // wrap the current DP so that we won't change the filter
        setFilter(other)
        // wrap the DP again so that nobody will change our filter.
    }.withConfigurableFilter2()

// since SerializablePredicate is Vaadin-built-in interface which our Filters do not use, since Vaadin 8 has different SerializablePredicate class
// than Vaadin 10 and we would have to duplicate filters just because a dumb interface.
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withVOKFilterAdapter() : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withConvertedFilter<Filter<T>?> {
        f -> if (f == null) null else SerializablePredicate { f.test(it) }
    }.withConfigurableFilter2()

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will chain the data providers and restrict the rows further.
 * @param other applies this filter
 * @return a [ConfigurableFilterDataProvider]; setting the [ConfigurableFilterDataProvider.setFilter] won't overwrite the filter specified in this method.
 */
@JvmName("withFilterAndConvert")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(other: Filter<T>) : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withVOKFilterAdapter().withFilter(other)

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter. Allows you to write
 * expressions like this: `Person.dataProvider.withFilter { Person::age lt 25 }`
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.withFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withConfigurableFilter2().withFilter(block(SqlWhereBuilder()))

@JvmName("andVaadin2")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
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
