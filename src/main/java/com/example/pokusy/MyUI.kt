package com.example.pokusy

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.data.fieldgroup.BeanFieldGroup
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinServlet
import com.vaadin.ui.*
import javax.servlet.annotation.WebServlet

/**
 * @author mvy
 */
@Theme("valo")
@Title("Pokusy")
class MyUI : UI() {

    private val personGrid = Grid()
    private val personName = TextField()
    private val createButton = Button("Create", Button.ClickListener { stuff() })

    override fun init(request: VaadinRequest?) {
        personName.nullRepresentation = ""
        val fg = BeanFieldGroup(Person::class.java)
        fg.setItemDataSource(Person()) // this sets up the validation
        fg.bind(personName, "name")

        personGrid.containerDataSource = createContainer(Person::class.java)
        personGrid.setColumns("id", "name")
        val content = VerticalLayout(personName, createButton, personGrid)
        setContent(content)
    }

    private fun stuff() {
        personName.validate()
        transaction {
            val person = Person(name = personName.value.trim())
            em.persist(person)
            Notification.show("Persisted " + person)
        }
        personGrid.refresh()
    }
}

@WebServlet(urlPatterns = arrayOf("/*"), name = "MyUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MyUI::class, productionMode = false)
class MyUIServlet : VaadinServlet() { }

fun Grid.refresh() = (containerDataSource as JPAContainer<*>).refresh()
