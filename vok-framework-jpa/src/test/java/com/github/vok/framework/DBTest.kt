package com.github.vok.framework

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import javax.persistence.EntityManager
import kotlin.test.expect
import kotlin.test.fail

class DBTest {

    companion object {
        @BeforeClass @JvmStatic
        fun initPlugin() {
            JPAVOKPlugin().init()
        }
    }

    @Before
    fun removeAllPersons() {
        db { em.deleteAll<TestPerson>() }
    }

    @Test(expected = IllegalStateException::class)
    fun testCantControlTxFromDbFun() {
        db { em.transaction }
    }

    @Test
    fun verifyEntityManagerClosed() {
        val em: EntityManager = db { em }
        expect(false) { em.isOpen }
    }

    @Test
    fun exceptionRollsBack() {
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

    @Test
    fun commitInNestedDbBlocks() {
        val person = db { db { db {
            TestPerson(name = "foo", age = 25).apply { em.persist(this) }
        }}}
        expect(listOf(person)) { db { em.findAll<TestPerson>() }}
    }

    @Test
    fun exceptionRollsBackInNestedDbBlocks() {
        try {
            db { db { db {
                em.persist(TestPerson(name = "foo", age = 25))
                throw IOException("simulated")
            }}}
            @Suppress("UNREACHABLE_CODE")
            fail("Exception should have been thrown")
        } catch (e: IOException) {
            // okay
            expect(listOf()) { db { em.findAll<TestPerson>() } }
        }
    }

    @Test
    fun singleOrNull() {
        expect(null) { db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull() }}
        db { em.persist(TestPerson(name = "Laurel", age = 50)) }
        expect("Laurel") { db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull()!!.name }}
    }

    @Test(expected = IllegalStateException::class)
    fun singleOrNullFailsOnTwoResults() {
        db { em.persist(TestPerson(name = "Laurel", age = 50)) }
        db { em.persist(TestPerson(name = "Hardy", age = 55)) }
        db { em.createQuery("select p from TestPerson p", TestPerson::class.java).singleOrNull() }
        fail("Should have failed")
    }

    @Test
    fun testDeleteByIdDoesNothingOnUnknownId() {
        db { em.deleteById<TestPerson>(25L) }
        expect(listOf()) { db { em.findAll<TestPerson>() } }
    }

    @Test
    fun testDeleteById() {
        val person = db { TestPerson(name = "Laurel", age = 50).apply { em.persist(this) } }
        expect(listOf(person)) { db { em.findAll<TestPerson>() }}
        db { em.deleteById<TestPerson>(person.id!!) }
        expect(listOf()) { db { em.findAll<TestPerson>() } }
    }

    @Test
    fun getById() {
        val person = db { TestPerson(name = "Laurel", age = 50).apply { em.persist(this) } }
        expect(person) { db { em.get<TestPerson>(person.id!!) } }
    }

    @Test(expected = IllegalArgumentException::class)
    fun getByIdFailsOnNonExistingEntity() {
        db { em.get<TestPerson>(15L) }
        fail("Should have failed")
    }
}
