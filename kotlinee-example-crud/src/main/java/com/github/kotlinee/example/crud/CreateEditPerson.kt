package com.github.kotlinee.example.crud

import com.github.kotlinee.framework.*
import com.vaadin.data.fieldgroup.BeanFieldGroup
import com.vaadin.data.fieldgroup.FieldGroup
import com.vaadin.server.UserError
import com.vaadin.ui.Button
import com.vaadin.ui.Window

/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
class CreateEditPerson(val person: Person): Window() {
    // the validation demo. infer validations from JSR303 annotations attached to the Person class, when
    // the fieldGroup.bind() is called.
    private val fieldGroup = BeanFieldGroup<Person>(Person::class.java)
    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.id == null

    private var persistButton: Button? = null

    init {
        fieldGroup.setItemDataSource(person)
        isModal = true
        verticalLayout {
            isSpacing = true
            setMargin(true)
            formLayout {
                isSpacing = true
                val name = textField("Name:") {
                    trimmingConverter()
                }
                fieldGroup.bind(name, "name")
                val age = textField("Age:")
                fieldGroup.bind(age, "age")
            }
            horizontalLayout {
                isSpacing = true
                persistButton = button(if(creating) "Create" else "Save") {
                    setLeftClickListener(Button.ClickListener { okPressed() })
                }
                button("Cancel") {
                    setLeftClickListener(Button.ClickListener { close() })
                }
            }
        }
    }

    private fun okPressed() {
        try {
            fieldGroup.commit()
        } catch(e: FieldGroup.CommitException) {
            persistButton!!.componentError = UserError("Please fix the errors on the form")
            return
        }
        db {
            if (creating) em.persist(person) else em.merge(person)
            Session.lastAddedPersonCache.lastAdded = person
        }
        close()
    }
}
