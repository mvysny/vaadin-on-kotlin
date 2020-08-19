package eu.vaadinonkotlin.vaadin17.vokdb

import com.github.mvysny.vokdataloader.DataLoader
import com.github.vokorm.KEntity
import eu.vaadinonkotlin.vaadin10.DataLoaderAdapter
import eu.vaadinonkotlin.vaadin10.vokdb.asDataProvider
import com.vaadin.flow.data.provider.HasDataView
import com.vaadin.flow.data.provider.DataView
/**
 * Sets given data loader to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
public fun <T: KEntity<*>, V: DataView<T>> HasDataView<T, V>.setDataLoader(dataLoader: DataLoader<T>) {
    setItems(dataLoader.asDataProvider())
}
