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

import com.github.vok.karibudsl.flow.*
import com.vaadin.data.BeanValidationBinder
import com.vaadin.data.Binder
import com.vaadin.shared.Registration
import com.vaadin.ui.Composite
import com.vaadin.ui.button.Button
import com.vaadin.ui.common.HtmlImport
import com.vaadin.ui.formlayout.FormLayout
import com.vaadin.ui.html.H2
import com.vaadin.ui.paper.dialog.GeneratedPaperDialog

import java.io.Serializable

/**
 * Abstract base class for dialogs adding, editing or deleting items.
 *
 * Subclasses are expected to
 *
 *  * add, during construction, the needed UI components to
 * [formLayout] and bind them using [binder], as well
 * as
 *  * override [confirmDelete] to open the confirmation dialog with
 * the desired message (by calling [openConfirmationDialog]).
 * @param T the type of the item to be added, edited or deleted
 * @property itemType The readable name of the item type
 * @property itemSaver Callback to save the edited item
 * @property itemDeleter Callback to delete the edited item
 * @param itemClass the class of item edited by this dialog
 */
@HtmlImport("frontend://bower_components/paper-dialog/paper-dialog.html")
abstract class AbstractEditorDialog<T : Serializable> protected constructor(private val itemType: String,
                      private val itemSaver: (T, Operation)->Unit, private val itemDeleter: (T)->Unit,
                      itemClass: Class<T>) : Composite<GeneratedPaperDialog<*>>() {

    private lateinit var titleField: H2
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var deleteButton: Button
    private var registrationForSave: Registration? = null

    /**
     * Gets the form layout, where additional components can be added for
     * displaying or editing the item's properties.
     * @return the form layout
     */
    protected lateinit var formLayout: FormLayout

    /**
     * Gets the binder.
     */
    protected val binder = BeanValidationBinder<T>(itemClass)
    /**
     * Gets the item currently being edited.
     * @return the item currently being edited
     */
    protected var currentItem: T? = null
        private set
    protected var currentOperation: Operation? = null
        private set

    private val confirmationDialog = ConfirmationDialog<T>()
    private lateinit var notification: PaperToast

    /**
     * The operations supported by this dialog. Delete is enabled when editing
     * an already existing item.
     */
    enum class Operation(val nameInTitle: String, val nameInText: String,
                                             val isDeleteDisabled: Boolean) {
        ADD("Add New", "add", true),
        EDIT("Edit", "edit", false)
    }

    init {
        content.apply {
            content.setModal(true)
            // Enabling modality disables cancel-on-esc (and cancel-on-outside-click)
            // We want to cancel on esc
            content.setNoCancelOnEscKey(false)

            titleField = h2()
            div { // form layout wrapper
                addClassName("has-padding")
                formLayout = formLayout {
                    setResponsiveSteps(FormLayout.ResponsiveStep("0", 1),
                            FormLayout.ResponsiveStep("50em", 2))
                    addClassName("no-padding")
                }
            }
            horizontalLayout { // button bar
                className = "buttons"
                saveButton = button("Save") {
                    isAutofocus = true
                    setPrimary()
                }
                cancelButton = button("Cancel") {
                    element.setAttribute("dialog-dismiss", true)
                }
                deleteButton = button("Delete") {
                    element.setAttribute("theme", "tertiary danger")
                    addClickListener { deleteClicked() }
                }
            }
            notification = paperToast {
                addClassName("notification")
            }
        }
    }

    /**
     * Opens the given item for editing in the dialog.
     *
     * @param item The item to edit; it may be an existing or a newly created instance
     * @param operation The operation being performed on the item
     */
    fun open(item: T, operation: Operation) {
        currentItem = item
        currentOperation = operation
        titleField.text = operation.nameInTitle + " " + itemType
        if (registrationForSave != null) {
            registrationForSave!!.remove()
        }
        registrationForSave = saveButton.addClickListener { saveClicked(operation) }
        binder.readBean(currentItem)

        deleteButton.isDisabled = operation.isDeleteDisabled
        content.open()
    }

    private fun saveClicked(operation: Operation) {
        if (binder.writeBeanIfValid(currentItem!!)) {
            itemSaver(currentItem!!, operation)
            content.close()
        } else {
            val status = binder.validate()
            notification.show(status.validationErrors.joinToString("; ") { it.errorMessage })
        }
    }

    private fun deleteClicked() {
        if (confirmationDialog.element.parent == null) {
            ui.orElse(null)?.add(confirmationDialog)
        }
        confirmDelete()
    }

    protected abstract fun confirmDelete()

    /**
     * Opens the confirmation dialog before deleting the current item.
     *
     * The dialog will display the given title and message(s), then call
     * [deleteConfirmed] if the Delete button is clicked.
     *
     * @param title The title text
     * @param message Detail message (optional, may be empty)
     * @param additionalMessage Additional message (optional, may be empty)
     */
    protected fun openConfirmationDialog(title: String, message: String = "",
                                         additionalMessage: String = "") {
        content.close()
        confirmationDialog.open(title, message, additionalMessage, "Delete",
                true, { deleteConfirmed(currentItem!!) }, { content.open() })
    }

    private fun deleteConfirmed(item: T) {
        itemDeleter(item)
        content.close()
    }
}
