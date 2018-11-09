package com.github.vok.example.crud.personeditor

import com.github.vok.example.crud.lastAddedPersonCache
import com.github.vok.framework.Session
import com.github.mvysny.karibudsl.v8.*
import com.vaadin.server.UserError
import com.vaadin.ui.*

/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
internal class CreateEditPerson(val person: Person) : Window() {
    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.id == null

    private lateinit var persistButton: Button
    private lateinit var form: PersonForm

    init {
        isModal = true
        caption = if (creating) "New Person" else "Edit #${person.id}"
        verticalLayout {
            isMargin = true
            form = personForm()
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
        form.binder.readBean(person)
    }

    private fun okPressed() {
        if (!form.binder.validate().isOk || !form.binder.writeBeanIfValid(person)) {
            persistButton.componentError = UserError("Please fix the errors on the form")
            return
        }
        person.save()
        Session.lastAddedPersonCache.lastAdded = person
        close()
    }
}

/**
 * The form, which edits a single [Person].
 * * To populate the fields, just call `form.binder.readBean(person)`
 * * To validate and save the data, just call `binder.validate().isOk && binder.writeBeanIfValid(person)`
 */
class PersonForm : FormLayout() {
    /**
     * Populates the fields with data from a bean. Also infers validations from JSR303 annotations attached to the Person class, when
     * the fieldGroup.bind() is called.
     */
    val binder = beanValidationBinder<Person>()
    init {
        w = wrapContent
        textField("Name:") {
            focus()
            bind(binder).trimmingConverter().bind(Person::personName)
        }
        textField("Age:") {
            bind(binder).toInt().bind(Person::age)
        }
        dateField("Date of birth:") {
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
}

/**
 * The DSL function which allows us to add the [PersonForm] into our UIs in a DSL way.
 */
@VaadinDsl
fun (@VaadinDsl HasComponents).personForm(block: (@VaadinDsl PersonForm).()->Unit = {}) = init(PersonForm(), block)
