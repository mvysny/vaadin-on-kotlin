package eu.vaadinonkotlin.vaadin

import com.github.mvysny.vokdataloader.*
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.provider.*
import kotlin.reflect.KProperty1

/**
 * A data provider with configurable filter.
 * Usually [FilterBar] will call [ConfigurableFilterDataProvider.setFilter] to set the final filter.
 * If you want an unremovable filter, use [DataLoader.withFilter] or
 * [FilterBar.setCustomFilter].
 */
public interface VokDataProvider<T : Any> :
    ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?>,
    BackEndDataProvider<T, Filter<T>?>

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [ILikeFilter] to the receiver.
 */
public fun <T : Any> DataLoader<T>.withStringFilterOn(
    property: KProperty1<T, String?>,
    idResolver: (T) -> Any
): BackEndDataProvider<T, String?> =
    withStringFilterOn(property.name, idResolver)

/**
 * Creates a data provider which performs string filtering on given [property]. Ideal for [ComboBox] which lazily
 * filters items as the user types in search phrase. Emits [ILikeFilter] to the receiver.
 */
public fun <T : Any> DataLoader<T>.withStringFilterOn(property: DataLoaderPropertyName, idResolver: (T) -> Any): BackEndDataProvider<T, String?> {
    val dp: VokDataProvider<T> = asDataProvider(idResolver)
    return object : BackEndDataProviderWrapper<T, String?, Filter<T>?>(dp) {
        override fun getFilter(query: Query<T, String?>): Filter<T>? {
            val filter: String? = query.filter.orElse(null)
            return if (filter.isNullOrBlank()) null else StartsWithFilter(property, filter.trim())
        }
    }
}

internal abstract class BackEndDataProviderWrapper<T, F, M>(private val delegate: BackEndDataProvider<T, M>) :
    DataProviderWrapper<T, F, M>(delegate), BackEndDataProvider<T, F> {

    override fun isInMemory(): Boolean = delegate.isInMemory

    override fun setSortOrders(sortOrders: MutableList<QuerySortOrder>) {
        delegate.setSortOrders(sortOrders)
    }
}
