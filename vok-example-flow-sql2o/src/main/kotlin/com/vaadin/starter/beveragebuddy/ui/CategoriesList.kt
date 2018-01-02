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
import com.github.vok.framework.sql2o.vaadin.and
import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.starter.beveragebuddy.backend.Category
import com.vaadin.starter.beveragebuddy.backend.Review
import com.vaadin.starter.beveragebuddy.dataProvider
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcons
import com.vaadin.flow.component.textfield.TextField

/**
 * Displays the list of available categories, with a search filter as well as
 * buttons to add a new category or edit existing ones.
 */
@Route(value = "categories", layout = MainLayout::class)
@PageTitle("Categories List")
class CategoriesList : Div() {

    private lateinit var searchField: TextField
    private val grid: Grid<Category>

    private val form = CategoryEditorDialog(
            { category, operation -> saveCategory(category, operation) },
            { deleteCategory(it) })

    private val notification: PaperToast

    init {
        addClassName("categories-list")
        notification = paperToast()
        add(form)
        div { // view toolbar
            addClassName("view-toolbar")
            searchField = textField {
                addClassName("view-toolbar__search-field")
                placeholder = "Search"
                addValueChangeListener { updateView() }
            }
            button("New category", Icon(VaadinIcons.PLUS)) {
                setPrimary()
                addClassName("view-toolbar__button")
                addClickListener { form.open(Category(null, ""), AbstractEditorDialog.Operation.ADD) }
            }
        }
        grid = grid {
            addColumn({ it.name }).setHeader("Category")
            // @todo mavi this is N+1 queries (a query run for every row). A performance bottleneck. I need to do some smart join here instead.
            addColumn({ it.getReviewCount() }).setHeader("Beverages")
            // Grid does not yet implement HasStyle
            element.classList.add("categories")
            element.setAttribute("theme", "row-dividers")
            asSingleSelect().addValueChangeListener {
                if (it.value != null) {  // deselect fires yet another selection event, this time with null Category.
                    selectionChanged(it.value.id!!)
                    selectionModel.deselect(it.value)
                }
            }
        }

        updateView()
    }

    private fun selectionChanged(categoryId: Long) {
        form.open(Category[categoryId], AbstractEditorDialog.Operation.EDIT)
    }

    private fun Category.getReviewCount(): String {
        return Review.getTotalCountForReviewsInCategory(id).toString()
    }

    private fun updateView() {
        var dp = Category.dataProvider
        if (!searchField.value.isNullOrBlank()) {
            dp = dp.and { Category::name ilike "%${searchField.value.trim()}%" }
        }
        grid.setDataProvider(dp)
    }

    private fun saveCategory(category: Category, operation: AbstractEditorDialog.Operation) {
        category.save()
        notification.show("Category successfully ${operation.nameInText}ed.")
        updateView()
    }

    private fun deleteCategory(category: Category) {
        category.delete()
        notification.show("Category successfully deleted.")
        updateView()
    }
}
