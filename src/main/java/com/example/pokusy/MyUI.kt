package com.example.pokusy

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
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

    override fun init(request: VaadinRequest?) {
        val clickMe = Button("Click me", Button.ClickListener { stuff() })
        personGrid.containerDataSource = createContainer(Person::class.java)
        personGrid.setColumns("id", "name")
        val content = VerticalLayout(clickMe, personGrid)
        setContent(content)
    }

    private fun stuff() {
        transaction {
            val person = Person(name = "Jozko")
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
