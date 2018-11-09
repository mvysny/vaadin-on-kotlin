@file:Suppress("DEPRECATION")

package com.github.vok.example.crud.personeditor

import com.github.vok.example.crud.lastAddedPersonCache
import eu.vaadinonkotlin.vaadin8.Session
import com.github.vok.framework.db
import com.github.mvysny.karibudsl.v8.*
import com.github.mvysny.karibudsl.v8.v7compat.*
import com.vaadin.server.UserError
import com.vaadin.ui.Alignment
import com.vaadin.ui.Button
import com.vaadin.ui.Window
import com.vaadin.v7.data.fieldgroup.FieldGroup

/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
internal class CreateEditPerson(val person: Person) : Window() {
    // the validation demo. infer validations from JSR303 annotations attached to the Person class, when
    // the fieldGroup.bind() is called.
    private val fieldGroup = BeanFieldGroup<Person>()
    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.id == null

    private lateinit var persistButton: Button

    init {
        fieldGroup.setItemDataSource(person)
        isModal = true
        caption = if (creating) "New Person" else "Edit #${person.id}"
        verticalLayout {
            isMargin = true
            formLayout {
                w = wrapContent
                val name = textField7("Name:") {
                    trimmingConverter()
                    focus()
                }
                fieldGroup.bind(name, "name")
                val age = textField7("Age:")
                fieldGroup.bind(age, "age")
            }
            horizontalLayout {
                isSpacing = true; alignment = Alignment.MIDDLE_CENTER
                persistButton = button(if (creating) "Create" else "Save") {
                    onLeftClick { okPressed() }
                    setPrimary()
                }
                button("Cancel") {
                    onLeftClick { close() }
                }
            }
        }
    }

    private fun okPressed() {
        try {
            fieldGroup.commit()
        } catch(e: FieldGroup.CommitException) {
            persistButton.componentError = UserError("Please fix the errors on the form")
            return
        }
        db {
            if (creating) em.persist(person) else em.merge(person)
            Session.lastAddedPersonCache.lastAdded = person
        }
        close()
    }
}
