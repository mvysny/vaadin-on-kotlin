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

import com.github.vok.karibudsl.flow.bind
import com.github.vok.karibudsl.flow.bindN
import com.github.vok.karibudsl.flow.textField
import com.github.vok.karibudsl.flow.trimmingConverter
import com.vaadin.flow.data.validator.StringLengthValidator
import com.vaadin.starter.beveragebuddy.backend.Category
import com.vaadin.starter.beveragebuddy.backend.Review
import com.vaadin.starter.beveragebuddy.backend.ReviewWithCategory
import com.vaadin.flow.component.textfield.TextField

/**
 * A dialog for editing [Category] objects.
 */
class CategoryEditorDialog(itemSaver: (Category, AbstractEditorDialog.Operation)->Unit, itemDeleter: (Category)->Unit)
    : AbstractEditorDialog<Category>("Category", itemSaver, itemDeleter, Category::class.java) {

    init {
        formLayout.apply {
            textField("Category Name") {
                bind(binder)
                        .trimmingConverter()
                        .withValidator(StringLengthValidator(
                                "Category name must contain at least 3 printable characters",
                                3, null))
                        .withValidator({ name -> isNameUnique(name) }, "Category name must be unique")
                        .bindN(Category::name)
            }
        }
    }

    private fun isNameUnique(name: String?): Boolean {
        if (name == null || name.isBlank()) return true
        if (currentItem?.name == name && currentOperation == Operation.EDIT) return true
        return !Category.existsWithName(name)
    }

    override fun confirmDelete() {
        val reviewCount = Review.findReviews(currentItem!!.name).size
        val additionalMessage = if (reviewCount == 0)
            ""
        else
            "Deleting the category will mark the associated reviews as “undefined”. You may link the reviews to other categories on the edit page."
        openConfirmationDialog(
                "Delete Category “${currentItem!!.name}”?",
                "There are $reviewCount reviews associated with this category.",
                additionalMessage)
    }
}
