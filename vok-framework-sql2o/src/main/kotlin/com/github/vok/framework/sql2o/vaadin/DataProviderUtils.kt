package com.github.vok.framework.sql2o.vaadin

import com.github.vokorm.Filter
import com.github.vokorm.SqlWhereBuilder
import com.github.vokorm.and
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.data.provider.DataProvider
import com.vaadin.server.SerializablePredicate

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return data provider which can be configured to always apply given filter.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.configurableFilter() : ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> =
    withConfigurableFilter({ f1: Filter<T>?, f2: Filter<T>? -> listOfNotNull(f1, f2).toSet().and() })

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param other applies this filter
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.and(other: Filter<T>) : DataProvider<T, Filter<T>?> = configurableFilter().apply {
    setFilter(other)
}
// since SerializablePredicate is Vaadin-built-in interface which our Filters do not use, since Vaadin 8 has different SerializablePredicate class
// than Vaadin 10 and we would have to duplicate filters just because a dumb interface.
private fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.adaptToVOKFilters() : DataProvider<T, Filter<T>?> = withConvertedFilter<Filter<T>?> {
        f -> if (f == null) null else SerializablePredicate { f.test(it) }
}
@JvmName("andVaadin")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.and(other: Filter<T>) : DataProvider<T, Filter<T>?> = adaptToVOKFilters().and(other)

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter. Allows you to write
 * expressions like this: `sqlDataProvider<Person>().and { Person::age lt 25 }`
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> DataProvider<T, in Filter<T>?>.and(block: SqlWhereBuilder<T>.()-> Filter<T>) : DataProvider<T, Filter<T>?> = configurableFilter().apply {
    setFilter(block)
}
@JvmName("andVaadin2")
fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.and(block: SqlWhereBuilder<T>.()-> Filter<T>) : DataProvider<T, Filter<T>?> = adaptToVOKFilters().and(block)

/**
 * Creates a filter programmatically: `filter { Person::age lt 25 }`
 */
fun <T: Any> filter(block: SqlWhereBuilder<T>.()-> Filter<T>): Filter<T> = block(SqlWhereBuilder())

/**
 * Removes the original filter and sets the new filter. Allows you to write
 * expressions like this: `sqlDataProvider<Person>().and { Person::age lt 25 }`.
 * See [SqlWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will overwrite the previous filter.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T: Any> ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>.setFilter(block: SqlWhereBuilder<T>.()-> Filter<T>) {
    setFilter(block(SqlWhereBuilder()))
}
