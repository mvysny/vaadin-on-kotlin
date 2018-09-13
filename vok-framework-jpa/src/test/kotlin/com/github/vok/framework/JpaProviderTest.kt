package com.github.vok.framework

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList

class JpaProviderTest : DynaTest({

    usingDatabase()

    /**
     * Hibernate fails with LazyInitializationException unless we do something. Reproduces
     * https://github.com/mvysny/vaadin-on-kotlin/issues/3
     */
    test("lazyLoadingInAnotherTransaction") {
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
    test("canUseArbitraryPojoForCustomSelects") {
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

    test("deletingPersonWillDeleteHobbies") {
        var p = db {
            TestPerson(name = "Zaphod", age = 42).apply {
                em.persist(this)
                withHobbies("Traveling through the galaxy")
            }
        }
        db {
            em.delete(p)
            expectList { em.findAll<TestPerson>()  }
            expectList { em.findAll<TestHobby>()  }
        }
    }
})

data class NameAndHobby(var name: String? = null, var hobby: String? = null) : Comparable<NameAndHobby> {

    override fun compareTo(other: NameAndHobby): Int = compareValuesBy(this, other, { it.name }, { it.hobby })
    override fun toString() = "$name/$hobby"
}
