package com.github.vok.example.crud.personeditor

import com.github.vok.example.crud.lastAddedPersonCache
import com.github.vok.framework.Session
import com.github.vok.framework.db
import com.github.vok.framework.dbId
import com.github.vok.framework.vaadin.*
import com.vaadin.server.UserError
import com.vaadin.ui.Alignment
import com.vaadin.ui.Button
import com.vaadin.ui.Window

/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
internal class CreateEditPerson(val person: Person) : Window() {
    // the validation demo. infer validations from JSR303 annotations attached to the Person class, when
    // the fieldGroup.bind() is called.
    private val binder = BeanValidationBinder<Person>()
    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.dbId == null

    private lateinit var persistButton: Button

    init {
        isModal = true
        caption = if (creating) "New Person" else "Edit #${person.id}"
        verticalLayout {
            isMargin = true
            formLayout {
                w = wrapContent
                val name = textField("Name:") {
                    focus()
                }
                binder.forField(name).trimmingConverter().bind("name")
                val age = textField("Age:")
                binder.forField(age).stringToInt().bind("age")
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
        binder.readBean(person)
    }

    private fun okPressed() {
        if (!binder.writeBeanIfValid(person)) {
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
