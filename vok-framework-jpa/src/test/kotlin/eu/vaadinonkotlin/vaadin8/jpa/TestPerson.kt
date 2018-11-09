package eu.vaadinonkotlin.vaadin8.jpa

import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A simple model: one person has multiple hobbies.
 */
@Entity
data class TestPerson(
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
        var name: String? = null,

        @field:NotNull
        @field:Min(15)
        @field:Max(100)
        var age: Int? = null

) : Serializable {

    @field:OneToMany(mappedBy = "person", cascade = arrayOf(CascadeType.REMOVE))
    var hobbies: List<TestHobby> = mutableListOf()

    fun withHobbies(vararg hobbies: String) = db {
        hobbies.forEach {
            em.persist(TestHobby(text = it).apply { person = this@TestPerson })
        }
    }
}

@Entity
data class TestHobby(
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @field:NotNull
        @field:Size(min = 1, max = 200)
        var text: String? = null
) : Serializable {
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "person_id")
    var person: TestPerson? = null
}
