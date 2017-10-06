package com.vaadin.starter.beveragebuddy.backend

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.*

/**
 * Represents a beverage review.
 */
// must be open - Flow requires it to create ModelProxy
open class Review(open var id: Long? = null,
                  
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

                  @field:NotNull
                  var category: Category = Category.UNDEFINED,

                  @field:NotNull
                  @field:Min(1)
                  @field:Max(99)
                  var count: Int = 1) : Serializable {
    override fun toString() = "Review(id=$id, score=$score, name='$name', date=$date, category=$category, count=$count)"

    fun copy() = Review(id, score, name, date, category.copy(), count)
}
