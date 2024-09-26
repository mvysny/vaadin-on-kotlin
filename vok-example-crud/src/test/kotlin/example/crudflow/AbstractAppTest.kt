package example.crudflow

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import example.crudflow.person.Person
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

private val routes: Routes = Routes().autoDiscoverViews("example")

/**
 * When called from a dyna test, it configures the test so that the app is properly bootstrapped and Vaadin is properly mocked.
 *
 * A demo of reusable test lifecycle; see https://github.com/mvysny/dynatest#patterns for details.
 */
abstract class AbstractAppTest {
    companion object {
        @BeforeAll @JvmStatic fun bootApp() { Bootstrap().contextInitialized(null) }
        @AfterAll @JvmStatic fun destroyApp() { Bootstrap().contextDestroyed(null) }
    }

    @BeforeEach fun fakeVaadin() { MockVaadin.setup(routes) }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }
    @BeforeEach @AfterEach fun cleanupDb() { Person.deleteAll() }
}
