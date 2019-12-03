@file:Suppress("UNCHECKED_CAST")

package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.vokdataloader.*
import com.vaadin.flow.component.HasValue
import eu.vaadinonkotlin.FilterFactory
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.HeaderRow
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.PropertyDefinition
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
import kotlin.reflect.KClass

/**
 * Produces filters defined by the `VoK-DataLoader` library. This will allow Vaadin Grid to piggyback on the DataLoader API
 * which provides access to REST (via the `vok-rest-client` module), the ability to use `VoK-ORM` to access SQL databases
 * (via the `vok-db` module). See the `vok-framework-v10-vokdb`'s `sqlDataProvider()` function and the `Dao.dataProvider` for more details.
 * @param clazz the type of the entity, not null.
 */
class DataLoaderFilterFactory<F : Any>(val clazz: Class<F>) : FilterFactory<Filter<F>> {
    override fun and(filters: Set<Filter<F>>) = filters.and()
    override fun or(filters: Set<Filter<F>>) = filters.or()
    override fun eq(propertyName: String, value: Any?) = EqFilter<F>(propertyName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<F>(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<F>(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<F>(propertyName, value)
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 *
 * This function uses [Filter] hierarchy from `vok-dataloader` which is what you often want to do. If not,
 * use [generateFilterComponents] instead.
 * @param grid the owner grid.
 * @param itemClass the item class as shown in the grid.
 * @param filterFieldFactory used to create the filters themselves. By default the [DefaultFilterFieldFactory] is used.
 * @param valueChangeMode how eagerly to apply the filtering after the user changes the filter value. Only applied to [HasValueChangeMode];
 * typically only applies to inline filter
 * components (most importantly [TextField]), typically ignored for popup components (such as [com.github.vok.framework.flow.NumberFilterPopup])
 * where the values are applied after the user clicks the "Apply" button. There are three values: EAGER - apply the value after every keystroke,
 * ON_CHANGE - apply the value after onblur or when the user presses the Enter button, ON_BLUR - apply the value when the focus leaves the component.
 * @param componentConfigurator invoked for every filter component when created. By default every component
 * is set to 100% and [TextField.isClearButtonVisible]/[ComboBox.isClearButtonVisible] is set to true, and by default this closure
 * will do nothing.
 * @return the [FilterRow] row with filter components already generated.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> HeaderRow.generateFilterComponents(
        grid: Grid<T>,
        itemClass: KClass<T>,
        filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(itemClass.java, DataLoaderFilterFactory(itemClass.java)),
        valueChangeMode: ValueChangeMode = ValueChangeMode.EAGER,
        componentConfigurator: (filterComponent: HasValue<*, *>, property: PropertyDefinition<T, *>) -> Unit = { _, _ -> }
): FilterRow<T, Filter<T>> {
    return generateFilterComponents2(grid, itemClass, DataLoaderFilterFactory(itemClass.java),
            filterFieldFactory, valueChangeMode, componentConfigurator)
}
