package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.kaributools.sort
import com.github.vokorm.KEntity
import com.github.vokorm.exp
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.OrderBy
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.vaadin.EntityDataProvider
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import kotlin.reflect.KProperty1

public val <E: KEntity<*>> Dao<E, *>.dataProvider: EntityDataProvider<E> get() = EntityDataProvider(this)

public inline fun <reified T> EntityDataProvider<T>.withStringFilterOn(prop: KProperty1<T, String>): DataProvider<T, String> =
    withStringFilterOn(prop.exp)
public fun <T> EntityDataProvider<T>.withStringFilterOn(prop: Property<*>): DataProvider<T, String> =
    withStringFilter { prop.likeIgnoreCase("$it%") }

private fun OrderBy.toQuerySortOrder() =
    QuerySortOrder(property.toExternalString(), if (order == OrderBy.Order.ASC) SortDirection.ASCENDING else SortDirection.DESCENDING)

public fun <T> Grid<T>.sort(criteria: List<OrderBy>) {
    sort(*criteria.toTypedArray())
}

/**
 * Forces a defined sort [criteria] for the columns in the Grid. Setting
 * empty list resets the ordering of all columns.
 * Columns not mentioned in the list are reset to the unsorted state.
 *
 * For Grids with multi-sorting, the index of a given column inside the list
 * defines the sort priority. For example, the column at index 0 of the list
 * is sorted first, then on the index 1, and so on.
 *
 * Exampe of usage:
 * ```
 * grid<Person> {
 *   addColumnFor(Person::name)
 *   sort(Person::name.asc)
 * }
 * ```
 */
public fun <T> Grid<T>.sort(vararg criteria: OrderBy) {
    sort(*criteria.map { it.toQuerySortOrder() } .toTypedArray())
}
