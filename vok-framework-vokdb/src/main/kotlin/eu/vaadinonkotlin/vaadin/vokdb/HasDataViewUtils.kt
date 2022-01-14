package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.vokdataloader.DataLoader
import com.github.mvysny.vokdataloader.Filter
import com.github.vokorm.KEntity
import com.vaadin.flow.data.provider.*
import eu.vaadinonkotlin.vaadin.DataLoaderAdapter

/**
 * Sets given data loader to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
public fun <T: KEntity<*>, V: DataView<T>> HasDataView<T, Filter<T>, V>.setDataLoader(dataLoader: DataLoader<T>): V =
    setItems(dataLoader.asDataProvider())

/**
 * Sets given [dataLoader] to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
@JvmName("setDataLoader2")
public fun <T: KEntity<*>, V: DataView<T>> HasDataView<T, Void, V>.setDataLoader(dataLoader: DataLoader<T>): V {
    // this cast is okay since HasDataView with Void filter type will never try to set any filters to the DataProvider.
    @Suppress("UNCHECKED_CAST")
    val dataProvider: DataProvider<T, Void> = dataLoader.asDataProvider() as DataProvider<T, Void>
    return setItems(dataProvider)
}

/**
 * Sets given data loader to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
public fun <T: KEntity<*>, V: LazyDataView<T>> HasLazyDataView<T, Filter<T>, V>.setLazyDataLoader(dataLoader: DataLoader<T>): V =
    setItems(dataLoader.asDataProvider())

/**
 * Sets given [dataLoader] to this Grid, by the means of wrapping the data loader
 * via [DataLoaderAdapter] and setting it
 * as a (configurable) data provider.
 */
@JvmName("setDataLoader2")
public fun <T: KEntity<*>, V: LazyDataView<T>> HasLazyDataView<T, Void, V>.setLazyDataLoader(dataLoader: DataLoader<T>): V {
    // this cast is okay since HasDataView with Void filter type will never try to set any filters to the DataProvider.
    @Suppress("UNCHECKED_CAST")
    val dataProvider: BackEndDataProvider<T, Void> = dataLoader.asDataProvider() as BackEndDataProvider<T, Void>
    return setItems(dataProvider)
}
