package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.vokorm.KEntity
import com.github.vokorm.dataloader.dataLoader
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.binder.BeanValidationBinder
import eu.vaadinonkotlin.vaadin.vokdb.withStringFilterOn
import kotlin.test.expect

class ConvertersTest : DynaTest({
    usingH2Database()
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("toId() test") {
        data class Review(override var id: Long? = null, var person: Long? = null) : KEntity<Long>

        val person = Person(personName = "foo")
        person.save()

        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Person>("Choose a category").apply {
            setItemLabelGenerator { it.personName }
            isAllowCustomValue = false
            setItems(Person.dataLoader.withStringFilterOn(Person::personName))
            bind(binder).toId().bind(Review::person)
        }

        val r = Review()
        binder.readBean(r)
        expect(null) { categoryBox.value }
        categoryBox.value = person
        binder.writeBean(r)
        expect(person.id!!) { r.person }
    }
})
