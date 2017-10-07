package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.get
import com.vaadin.starter.beveragebuddy.ui.converters.LocalDateToStringConverter
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple backend service to store and retrieve [Review] instances.
 */
object ReviewService {


    init {
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
    fun findReviews(filter: String): List<ReviewWithCategory> {
//        val normalizedFilter = filter.trim().toLowerCase()
//        return reviews.values
//                .filter { review -> filterTextOf(review).any { it.contains(normalizedFilter, ignoreCase = true) } }
//                .sortedBy { it.id }
        return ReviewWithCategory.findReviews(filter)
    }

/*
    private fun filterTextOf(review: Review): List<String> {
        val dateConverter = LocalDateToStringConverter()
        return setOf(review.name,
                Category[review.category!!].name,
                review.score.toString(),
                review.count.toString(),
                dateConverter.toPresentation(review.date)).filterNotNull()
    }

*/
    /**
     * Deletes the given review from the review store.
     * @param review    the review to delete
     * @return  true if the operation was successful, otherwise false
     */
    fun deleteReview(review: Review) {
        review.delete()
    }

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
        dto.save()
//        var entity: Review? = if (dto.id == null) null else reviews[dto.id!!]
//        if (entity == null) {
//            // Make a copy to keep entities and DTOs separated
//            entity = dto.copy()
//            if (dto.id == null) {
//                entity.id = nextId.incrementAndGet()
//            }
//            reviews.put(entity.id!!, entity)
//        } else {
//            entity.score = dto.score
//            entity.name = dto.name
//            entity.date = dto.date
//            entity.count = dto.count
//        }
    }

    fun get(id: Long): Review = Review[id]
}
