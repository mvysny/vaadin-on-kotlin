package com.example.pokusy

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.data.fieldgroup.BeanFieldGroup
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinServlet
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.annotation.WebServlet

/**
 * @author mvy
 */
@Theme("valo")
@Title("Pokusy")
@Push(transport = Transport.WEBSOCKET_XHR)
class MyUI : UI() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val personGrid = Grid()
    private val personName = TextField()
    private val createButton = Button("Create", Button.ClickListener { stuff() })
    private val timerLabel = Label()
    private val timer = AtomicInteger()
    private var timerHandle: ScheduledFuture<*>? = null

    override fun init(request: VaadinRequest?) {
        log.error("INIT()")
        personName.nullRepresentation = ""
        val fg = BeanFieldGroup(Person::class.java)
        // use a dummy person for now. we only want to set up the validation on the personName field.
        fg.setItemDataSource(Person())
        fg.bind(personName, "name")

        personGrid.containerDataSource = createContainer(Person::class.java)
        personGrid.setColumns("id", "name")

        val content = VerticalLayout(personName, createButton, personGrid, timerLabel)
        setContent(content)

        timerHandle = scheduleAtFixedRate(0, 1 * SECONDS) {
            timer.incrementAndGet()
            access {
                timerLabel.value = "Timer: $timer; last added = ${lastAddedPersonCache.lastAdded}"
            }
        }
    }

    private fun stuff() {
        personName.validate()
        transaction {
            val person = Person(name = personName.value.trim())
            em.persist(person)
            Notification.show("Persisted " + person)
            lastAddedPersonCache.lastAdded = person
        }
        personGrid.refresh()
        createButton.componentError = null
    }

    override fun detach() {
        log.error("DETACHED")
        timerHandle?.cancel(false)
        super.detach()
    }
}

@WebServlet(urlPatterns = arrayOf("/*"), name = "MyUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MyUI::class, productionMode = false)
class MyUIServlet : VaadinServlet() { }

fun Grid.refresh() = (containerDataSource as JPAContainer<*>).refresh()
