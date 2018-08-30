@file:Suppress("UNCHECKED_CAST")

package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.*
import com.github.vokorm.*
import com.vaadin.data.BeanPropertySet
import com.vaadin.data.HasValue
import com.vaadin.data.PropertyDefinition
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.server.SerializableConsumer
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.ui.Component
import com.vaadin.ui.Grid
import com.vaadin.ui.HasValueChangeMode
import com.vaadin.ui.components.grid.HeaderRow
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * Produces filters defined by the `VoK-ORM` library. This will allow us to piggyback on the ability of `VoK-ORM` filters to produce
 * SQL92 WHERE clause. See [SqlDataProvider] and [EntityDataProvider] for more details.
 */
class SqlFilterFactory<T: Any> : FilterFactory<Filter<T>> {
    override fun and(filters: Set<Filter<T>>) = filters.and()
    override fun or(filters: Set<Filter<T>>) = filters.or()
    override fun eq(propertyName: String, value: Any) = EqFilter<T>(propertyName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<T>(propertyName, value)
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 * @param T the type of items in the grid.
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 * @param valueChangeMode how eagerly to apply the filtering after the user changes the filter value. Only applied to [HasValueChangeMode];
 * typically only applies to inline filter
 * components (most importantly [com.vaadin.ui.TextField]), typically ignored for popup components (such as [com.github.vok.framework.NumberFilterPopup])
 * where the values are applied after the user clicks the "Apply" button. Defaults to [ValueChangeMode.LAZY].
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> HeaderRow.generateFilterComponents(grid: Grid<T>, itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(itemClass.java, SqlFilterFactory<T>()),
                                                valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY): FilterRow<T, Filter<T>> {
    val filterRow = FilterRow(grid, itemClass, this, filterFieldFactory)
    filterRow.generateFilterComponents(valueChangeMode)
    return filterRow
}
