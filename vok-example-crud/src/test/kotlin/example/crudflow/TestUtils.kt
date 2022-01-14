package example.crudflow

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import example.crudflow.person.Person

private val routes: Routes = Routes().autoDiscoverViews("example")

/**
 * When called from a dyna test, it configures the test so that the app is properly bootstrapped and Vaadin is properly mocked.
 *
 * A demo of reusable test lifecycle; see https://github.com/mvysny/dynatest#patterns for details.
 */
@DynaTestDsl
fun DynaNodeGroup.usingApp() {
    beforeGroup { Bootstrap().contextInitialized(null) }
    afterGroup { Bootstrap().contextDestroyed(null) }

    beforeEach { MockVaadin.setup(routes) }
    afterEach { MockVaadin.tearDown() }
    fun cleanupDb() { Person.deleteAll() }
    beforeEach { cleanupDb() }
    afterEach { cleanupDb() }
}
