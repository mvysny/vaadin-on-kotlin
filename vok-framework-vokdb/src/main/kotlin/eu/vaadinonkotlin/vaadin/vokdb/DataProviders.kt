package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.kaributools.sort
import com.github.vokorm.KEntity
import com.github.vokorm.exp
import com.gitlab.mvysny.jdbiorm.Dao
import com.gitlab.mvysny.jdbiorm.OrderBy
import com.gitlab.mvysny.jdbiorm.Property
import com.gitlab.mvysny.jdbiorm.condition.Expression
import com.gitlab.mvysny.jdbiorm.vaadin.EntityDataProvider
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import kotlin.reflect.KProperty1

/**
 * Creates a new [EntityDataProvider] for this entity. Every time you poll this property you'll
 * get a new instance, with their separate customizable [EntityDataProvider.setFilter].
 */
public val <E: KEntity<*>> Dao<E, *>.dataProvider: EntityDataProvider<E> get() = EntityDataProvider(this)

public inline fun <reified T> EntityDataProvider<T>.withStringFilterOn(prop: KProperty1<T, String>): DataProvider<T, String> =
    withStringFilterOn(prop.exp)

/**
 * Allows this data provider to be set to a Vaadin component which performs String-based
 * filtering, e.g. ComboBox. When the user types in something in
 * hopes to filter the items in the dropdown, a [Expression.likeIgnoreCase] Condition is created from the user input and
 * `%` is appended at the end, meaning that the filter passes all rows where [prop] starts with given user input.
 *
 * If you need to create a different condition, call [EntityDataProvider.withStringFilter] directly.
 */
public fun <T> EntityDataProvider<T>.withStringFilterOn(prop: Expression<*>): DataProvider<T, String> =
    withStringFilter { prop.likeIgnoreCase("${it.trim()}%") }

private fun OrderBy.toQuerySortOrder() =
    QuerySortOrder(property.toExternalString(), if (order == OrderBy.Order.ASC) SortDirection.ASCENDING else SortDirection.DESCENDING)

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
 *   sort(listOf(Person::name.asc))
 * }
 * ```
 */
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
