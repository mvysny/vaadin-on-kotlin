package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.ktormvaadin.dataProvider
import com.github.mvysny.ktormvaadin.withStringFilterOn
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.binder.BeanValidationBinder
import org.junit.jupiter.api.Test
import org.ktorm.entity.Entity
import kotlin.test.expect

class ConvertersTest : AbstractVaadinDbTest() {
    interface Review : Entity<Review> {
        var id: Long?
        var person: Long?
        companion object : Entity.Factory<Review>()
    }

    @Test fun `toId() test`() {
        val person = Person { personName = "foo"; age = 0 }.create()

        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Person>("Choose a category").apply {
            setItemLabelGenerator { it.personName }
            isAllowCustomValue = false
            setItems(Persons.dataProvider.withStringFilterOn(Persons.name))
            bind(binder).toId(Persons.id).bind(Review::person)
        }

        val r = Review()
        binder.readBean(r)
        expect(null) { categoryBox.value }
        categoryBox.value = person
        binder.writeBean(r)
        expect(person.id!!) { r.person }
    }
}
