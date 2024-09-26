package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.karibudsl.v10.bind
import com.github.vokorm.KEntity
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.binder.BeanValidationBinder
import org.junit.jupiter.api.Test
import kotlin.test.expect

class ConvertersTest : AbstractVaadinDbTest() {
    @Test fun `toId() test`() {
        data class Review(override var id: Long? = null, var person: Long? = null) : KEntity<Long>

        val person = Person(personName = "foo")
        person.save()

        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Person>("Choose a category").apply {
            setItemLabelGenerator { it.personName }
            isAllowCustomValue = false
            setItems(Person.dataProvider.withStringFilterOn(Person::personName))
            bind(binder).toId().bind(Review::person)
        }

        val r = Review()
        binder.readBean(r)
        expect(null) { categoryBox.value }
        categoryBox.value = person
        binder.writeBean(r)
        expect(person.id!!) { r.person }
    }
}
