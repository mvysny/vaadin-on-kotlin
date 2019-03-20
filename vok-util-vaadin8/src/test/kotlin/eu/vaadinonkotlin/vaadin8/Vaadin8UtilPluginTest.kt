package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.github.mvysny.kaributesting.v8._expectOne
import com.vaadin.ui.Button
import com.vaadin.ui.UI
import eu.vaadinonkotlin.VaadinOnKotlin

class Vaadin8UtilPluginTest : DynaTest({
    beforeEach { VaadinOnKotlin.init(); MockVaadin.setup() }
    afterEach { MockVaadin.tearDown(); VaadinOnKotlin.destroy() }
    test("test i18n") {
        UI.getCurrent().apply {
            dateRangePopup {
                isPopupVisible = true
            }
        }
        _expectOne<Button> { caption = "Clear" }
    }
})