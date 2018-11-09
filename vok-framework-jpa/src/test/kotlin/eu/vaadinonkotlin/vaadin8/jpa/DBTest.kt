package eu.vaadinonkotlin.vaadin8.jpa

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.framework.VaadinOnKotlin
import org.flywaydb.core.Flyway
import java.io.IOException
import javax.persistence.EntityManager
import kotlin.test.expect
import kotlin.test.fail

fun DynaNodeGroup.usingDatabase() {
    beforeGroup {
        JPAVOKPlugin().init()
        val flyway = Flyway.configure()
            .dataSource(VaadinOnKotlin.getDataSource())
            .load()
        flyway.migrate()
    }
    fun clearDatabase() {
        db { em.deleteAll<TestHobby>() }
        db { em.deleteAll<TestPerson>() }
    }
    beforeEach { clearDatabase() }
    afterEach { clearDatabase() }
}

class DBTest : DynaTest({

    usingDatabase()

    test("CantControlTxFromDbFun") {
        expectThrows(IllegalStateException::class) {
            db { em.transaction }
        }
    }

    test("verifyEntityManagerClosed") {
        val em: EntityManager = db { em }
        expect(false) { em.isOpen }
    }

    test("exceptionRollsBack") {
        try {
            db {
                em.persist(TestPerson(name = "foo", age = 25))
                throw IOException("simulated")
            }
            @Suppress("UNREACHABLE_CODE")
            fail("Exception should have been thrown")
        } catch (e: IOException) {
            // okay
            expect(listOf()) { db { em.findAll<TestPerson>() } }
        }
    }

    test("commitInNestedDbBlocks") {
        val person = db {
            db {
                db {
                    TestPerson(name = "foo", age = 25).apply { em.persist(this) }
                }
            }
        }
        expect(listOf(person)) { db { em.findAll<TestPerson>() } }
    }

    test("exceptionRollsBackInNestedDbBlocks") {
        try {
            db {
                db {
                    db {
                        em.persist(TestPerson(name = "foo", age = 25))
                        throw IOException("simulated")
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            fail("Exception should have been thrown")
        } catch (e: IOException) {
            // okay
            expect(listOf()) { db { em.findAll<TestPerson>() } }
        }
    }

    test("singleOrNull") {
        expect(null) { db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull() } }
        db { em.persist(TestPerson(name = "Laurel", age = 50)) }
        expect("Laurel") { db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull()!!.name } }
    }

    test("singleOrNullFailsOnTwoResults") {
        db { em.persist(TestPerson(name = "Laurel", age = 50)) }
        db { em.persist(TestPerson(name = "Hardy", age = 55)) }
        expectThrows(IllegalStateException::class) {
            db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull() }
        }
    }

    test("DeleteByIdDoesNothingOnUnknownId") {
        db { em.deleteById<TestPerson>(25L) }
        expect(listOf()) { db { em.findAll<TestPerson>() } }
    }

    test("DeleteById") {
        val person = db { TestPerson(name = "Laurel", age = 50).apply { em.persist(this) } }
        expect(listOf(person)) { db { em.findAll<TestPerson>() } }
        db { em.deleteById<TestPerson>(person.id!!) }
        expect(listOf()) { db { em.findAll<TestPerson>() } }
    }

    test("getById") {
        val person = db { TestPerson(name = "Laurel", age = 50).apply { em.persist(this) } }
        expect(person) { db { em.get<TestPerson>(person.id!!) } }
    }

    test("getByIdFailsOnNonExistingEntity") {
        expectThrows(IllegalArgumentException::class) {
            db { em.get<TestPerson>(15L) }
        }
    }
})
