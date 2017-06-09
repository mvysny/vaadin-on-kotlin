package com.github.vok.framework

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class JpaProviderTest {
    companion object {
        @BeforeClass @JvmStatic
        fun initVOK() {
            DBTest.initVOK()
        }
    }

    @Before
    @After
    fun clearDb() {
        db {
            em.deleteAll<TestHobby>()
            em.deleteAll<TestPerson>()
        }
    }

    /**
     * Hibernate fails with LazyInitializationException unless we do something. Reproduces
     * https://github.com/mvysny/vaadin-on-kotlin/issues/3
     */
    @Test
    fun lazyLoadingInAnotherTransaction() {
        val person: TestPerson = db {
            val p = TestPerson(name = "Laurel", age = 50)
            em.persist(p)
            p.withHobbies("Playing piano", "Hiking")
            p
        }
        val p: TestPerson = db { em.find(TestPerson::class.java, person.id)!! }
        db {
            // doesn't help and throws LazyInitializationException
//            Hibernate.initialize(p.hobbies)

            // the only thing that works here is to set enable_lazy_load_no_trans to true in persistence.xml
            println(p.hobbies)
        }
    }

    /**
     * Test that we can create queries and stuff the result data into arbitrary POJOs. This way, we don't have to
     * model the relationships between the entities - the select will dictate the precise content of the result.
     */
    @Test
    fun canUseArbitraryPojoForCustomSelects() {
        db {
            var p = TestPerson(name = "Laurel", age = 50)
            em.persist(p)
            p.withHobbies("Playing piano", "Hiking")
            p = TestPerson(name = "Hardy", age = 48)
            em.persist(p)
            p.withHobbies("Drinking beer")
        }
        db {
            expectList(NameAndHobby("Hardy", "Drinking beer"), NameAndHobby("Laurel", "Hiking"), NameAndHobby("Laurel", "Playing piano")) {
                em.createQuery("select new com.github.vok.framework.NameAndHobby(p.name, h.text) from TestPerson p, TestHobby h where h.person = p", NameAndHobby::class.java).resultList.sorted()
            }
        }
    }

    @Test
    fun deletingPersonWillDeleteHobbies() {
        var p = db {
            TestPerson(name = "Zaphod", age = 42).apply {
                em.persist(this)
                withHobbies("Traveling through the galaxy")
            }
        }
        db {
            // this will not cascade the delete to hobbies and will thus fail on constraint violation when removing the person
            // p = em.merge(p)

            // this will fail because the entity is detached
            // em.refresh(p)

            // there is no call in em to make an entity "attached", so that the bloody remove() stops complaining about detached entity
            // https://stackoverflow.com/questions/912659/what-is-the-proper-way-to-re-attach-detached-objects-in-hibernate

            // this is the only way I have found, to delete the detached entity. Good job JPA, you managed to make things
            // unintuitively fail, while making them complicated </sarcasm>

            p = em.find(TestPerson::class.java, p.id)
            em.remove(p)
            expectList { em.findAll<TestPerson>()  }
            expectList { em.findAll<TestHobby>()  }
        }
    }
}

data class NameAndHobby(var name: String? = null, var hobby: String? = null) : Comparable<NameAndHobby> {

    override fun compareTo(other: NameAndHobby): Int = compareValuesBy(this, other, { it.name }, { it.hobby })
    override fun toString() = "$name/$hobby"
}
