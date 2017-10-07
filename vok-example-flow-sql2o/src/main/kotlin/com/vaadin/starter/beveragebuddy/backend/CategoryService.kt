package com.vaadin.starter.beveragebuddy.backend

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple backend service to store and retrieve [Category] instances.
 */
object CategoryService {

    private val categories = ConcurrentHashMap<Long, Category>()

    /**
     * Fetches the categories whose name matches the given filter text.
     *
     * The matching is case insensitive. When passed an empty filter text,
     * the method returns all categories. The returned list is ordered
     * by name.
     *
     * @param filter    the filter text
     * @return          the list of matching categories
     */
    fun findCategories(filter: String): List<Category> {
        val normalizedFilter = filter.trim()
        // Make a copy of each matching item to keep entities and DTOs separated
        return categories.values
                .filter { it.name.contains(normalizedFilter, ignoreCase = true) }
                .map { it.copy() }
                .sortedBy { it.name }
    }

    /**
     * Searches for the exact category whose name matches the given [filter] text.
     *
     * The matching is substring-based and case insensitive.
     * @return the category or null.
     * @throws IllegalStateException    if the result is ambiguous
     */
    fun findCategoryByName(filter: String): Category? {
        val categoriesMatching = findCategories(filter)
        check(categoriesMatching.size <= 1) { "Category $filter is ambiguous" }
        return categoriesMatching.firstOrNull()
    }

    /**
     * Fetches the exact category whose name matches the given [filter] text.
     *
     * Behaves like [findCategoryByName], except that returns
     * a [Category] instead of an [Optional]. If the category
     * can't be identified, an exception is thrown.
     *
     * @return      the category, if found
     * @throws IllegalStateException    if not exactly one category matches the given name
     */
    fun findCategoryOrThrow(filter: String): Category = findCategoryByName(filter) ?: throw IllegalStateException("Category matching $filter does not exist")
}
