package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributesting.v10.MockVaadin
import eu.vaadinonkotlin.VaadinOnKotlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VaadinUtilPluginTest {
    @BeforeEach fun setup() { VaadinOnKotlin.init(); MockVaadin.setup() }
    @AfterEach fun tearDown() { MockVaadin.tearDown(); VaadinOnKotlin.destroy() }
    @Test fun smoke() {}
}
