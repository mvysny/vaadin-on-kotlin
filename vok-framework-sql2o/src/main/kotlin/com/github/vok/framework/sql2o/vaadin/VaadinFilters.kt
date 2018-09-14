@file:Suppress("UNCHECKED_CAST")

package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.*
import com.github.vokorm.*
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.ui.Grid
import com.vaadin.ui.HasValueChangeMode
import com.vaadin.ui.components.grid.HeaderRow
import kotlin.reflect.KClass

/**
 * Produces filters defined by the `VoK-ORM` library. This will allow us to piggyback on the ability of `VoK-ORM` filters to produce
 * SQL92 WHERE clause. See [sqlDataProvider] and [entityDataProvider] for more details.
 * @param clazz the type of the entity, not null.
 */
class SqlFilterFactory<T: Any>(val clazz: Class<T>) : FilterFactory<Filter<T>> {
    private val String.dbColumnName: String get() = clazz.entityMeta.getProperty(this).dbColumnName
    override fun and(filters: Set<Filter<T>>) = filters.and()
    override fun or(filters: Set<Filter<T>>) = filters.or()
    override fun eq(propertyName: String, value: Any?) = EqFilter<T>(propertyName.dbColumnName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<T>(propertyName.dbColumnName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<T>(propertyName.dbColumnName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<T>(propertyName.dbColumnName, value)
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
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(SqlFilterFactory<T>(itemClass.java)),
                                                valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY): FilterRow<T, Filter<T>> {
    return generateFilterComponents(grid, itemClass, SqlFilterFactory<T>(itemClass.java), filterFieldFactory, valueChangeMode)
}
