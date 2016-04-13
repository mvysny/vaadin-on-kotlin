package com.example.pokusy

import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinServlet
import com.vaadin.ui.Button
import com.vaadin.ui.Notification
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import javax.servlet.annotation.WebServlet

/**
 * @author mvy
 */
@Theme("valo")
@Title("Pokusy")
class MyUI : UI() {
    override fun init(request: VaadinRequest?) {
        val clickMe = Button("Click me", Button.ClickListener { stuff() })
        val content = VerticalLayout(clickMe)
        setContent(content)
    }

    private fun stuff() {
        transaction {
            val person = Person(name = "Jozko")
            em.persist(person)
            Notification.show("Persisted " + person)
        }
    }
}

@WebServlet(urlPatterns = arrayOf("/*"), name = "MyUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MyUI::class, productionMode = false)
class MyUIServlet : VaadinServlet() { }
