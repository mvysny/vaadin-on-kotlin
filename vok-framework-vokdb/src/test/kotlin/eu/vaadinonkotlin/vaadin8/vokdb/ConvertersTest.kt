package eu.vaadinonkotlin.vaadin8.vokdb

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v8.bind
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.github.vokorm.KEntity
import com.vaadin.data.BeanValidationBinder
import com.vaadin.ui.ComboBox
import kotlin.test.expect

class ConvertersTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }
    usingH2Database()

    lateinit var person: Person
    beforeEach {
        person = Person(personName = "cat")
        person.create()
    }

    test("toId() test") {
        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Person>("Choose person").apply {
            setItemCaptionGenerator { it.personName }
            isTextInputAllowed = false
            setItems(Person.findAll())
            bind(binder).toId().bind(Review::personId)
        }

        val r = Review()
        binder.readBean(r)
        expect(null) { categoryBox.value }
        categoryBox.value = person
        binder.writeBean(r)
        expect(person.id) { r.personId }
    }

    test("toPresentation() test") {
        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Person>("Choose person").apply {
            setItemCaptionGenerator { it.personName }
            isTextInputAllowed = false
            setItems(Person.findAll())
            bind(binder).toId().bind(Review::personId)
        }

        expect(null) { categoryBox.value }
        val r = Review(personId = person.id)
        binder.readBean(r)
        expect(person) { categoryBox.value }
        categoryBox.value = null
        binder.writeBean(r)
        expect(null) { r.personId }
    }
})

data class Review(override var id: Long? = null, var personId: Long? = null, var score: Int? = null) : KEntity<Long>
