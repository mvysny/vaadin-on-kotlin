package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.Dao
import com.github.vok.framework.sql2o.db
import com.vaadin.starter.beveragebuddy.LEntity
import java.time.LocalDate
import javax.validation.constraints.*

/**
 * Represents a beverage review.
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
        fun getTotalCountForReviewsInCategory(categoryId: Long?): Long = db {
            val scalar = con.createQuery("select sum(r.count) from Review r where r.category = :catId")
                    .addParameter("catId", categoryId)
                    .executeScalar()
            (scalar as Number?)?.toLong() ?: 0L
        }
    }
}

/**
 * Fetches a join of Review and its Category.
 */
// must be open - Flow requires non-final classes for ModelProxy
open class ReviewWithCategory(open var id: Long? = null, var score: Int = 0, var name: String = "", var date: LocalDate = LocalDate.now(),
                              var count: Int = 1, var category: Long? = null, var categoryName: String? = null) {
    companion object {
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
            val normalizedFilter = filter.trim().toLowerCase() + "%"
            return db {
                con.createQuery("""select r.id, r.score, r.name, r.date, r.count, r.category, IFNULL(c.name, 'Undefined') as categoryName
                    from Review r left join Category c on r.category = c.id
                    where r.name ILIKE :filter or IFNULL(c.name, 'Undefined') ILIKE :filter or
                     CAST(r.score as VARCHAR) ILIKE :filter or
                     CAST(r.count as VARCHAR) ILIKE :filter""")
                        .addParameter("filter", normalizedFilter)
                        .executeAndFetch(ReviewWithCategory::class.java)
            }
        }
    }
}
