@file:Suppress("UNCHECKED_CAST")

package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.vokdataloader.*
import com.vaadin.data.HasValue
import com.vaadin.data.PropertyDefinition
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.ui.Grid
import com.vaadin.ui.HasValueChangeMode
import com.vaadin.ui.components.grid.HeaderRow
import eu.vaadinonkotlin.FilterFactory
import kotlin.reflect.KClass

/**
 * Produces filters defined by the `VoK-DataLoader` library. This will allow Vaadin Grid to piggyback on the DataLoader API
 * which provides access to REST (via the `vok-rest-client` module), the ability to use `VoK-ORM` to access SQL databases
 * (via the `vok-db` module). See the `vok-framework-v10-vokdb`'s `sqlDataProvider()` function and the `Dao.dataProvider` for more details.
 * @param clazz the type of the entity, not null.
 */
public open class DataLoaderFilterFactory<F : Any>(public val clazz: Class<F>) : FilterFactory<Filter<F>> {
    override fun and(filters: Set<Filter<F>>): Filter<F>? = filters.and()
    override fun or(filters: Set<Filter<F>>): Filter<F>? = filters.or()
    override fun eq(propertyName: String, value: Any?): Filter<F> = EqFilter(propertyName, value)
    override fun le(propertyName: String, value: Any): Filter<F> = OpFilter(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any): Filter<F> = OpFilter(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String): Filter<F> = ILikeFilter(propertyName, value)
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 *
 * This function automatically generates `vok-dataloader` [Filter]s and this is what you typically want to use (since you're
 * using [VokDataProvider]).
 * @param T the type of items in the grid.
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. By default the [DefaultFilterFieldFactory] is used.
 * @param valueChangeMode how eagerly to apply the filtering after the user changes the filter value. Only applied to [HasValueChangeMode];
 * typically only applies to inline filter
 * components (most importantly [com.vaadin.ui.TextField]), typically ignored for popup components (such as [com.github.vok.framework.NumberFilterPopup])
 * where the values are applied after the user clicks the "Apply" button. Defaults to [ValueChangeMode.LAZY].
 * @param componentConfigurator invoked for every filter component when created. By default every component
 * is set to 100%, and by default this closure
 * will do nothing.
 * @return the [FilterRow] row with filter components already generated.
 */
@Suppress("UNCHECKED_CAST")
public fun <T : Any> HeaderRow.generateFilterComponents(
        grid: Grid<T>, itemClass: KClass<T>,
        filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(DataLoaderFilterFactory<T>(itemClass.java)),
        valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY,
        componentConfigurator: (filterComponent: HasValue<*>, property: PropertyDefinition<T, *>) -> Unit = { _, _ -> }
): FilterRow<T, Filter<T>> {
    return generateFilterComponents(grid, itemClass, DataLoaderFilterFactory<T>(itemClass.java),
            filterFieldFactory, valueChangeMode, componentConfigurator)
}
