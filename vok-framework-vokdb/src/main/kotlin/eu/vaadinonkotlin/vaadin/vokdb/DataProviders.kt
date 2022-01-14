package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.kaributools.sort
import com.github.mvysny.vokdataloader.DataLoader
import com.github.mvysny.vokdataloader.DataLoaderPropertyName
import com.github.mvysny.vokdataloader.SortClause
import com.github.vokorm.KEntity
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.binder.HasDataProvider
import com.vaadin.flow.data.provider.BackEndDataProvider
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.SortDirection
import eu.vaadinonkotlin.vaadin.DataLoaderAdapter
import eu.vaadinonkotlin.vaadin.VokDataProvider
import kotlin.reflect.KProperty1
import eu.vaadinonkotlin.vaadin.withStringFilterOn as withStringFilterOn1

/**
 * Sets given data loader to this Grid, by the means of wrapping the data loader via [DataLoaderAdapter] and setting it
 * as a (configurable) [Grid.getDataProvider].
 */
public fun <T: KEntity<*>> HasDataProvider<T>.setDataLoader(dataLoader: DataLoader<T>) {
    setDataProvider(dataLoader.asDataProvider())
}

/**
 * Returns a [VokDataProvider] which loads data from this [DataLoader].
 */
public fun <T: KEntity<*>> DataLoader<T>.asDataProvider(): VokDataProvider<T> = DataLoaderAdapter(this) { it.id!! }

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [com.github.mvysny.vokdataloader.StartsWithFilter] to the receiver.
 */
public fun <T : KEntity<*>> DataLoader<T>.withStringFilterOn(property: KProperty1<T, String?>): BackEndDataProvider<T, String?> =
        withStringFilterOn(property.name)

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [com.github.mvysny.vokdataloader.StartsWithFilter] to the receiver.
 */
public fun <T : KEntity<*>> DataLoader<T>.withStringFilterOn(property: DataLoaderPropertyName): BackEndDataProvider<T, String?> =
        withStringFilterOn1(property) { it.id!! }

private fun SortClause.toQuerySortOrder() =
    QuerySortOrder(propertyName, if (asc) SortDirection.ASCENDING else SortDirection.DESCENDING)

public fun <T> Grid<T>.sort(criteria: List<SortClause>) {
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
public fun <T> Grid<T>.sort(vararg criteria: SortClause) {
    sort(*criteria.map { it.toQuerySortOrder() } .toTypedArray())
}
