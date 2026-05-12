package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.ktormvaadin.findAll
import org.junit.jupiter.api.Test
import kotlin.test.expect

class EntityTest : AbstractDbTest() {
    @Test fun `enum column round-trips through the database`() {
        MaritalStatus.entries.forEachIndexed { i, status ->
            Person { personName = "p$i"; age = 20 + i; maritalStatus = status }.create()
        }
        val loaded = Persons.findAll().sortedBy { it.personName }
        expect(MaritalStatus.entries.toList()) { loaded.map { it.maritalStatus } }
    }
}
