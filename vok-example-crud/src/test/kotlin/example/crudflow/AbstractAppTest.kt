package example.crudflow

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.ktormvaadin.deleteAll
import example.crudflow.person.Persons
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

private val routes: Routes = Routes().autoDiscoverViews("example")

/**
 * When extended, configures the test so that the app is properly bootstrapped and Vaadin is properly mocked.
 *
 * A demo of reusable test lifecycle using JUnit 6 (Jupiter).
 */
abstract class AbstractAppTest {
    companion object {
        @BeforeAll @JvmStatic fun bootApp() { Bootstrap().contextInitialized(null) }
        @AfterAll @JvmStatic fun destroyApp() { Bootstrap().contextDestroyed(null) }
    }

    @BeforeEach fun fakeVaadin() { MockVaadin.setup(routes) }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }
    @BeforeEach @AfterEach fun cleanupDb() { Persons.deleteAll() }
}
