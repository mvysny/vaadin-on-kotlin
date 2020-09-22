package eu.vaadinonkotlin.vaadin10.vokdb

import com.github.mvysny.vokdataloader.DataLoader
import com.github.mvysny.vokdataloader.DataLoaderPropertyName
import com.github.vokorm.KEntity
import com.github.vokorm.dataloader.EntityDataLoader
import com.gitlab.mvysny.jdbiorm.Dao
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.binder.HasDataProvider
import com.vaadin.flow.data.provider.DataProvider
import eu.vaadinonkotlin.vaadin10.DataLoaderAdapter
import eu.vaadinonkotlin.vaadin10.VokDataProvider
import kotlin.reflect.KProperty1
import eu.vaadinonkotlin.vaadin10.withStringFilterOn as withStringFilterOn1

/**
 * Provides instances of this entity from a database. Does not support joins on any of the like; supports filtering
 * and sorting. Only supports simple views over one database table (one entity) - for anything more complex please use [sqlDataProvider].
 *
 * Example of use: `grid.setDataLoader(Person.dataLoader)`.
 */
public inline val <reified T: KEntity<*>> Dao<T, *>.dataLoader: DataLoader<T>
    get() = EntityDataLoader(this)

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
public fun <T : KEntity<*>> DataLoader<T>.withStringFilterOn(property: KProperty1<T, String?>): DataProvider<T, String?> =
        withStringFilterOn(property.name)

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [com.github.mvysny.vokdataloader.StartsWithFilter] to the receiver.
 */
public fun <T : KEntity<*>> DataLoader<T>.withStringFilterOn(property: DataLoaderPropertyName): DataProvider<T, String?> =
        withStringFilterOn1(property) { it.id!! }
