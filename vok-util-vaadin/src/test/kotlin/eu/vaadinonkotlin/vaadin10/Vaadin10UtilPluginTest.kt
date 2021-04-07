package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.karibudsl.v10.dateRangePopup
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._expectOne
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import eu.vaadinonkotlin.VaadinOnKotlin

class Vaadin10UtilPluginTest : DynaTest({
    beforeEach { VaadinOnKotlin.init(); MockVaadin.setup() }
    afterEach { MockVaadin.tearDown(); VaadinOnKotlin.destroy() }
    test("test i18n") {
        UI.getCurrent().apply {
            dateRangePopup {
                isDialogVisible = true
            }
        }
        _expectOne<Button> { caption = "Clear" }
    }
})