package com.github.vok.framework.vaadin

import com.github.vok.framework.*
import com.github.vok.karibudsl.*
import com.vaadin.data.*
import com.vaadin.data.provider.ConfigurableFilterDataProvider
import com.vaadin.server.Page
import com.vaadin.server.Resource
import com.vaadin.shared.ui.ValueChangeMode
import com.vaadin.shared.ui.datefield.DateTimeResolution
import com.vaadin.ui.*
import com.vaadin.ui.components.grid.HeaderRow
import java.io.Serializable
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass
import kotlin.streams.toList

@Suppress("UNCHECKED_CAST")
class JPAFilterFactory : FilterFactory<JPAFilter> {
    override fun and(filters: Set<JPAFilter>) = filters.and()
    override fun or(filters: Set<JPAFilter>) = filters.or()
    override fun eq(propertyName: String, value: Any) = EqFilter(propertyName, value as Serializable?)
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
                                                filterFieldFactory: FilterFieldFactory<T, JPAFilter> = DefaultFilterFieldFactory(itemClass.java,
                                                        JPAFilterFactory()),
                                                valueChangeMode: ValueChangeMode = ValueChangeMode.LAZY): FilterRow<T, JPAFilter> {
    val filterRow = FilterRow(grid, itemClass, this, filterFieldFactory)
    filterRow.generateFilterComponents(valueChangeMode)
    return filterRow
}
