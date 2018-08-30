@file:Suppress("UNCHECKED_CAST")

package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.FilterFactory
import com.github.vok.framework.flow.DefaultFilterFieldFactory
import com.github.vok.framework.flow.FilterFieldFactory
import com.github.vokorm.*
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasSize
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.HeaderRow
import com.vaadin.flow.data.binder.BeanPropertySet
import com.vaadin.flow.data.binder.PropertyDefinition
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
import kotlin.reflect.KClass
import kotlin.streams.*

/**
 * Produces filters defined by the `VoK-ORM` library. This will allow us to piggyback on the ability of `VoK-ORM` filters to produce
 * SQL92 WHERE clause. See [sqlDataProvider] and [Dao.dataProvider] for more details.
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
 * @param itemClass the item class as shown in the grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 * @param valueChangeMode how eagerly to apply the filtering after the user changes the filter value. Only applied to [HasValueChangeMode];
 * typically only applies to inline filter
 * components (most importantly [com.vaadin.flow.component.textfield.TextField]), typically ignored for popup components (such as [com.github.vok.framework.flow.NumberFilterPopup])
 * where the values are applied after the user clicks the "Apply" button. There are three values: EAGER - apply the value after every keystroke,
 * ON_CHANGE - apply the value after onblur or when the user presses the Enter button, ON_BLUR - apply the value when the focus leaves the component.
 * @return map mapping property ID to the filtering component generated
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> HeaderRow.generateFilterComponents(grid: Grid<T>,
                                                itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(itemClass.java,
                                                    { grid.dataProvider as VokDataProvider<T> },
                                                        SqlFilterFactory<T>()),
                                              valueChangeMode: ValueChangeMode = ValueChangeMode.EAGER
                                              ): Map<String, Component> {
    val properties: Map<String, PropertyDefinition<T, *>> = BeanPropertySet.get(itemClass.java).properties.toList().associateBy { it.name }
    val result = mutableMapOf<String, Component>()
    for (propertyId in grid.columns.mapNotNull { it.key }) {
        val property = properties[propertyId]
        val field: HasValue<*, *>? = if (property == null) null else filterFieldFactory.createField(property)
        if (field != null) {
            (field as? HasValueChangeMode)?.valueChangeMode = valueChangeMode
            filterFieldFactory.bind(field as HasValue<HasValue.ValueChangeEvent<Any?>, Any?>, property!! as PropertyDefinition<T, Any?>)
            getCell(grid.getColumnByKey(propertyId)).setComponent(field as Component)
            result[propertyId] = field as Component
            (field as? HasSize)?.width = "100%"
        }
    }
    return result
}
