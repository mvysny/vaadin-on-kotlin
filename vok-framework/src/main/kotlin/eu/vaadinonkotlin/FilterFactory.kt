package eu.vaadinonkotlin

import java.io.Serializable

/**
 * Used by filter components (such as NumberInterval) to create actual filter objects. The filters are expected to have properly
 * implemented [Any.equals], [Any.hashCode] and [Any.toString], to allow for filter expression simplification (e.g. to remove non-unique
 * filters from AND or OR expressions).
 *
 * The filter objects produced by this factory will be passed into Vaadin's DataProvider in the Query object.
 *
 * @param F the type of the filter objects. Every database access type or DataProvider implementation may have different filters;
 * for example VoK-ORM defines its own hierarchy of filters.
 */
interface FilterFactory<F> : Serializable {
    /**
     * ANDs given set of filters and returns a new filter.
     * @param filters set of filters, may be empty.
     * @return a filter which ANDs given filter set; returns `null` when the filter set is empty.
     */
    fun and(filters: Set<F>): F?
    /**
     * ORs given set of filters and returns a new filter.
     * @param filters set of filters, may be empty.
     * @return a filter which ORs given filter set; returns `null` when the filter set is empty.
     */
    fun or(filters: Set<F>): F?
    /**
     * Creates a filter which matches the value of given [propertyName] to given [value].
     */
    fun eq(propertyName: String, value: Any?): F
    /**
     * Creates a filter which accepts only such values of given [propertyName] which are less than or equal to given [value].
     */
    fun le(propertyName: String, value: Any): F
    /**
     * Creates a filter which accepts only such values of given [propertyName] which are greater than or equal to given [value].
     */
    fun ge(propertyName: String, value: Any): F
    /**
     * Creates a filter which performs a case-insensitive substring matching of given [propertyName] to given [value].
     * @param value not empty; matching rows must contain this string. To emit SQL LIKE statement you need to prepend and append '%' to this string.
     */
    fun ilike(propertyName: String, value: String): F

    /**
     * Creates a filter which accepts only such values of given [propertyName] which are greater than or equal to given [min] and less than or equal to given [max].
     */
    fun between(propertyName: String, min: Any, max: Any): F = when {
        min == max -> eq(propertyName, min)
        else -> and(setOf(ge(propertyName, min), le(propertyName, max)))!!
    }
}
