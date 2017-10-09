package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.Dao
import com.github.vok.framework.sql2o.db
import com.vaadin.starter.beveragebuddy.LEntity
import java.time.LocalDate
import javax.validation.constraints.*

/**
 * Represents a beverage review.
 * @property name the beverage name
 * @property score the score, 1..5, 1 being worst, 5 being best
 * @property date when the review was done
 * @property category the beverage category [Category.id]
 * @property count times tasted, 1..99
 */
// must be open - Flow requires it to create ModelProxy
open class Review(override var id: Long? = null,
                  
                  @field:NotNull
                  @field:Min(1)
                  @field:Max(5)
                  var score: Int = 1,

                  @field:NotBlank
                  @field:Size(min = 3)
                  var name: String = "",

                  @field:NotNull
                  @field:PastOrPresent
                  var date: LocalDate = LocalDate.now(),

                  var category: Long? = null,

                  @field:NotNull
                  @field:Min(1)
                  @field:Max(99)
                  var count: Int = 1) : LEntity {
    override fun toString() = "Review(id=$id, score=$score, name='$name', date=$date, category=$category, count=$count)"

    fun copy() = Review(id, score, name, date, category, count)

    companion object : Dao<Review> {
        /**
         * Computes the total sum of [count] for all reviews belonging to given [categoryId].
         * @return the total sum, 0 or greater.
         */
        fun getTotalCountForReviewsInCategory(categoryId: Long?): Long = db {
            val scalar = con.createQuery("select sum(r.count) from Review r where r.category = :catId")
                    .addParameter("catId", categoryId)
                    .executeScalar()
            (scalar as Number?)?.toLong() ?: 0L
        }

        /**
         * Fetches the reviews matching the given filter text.
         *
         * The matching is case insensitive. When passed an empty filter text,
         * the method returns all categories. The returned list is ordered
         * by name.
         * @param filter the filter text
         * @return the list of matching reviews, may be empty.
         */
        fun findReviews(filter: String): List<ReviewWithCategory> {
            val normalizedFilter = filter.trim().toLowerCase() + "%"
            return db {
                con.createQuery("""select r.id, r.score, r.name, r.date, r.count, r.category, IFNULL(c.name, 'Undefined') as categoryName
                    FROM Review r left join Category c on r.category = c.id
                    WHERE r.name ILIKE :filter or IFNULL(c.name, 'Undefined') ILIKE :filter or
                     CAST(r.score as VARCHAR) ILIKE :filter or
                     CAST(r.count as VARCHAR) ILIKE :filter
                     ORDER BY r.name""")
                        .addParameter("filter", normalizedFilter)
                        .executeAndFetch(ReviewWithCategory::class.java)
            }
        }
    }
}

/**
 * Holds the join of Review and its Category.
 * @property categoryName the [Category.name]
 */
// must be open - Flow requires non-final classes for ModelProxy
open class ReviewWithCategory(var categoryName: String? = null) : Review()
