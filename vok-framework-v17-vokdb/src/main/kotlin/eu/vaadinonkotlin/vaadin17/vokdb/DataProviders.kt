package eu.vaadinonkotlin.vaadin17.vokdb

import com.github.mvysny.vokdataloader.DataLoader
import com.github.vokorm.KEntity
import eu.vaadinonkotlin.vaadin10.DataLoaderAdapter
import eu.vaadinonkotlin.vaadin10.vokdb.asDataProvider
import com.vaadin.flow.data.provider.HasDataView
import com.vaadin.flow.data.provider.DataView
import eu.vaadinonkotlin.vaadin10.asDataProvider

/**
 * Sets given data loader to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
public fun <T: KEntity<*>, V: DataView<T>> HasDataView<T, V>.setDataLoader(dataLoader: DataLoader<T>) {
    setItems(dataLoader.asDataProvider())
}

/**
 * Sets given [dataLoader] to this [Grid], by the means of wrapping the [dataLoader] via [DataLoaderAdapter] and setting it
 * as a (configurable) [Grid.getDataProvider].
 * @param idResolver provides unique ID for every item. The ID is then used to differentiate items.
 * See [com.vaadin.flow.data.provider.DataProvider.getId] for more details. Typically every item
 * has a primary key of type [Long], but any Java/Kotlin object with properly written [Any.equals] and [Any.hashCode] can act as the ID,
 * including the item itself.
 */
public fun <T: Any, V: DataView<T>> HasDataView<T, V>.setDataLoader(dataLoader: DataLoader<T>, idResolver: (T)->Any) {
    setItems(dataLoader.asDataProvider(idResolver))
}
