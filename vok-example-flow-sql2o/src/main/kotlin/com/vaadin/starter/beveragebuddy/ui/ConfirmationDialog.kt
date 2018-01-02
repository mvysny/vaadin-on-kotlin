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

import com.github.vok.karibudsl.flow.button
import com.github.vok.karibudsl.flow.div
import com.github.vok.karibudsl.flow.h2
import com.github.vok.karibudsl.flow.horizontalLayout
import com.vaadin.flow.shared.Registration
import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.HasStyle
import com.vaadin.flow.component.dependency.HtmlImport
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2

import java.io.Serializable

/**
 * A generic dialog for confirming or cancelling an action.
 *
 * @param <T> The type of the action's subject
 */
@HtmlImport("frontend://bower_components/paper-dialog/paper-dialog.html")
internal class ConfirmationDialog<T : Serializable> : Composite<GeneratedPaperDialog<*>>(), HasStyle {

    private lateinit var titleField: H2
    private lateinit var messageLabel: Div
    private lateinit var extraMessageLabel: Div
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private var registrationForConfirm: Registration? = null
    private var registrationForCancel: Registration? = null

    /**
     * Constructor.
     */
    init {
        addClassName("confirm-dialog")
        content.apply {
            setModal(true)
            // Enabling modality disables cancel-on-esc (and cancel-on-outside-click)
            // We want to cancel on esc
            setNoCancelOnEscKey(false)

            titleField = h2()
            div { // labels
                className = "text"
                messageLabel = div()
                extraMessageLabel = div()
            }
            horizontalLayout { // button bar
                className = "buttons"
                confirmButton = button {
                    element.setAttribute("dialog-confirm", true)
                    element.setAttribute("theme", "tertiary")
                    isAutofocus = true
                }
                cancelButton = button("Cancel") {
                    element.setAttribute("dialog-dismiss", true)
                    element.setAttribute("theme", "tertiary")
                }
            }
        }
    }

    /**
     * Opens the confirmation dialog with given [title].
     *
     * The dialog will display the given title and message(s), then call
     * [confirmHandler] if the Confirm button is clicked, or
     * [cancelHandler] if the Cancel button is clicked.
     * @param message Detail message (optional, may be empty)
     * @param additionalMessage Additional message (optional, may be empty)
     * @param actionName The action name to be shown on the Confirm button
     * @param isDisruptive True if the action is disruptive, such as deleting an item
     */
    fun open(title: String, message: String = "", additionalMessage: String = "",
             actionName: String, isDisruptive: Boolean, confirmHandler: ()->Unit,
             cancelHandler: ()->Unit) {
        titleField.text = title
        messageLabel.text = message
        extraMessageLabel.text = additionalMessage
        confirmButton.text = actionName

        if (registrationForConfirm != null) {
            registrationForConfirm!!.remove()
        }
        registrationForConfirm = confirmButton.addClickListener { confirmHandler() }
        if (registrationForCancel != null) {
            registrationForCancel!!.remove()
        }
        registrationForCancel = cancelButton.addClickListener { cancelHandler() }
        if (isDisruptive) {
            confirmButton.element.setAttribute("theme", "tertiary danger")
        }
        content.open()
    }
}
