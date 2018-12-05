package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.vokdataloader.dataLoader
import com.vaadin.flow.component.combobox.ComboBox

class FilteredComboBoxTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("combo box filtering") {
        val dp = (0..10).map { Person(it.toLong(), "foo $it", it) } .dataLoader().toDataProvider { it.id!! }
        val cb = ComboBox<Person>().apply {
            setDataProvider(dp.withStringFilterOn(Person::personName))
        }
        // @todo when Karibu-Testing 1.0.2 is released, add tests for proper filtering
    }
})
