package com.github.vok.example.crudflow.person

import com.github.vok.karibudsl.flow.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.FlexComponent


/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
class CreateEditPerson(val person: Person) : Dialog() {

    var onSaveOrCreateListener: ()->Unit = {}

    // the validation demo. infer validations from JSR303 annotations attached to the Person class, when
    // the fieldGroup.bind() is called.
    private val binder = beanValidationBinder<Person>()
    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.id == null

    private lateinit var persistButton: Button

    init {
//        caption = if (creating) "New Person" else "Edit #${person.id}"
        verticalLayout {
            isMargin = true
            formLayout {
                textField("Name:") {
                    focus()
                    bind(binder).trimmingConverter().bind(Person::name)
                }
                textField("Age:") {
                    bind(binder).toInt().bind(Person::age)
                }
                datePicker("Date of birth:") {
                    bind(binder).bind(Person::dateOfBirth)
                }
                comboBox<MaritalStatus>("Marital status:") {
                    setItems(*MaritalStatus.values())
                    bind(binder).bind(Person::maritalStatus)
                }
                checkBox("Alive") {
                    bind(binder).bind(Person::alive)
                }
            }
            horizontalLayout {
                isSpacing = true; alignSelf = FlexComponent.Alignment.CENTER
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
        if (!binder.validate().isOk || !binder.writeBeanIfValid(person)) {
            return
        }
        person.save()
        onSaveOrCreateListener()
        close()
    }
}
