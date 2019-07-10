package eu.vaadinonkotlin.vaadin8.jpa

import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.ui.Grid
import com.vaadin.ui.components.grid.HeaderRow
import eu.vaadinonkotlin.FilterFactory
import eu.vaadinonkotlin.vaadin8.DefaultFilterFieldFactory
import eu.vaadinonkotlin.vaadin8.FilterFieldFactory
import eu.vaadinonkotlin.vaadin8.FilterRow
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class JPAFilterFactory : FilterFactory<JPAFilter> {
    override fun and(filters: Set<JPAFilter>) = filters.and()
    override fun or(filters: Set<JPAFilter>) = filters.or()
    override fun eq(propertyName: String, value: Any?) = EqFilter(propertyName, value as Serializable?)
    override fun le(propertyName: String, value: Any) = Le2Filter(propertyName, value as Comparable<Any>)
    override fun ge(propertyName: String, value: Any) = Ge2Filter(propertyName, value as Comparable<Any>)
    override fun ilike(propertyName: String, value: String) = ILikeFilter(propertyName, "%$value%")
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> HeaderRow.generateFilterComponents(grid: Grid<T>, itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T, JPAFilter> = DefaultFilterFieldFactory(JPAFilterFactory()),
                                                valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY): FilterRow<T, JPAFilter> {
    val filterRow = FilterRow(grid, itemClass.java, this, filterFieldFactory, JPAFilterFactory())
    filterRow.generateFilterComponents(valueChangeMode)
    return filterRow
}
