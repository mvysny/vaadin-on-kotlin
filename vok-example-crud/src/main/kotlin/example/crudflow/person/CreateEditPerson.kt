package example.crudflow.person

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.FlexComponent
import java.time.Instant

/**
 * Edits or creates a person.
 * @property person the person to edit or create.
 */
class CreateEditPerson(val person: Person) : Dialog() {

    var onSaveOrCreateListener: () -> Unit = {}

    private val creating: Boolean
        get() = person.id == null

    private lateinit var persistButton: Button
    private lateinit var form: PersonForm

    init {
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
        if (person.created == null) person.created = Instant.now()
        person.save()
        onSaveOrCreateListener()
        close()
    }
}

class PersonForm : FormLayout() {
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
            setItems(*MaritalStatus.entries.toTypedArray())
            bind(binder).bind(Person::maritalStatus)
        }
        checkBox("Alive") {
            bind(binder).bind(Person::alive)
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).personForm(block: (@VaadinDsl PersonForm).()->Unit = {}) = init(PersonForm(), block)
