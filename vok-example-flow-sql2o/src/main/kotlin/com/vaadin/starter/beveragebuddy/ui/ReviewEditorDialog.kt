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

import com.github.vok.framework.sql2o.vaadin.dataProvider
import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.starter.beveragebuddy.backend.Category
import com.vaadin.starter.beveragebuddy.backend.Review
import com.vaadin.starter.beveragebuddy.ui.converters.toId
import java.time.LocalDate

/**
 * A dialog for editing [Review] objects.
 */
class ReviewEditorDialog(saveHandler: (Review, AbstractEditorDialog.Operation) -> Unit, deleteHandler: (Review) -> Unit)
    : AbstractEditorDialog<Review>("Review", saveHandler, deleteHandler, Review::class.java) {

    private lateinit var categoryBox: ComboBox<Category>
    private lateinit var scoreBox: ComboBox<String>
    private lateinit var lastTasted: DatePicker
    private lateinit var beverageName: TextField
    private lateinit var timesTasted: TextField

    init {
        formLayout.apply {
            beverageName = textField("Beverage name") {
                // no need to have validators here: they are automatically picked up from the bean field.
                bind(binder).trimmingConverter().bindN(Review::name)
            }
            timesTasted = textField("Times tasted") {
                pattern = "[0-9]*"
                isPreventInvalidInput = true
                bind(binder).toInt().bindN(Review::count)
            }
            categoryBox = comboBox("Choose a category") {
                setItemLabelGenerator { it.name }
                isAllowCustomValue = false
                dataProvider = Category.dataProvider
                bind(binder).toId().bindN(Review::category)
            }
            lastTasted = datePicker("Choose the date") {
                max = LocalDate.now()
                min = LocalDate.of(1, 1, 1)
                value = LocalDate.now()
                bind(binder).bindN(Review::date)
            }
            scoreBox = comboBox("Mark a score") {
                isAllowCustomValue = false
                setItems("1", "2", "3", "4", "5")
                bind(binder).toInt().bindN(Review::score)
            }
        }
    }

    override fun confirmDelete() {
        openConfirmationDialog("""Delete beverage "${currentItem!!.name}"?""")
    }
}
