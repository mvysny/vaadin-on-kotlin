package eu.vaadinonkotlin.vaadin

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.vokorm.exp
import com.gitlab.mvysny.jdbiorm.vaadin.filter.DateInterval
import com.gitlab.mvysny.jdbiorm.vaadin.filter.NumberInterval
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.expect

class JdbiOrmUtilsTest {
    @BeforeEach fun fakeVaadin() { MockVaadin.setup() }
    @AfterEach fun tearDownVaadin() { MockVaadin.tearDown() }

    @Test fun apitest() {
        expect("(Person.dateOfBirth >= 2024-10-19) AND (Person.dateOfBirth <= 2024-10-19)") {
            Person::dateOfBirth.exp.between(DateInterval.of(LocalDate.of(2024, 10, 19))).toString()
        }
        expect("NoCondition") {
            Person::dateOfBirth.exp.between(DateInterval.UNIVERSAL).toString()
        }
        expect("(Person.dateOfBirth >= 2000-01-02) AND (Person.dateOfBirth <= 2000-01-01)") {
            Person::dateOfBirth.exp.between(DateInterval.EMPTY).toString()
        }
        expect("(Person.id >= 2) AND (Person.id <= 3)") {
            Person::id.exp.between(NumberInterval.ofLong(2L, 3L)).toString()
        }
        expect("Person.id = 2") {
            Person::id.exp.between(NumberInterval.ofLong(2L, 2L)).toString()
        }
        expect("(Person.age >= 2) AND (Person.age <= 3)") {
            Person::age.exp.between(NumberInterval.ofInt(2, 3)).toString()
        }
    }
}