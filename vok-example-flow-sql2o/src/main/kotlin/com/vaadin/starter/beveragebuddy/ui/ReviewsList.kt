/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.starter.beveragebuddy.ui

import com.github.vok.framework.sql2o.get
import com.vaadin.flow.templatemodel.Convert
import com.vaadin.flow.templatemodel.TemplateModel
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.starter.beveragebuddy.backend.Review
import com.vaadin.starter.beveragebuddy.ui.ReviewsList.ReviewsModel
import com.vaadin.starter.beveragebuddy.ui.converters.LocalDateToStringConverter
import com.vaadin.starter.beveragebuddy.ui.converters.LongToStringConverter
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dependency.HtmlImport
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.polymertemplate.EventHandler
import com.vaadin.flow.component.polymertemplate.Id
import com.vaadin.flow.component.polymertemplate.ModelItem
import com.vaadin.flow.component.polymertemplate.PolymerTemplate
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.starter.beveragebuddy.backend.ReviewWithCategory

/**
 * Displays the list of available categories, with a search filter as well as
 * buttons to add a new category or edit existing ones.
 *
 * Implemented using a simple template.
 */
@Route(value = "", layout = MainLayout::class)
@PageTitle("Review List")
@Tag("reviews-list")
@HtmlImport("frontend://reviews-list.html")
class ReviewsList : PolymerTemplate<ReviewsModel>() {

    @Id("search")
    private lateinit var search: TextField
    @Id("newReview")
    private lateinit var addReview: Button
    @Id("header")
    private lateinit var header: H1

    private val reviewForm = ReviewEditorDialog(
        { review, operation -> save(review, operation) },
        { this.delete(it) })

    interface ReviewsModel : TemplateModel {
        // remove this when https://youtrack.jetbrains.com/issue/KT-12794 is fixed
        @Convert.Container(
        Convert(value = LongToStringConverter::class, path = "id"),
        Convert(value = LocalDateToStringConverter::class, path = "date"),
        Convert(value = LongToStringConverter::class, path = "category")
        )
        // suppress wildcards: Kotlin generates List<? extends Review> which Flow doesn't like much and will fail with an exception.
        fun setReviews(reviews: @JvmSuppressWildcards List<ReviewWithCategory>)
    }

    init {
        search.placeholder = "Search"
        search.addValueChangeListener { updateList() }
        search.valueChangeMode = ValueChangeMode.EAGER
        addReview.addClickListener { openForm(Review(), AbstractEditorDialog.Operation.ADD) }
        updateList()
    }

    private fun save(review: Review, operation: AbstractEditorDialog.Operation) {
        review.save()
        updateList()
        Notification.show("Beverage successfully ${operation.nameInText}ed.", 3000, Notification.Position.BOTTOM_START)
    }

    private fun delete(review: Review) {
        review.delete()
        updateList()
        Notification.show("Beverage successfully deleted.", 3000, Notification.Position.BOTTOM_START)
    }

    private fun updateList() {
        val reviews = Review.findReviews(search.value ?: "")
        if (search.value.isNullOrBlank()) {
            header.text = "Reviews"
            header.add(Span("${reviews.size} in total"))
        } else {
            header.text = "Search for “${search.value}”"
            if (!reviews.isEmpty()) {
                header.add(Span("${reviews.size} results"))
            }
        }
        model.setReviews(reviews)
    }

    @EventHandler
    private fun edit(@ModelItem review: ReviewWithCategory) {
        openForm(Review[review.id!!], AbstractEditorDialog.Operation.EDIT)
    }

    private fun openForm(review: Review,
                         operation: AbstractEditorDialog.Operation) {
        // Add the form lazily as the UI is not yet initialized when
        // this view is constructed
        if (reviewForm.element.parent == null) {
            ui.get().add(reviewForm)
        }
        reviewForm.open(review, operation)
    }
}
