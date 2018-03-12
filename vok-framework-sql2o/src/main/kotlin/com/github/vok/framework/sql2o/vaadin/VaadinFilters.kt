@file:Suppress("UNCHECKED_CAST")

package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.DefaultFilterFieldFactory
import com.github.vok.framework.FilterFactory
import com.github.vok.framework.FilterFieldFactory
import com.github.vokorm.*
import com.vaadin.data.BeanPropertySet
import com.vaadin.data.HasValue
import com.vaadin.data.PropertyDefinition
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.ui.Component
import com.vaadin.ui.Grid
import com.vaadin.ui.components.grid.HeaderRow
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
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> HeaderRow.generateFilterComponents(grid: Grid<T>, itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(itemClass.java,
                                                    { grid.dataProvider as ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> },
                                                        SqlFilterFactory<T>())) {
    val properties: Map<String, PropertyDefinition<T, *>> = BeanPropertySet.get(itemClass.java).properties.toList().associateBy { it.name }
    for (propertyId in grid.columns.mapNotNull { it.id }) {
        val property = properties[propertyId]
        val field: HasValue<*>? = if (property == null) null else filterFieldFactory.createField(property)
        val cell = getCell(propertyId)
        if (field == null) {
            cell.text = ""  // this also removes the cell from the row
        } else {
            filterFieldFactory.bind(field as HasValue<Any?>, property!! as PropertyDefinition<T, Any?>)
            cell.component = field as Component
        }
    }
}
