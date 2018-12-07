package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.getSuggestions
import com.github.mvysny.kaributesting.v10.setUserInput
import com.github.mvysny.vokdataloader.dataLoader
import com.vaadin.flow.component.combobox.ComboBox
import kotlin.test.expect

class FilteredComboBoxTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("combo box filtering") {
        val dp = (0..10).map { Person(it.toLong(), "foo $it", it) } .dataLoader().toDataProvider { it.id!! }
        val cb = ComboBox<Person>().apply {
            setDataProvider(dp.withStringFilterOn(Person::personName))
            setItemLabelGenerator { it.personName }
        }
        expect((0..10).map { "foo $it" }) { cb.getSuggestions() }
        cb.setUserInput("foo 1")
        expectList("foo 1", "foo 10") { cb.getSuggestions() }
    }
})
