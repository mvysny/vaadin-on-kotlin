package eu.vaadinonkotlin.vaadin

import com.github.mvysny.vokdataloader.*
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.provider.DataProvider
import kotlin.reflect.KProperty1

/**
 * A data provider with configurable filter.
 * Usually [FilterBar] will call [ConfigurableFilterDataProvider.setFilter] to set the final filter.
 * If you want an unremovable filter use [DataLoader.withFilter] or
 * [FilterBar.setCustomFilter].
 */
public typealias VokDataProvider<T> = ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [ILikeFilter] to the receiver.
 */
public fun <T : Any> DataLoader<T>.withStringFilterOn(property: KProperty1<T, String?>, idResolver: (T) -> Any): DataProvider<T, String?> =
        withStringFilterOn(property.name, idResolver)

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [ILikeFilter] to the receiver.
 */
public fun <T : Any> DataLoader<T>.withStringFilterOn(property: DataLoaderPropertyName, idResolver: (T) -> Any): DataProvider<T, String?> =
        asDataProvider(idResolver).withConvertedFilter<String> { filter: String? ->
            if (filter.isNullOrBlank()) null else StartsWithFilter(property, filter.trim())
        }
