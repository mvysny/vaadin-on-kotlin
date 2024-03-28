package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.FlexComponent

/**
 * Edits or creates a person. Use [Window.addCloseListener] to handle window close.
 * @property person the person to edit or create.
 */
class CreateEditPerson(val person: Person) : Dialog() {

    var onSaveOrCreateListener: () -> Unit = {}

    /**
     * True if we are creating a new person, false if we are editing an existing one.
     */
    private val creating: Boolean
        get() = person.id == null

    private lateinit var persistButton: Button
    private lateinit var form: PersonForm

    init {
//        caption = if (creating) "New Person" else "Edit #${person.id}"
        verticalLayout {
            isMargin = true
            form = personForm()
            horizontalLayout {
                isSpacing = true; alignSelf = FlexComponent.Alignment.CENTER
                persistButton = button(if (creating) "Create" else "Save") {
                    onClick { okPressed() }
                    setPrimary()
                }
                button("Cancel") {
                    onClick { close() }
                }
            }
        }
        form.binder.readBean(person)
    }

    private fun okPressed() {
        if (!form.binder.validate().isOk || !form.binder.writeBeanIfValid(person)) {
            return
        }
        person.save()
        onSaveOrCreateListener()
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
}

@VaadinDsl
fun (@VaadinDsl HasComponents).personForm(block: (@VaadinDsl PersonForm).()->Unit = {}) = init(PersonForm(), block)
