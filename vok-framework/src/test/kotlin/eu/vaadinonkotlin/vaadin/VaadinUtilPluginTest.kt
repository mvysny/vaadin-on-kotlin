package eu.vaadinonkotlin.vaadin

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import eu.vaadinonkotlin.VaadinOnKotlin

class VaadinUtilPluginTest : DynaTest({
    beforeEach { VaadinOnKotlin.init(); MockVaadin.setup() }
    afterEach { MockVaadin.tearDown(); VaadinOnKotlin.destroy() }
    test("smoke") {
    }
})