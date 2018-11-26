@file:Suppress("UNCHECKED_CAST")

package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.vokdataloader.*
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.ui.Grid
import com.vaadin.ui.HasValueChangeMode
import com.vaadin.ui.components.grid.HeaderRow
import eu.vaadinonkotlin.FilterFactory
import kotlin.reflect.KClass

/**
 * Produces filters defined by the `VoK-ORM` library. This will allow us to piggyback on the ability of `VoK-ORM` filters to produce
 * SQL92 WHERE clause. See [sqlDataProvider] and [entityDataProvider] for more details.
 * @param clazz the type of the entity, not null.
 */
class DataLoaderFilterFactory<T: Any>(val clazz: Class<T>) : FilterFactory<Filter<T>> {
    override fun and(filters: Set<Filter<T>>) = filters.and()
    override fun or(filters: Set<Filter<T>>) = filters.or()
    override fun eq(propertyName: String, value: Any?) = EqFilter<T>(propertyName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<T>(propertyName, value)
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 *
 * This function automatically generates `vok-dataloader` [Filter]s and this is what you typically want to use (since you're
 * using [VokDataProvider]).
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
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(DataLoaderFilterFactory<T>(itemClass.java)),
                                                valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY): FilterRow<T, Filter<T>> {
    return generateFilterComponents(grid, itemClass, DataLoaderFilterFactory<T>(itemClass.java), filterFieldFactory, valueChangeMode)
}
