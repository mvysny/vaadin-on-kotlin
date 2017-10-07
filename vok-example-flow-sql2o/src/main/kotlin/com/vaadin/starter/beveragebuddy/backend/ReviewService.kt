package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.findById
import com.vaadin.starter.beveragebuddy.ui.converters.LocalDateToStringConverter
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple backend service to store and retrieve [Review] instances.
 */
object ReviewService {

    private val reviews = ConcurrentHashMap<Long, Review>()
    private val nextId = AtomicLong(0)

    init {
/*
        val r = Random()
        val reviewCount = 20 + r.nextInt(30)
        val beverages = StaticData.BEVERAGES.entries.toList()

        for (i in 0 until reviewCount) {
            val review = Review()
            val beverage = beverages[r.nextInt(StaticData.BEVERAGES.size)]
            val category = CategoryService.findCategoryOrThrow(beverage.value)
            review.name = beverage.key
            val testDay = LocalDate.of(1930 + r.nextInt(88),
                    1 + r.nextInt(12), 1 + r.nextInt(28))
            review.date = testDay
            review.score = 1 + r.nextInt(5)
            review.category = category
            review.count = 1 + r.nextInt(15)
            saveReview(review)
        }
*/
    }

    /**
     * Fetches the reviews matching the given filter text.
     *
     * The matching is case insensitive. When passed an empty filter text,
     * the method returns all categories. The returned list is ordered
     * by name.
     *
     * @param filter    the filter text
     * @return          the list of matching reviews
     */
    fun findReviews(filter: String): List<Review> {
        val normalizedFilter = filter.trim().toLowerCase()
        return reviews.values
                .filter { review -> filterTextOf(review).any { it.contains(normalizedFilter, ignoreCase = true) } }
                .sortedBy { it.id }
    }

    private fun filterTextOf(review: Review): List<String> {
        val dateConverter = LocalDateToStringConverter()
        return setOf(review.name,
                review.category.name,
                review.score.toString(),
                review.count.toString(),
                dateConverter.toPresentation(review.date)).filterNotNull()
    }

    /**
     * Deletes the given review from the review store.
     * @param review    the review to delete
     * @return  true if the operation was successful, otherwise false
     */
    fun deleteReview(review: Review): Boolean = reviews.remove(review.id) != null

    /**
     * Persists the given review into the review store.
     *
     * If the review is already persistent, the saved review will get updated
     * with the field values of the given review object.
     * If the review is new (i.e. its id is null), it will get a new unique id
     * before being saved.
     *
     * @param dto   the review to save
     */
    fun saveReview(dto: Review) {
        var entity: Review? = if (dto.id == null) null else reviews[dto.id!!]
        var category: Category = dto.category

            // The case when the category is new (not persisted yet, thus
            // has null id) is not handled here, because it can't currently
            // occur via the UI.
            // Note that Category.UNDEFINED also gets mapped to null.
        category = (if (category.id == null) null else Category.findById(category.id!!)) ?: Category.UNDEFINED
        if (entity == null) {
            // Make a copy to keep entities and DTOs separated
            entity = dto.copy()
            if (dto.id == null) {
                entity.id = nextId.incrementAndGet()
            }
            reviews.put(entity.id!!, entity)
        } else {
            entity.score = dto.score
            entity.name = dto.name
            entity.date = dto.date
            entity.count = dto.count
        }
        entity.category = category
    }

    fun get(id: Long): Review = reviews[id] ?: throw IllegalArgumentException("No review with id $id")
}
