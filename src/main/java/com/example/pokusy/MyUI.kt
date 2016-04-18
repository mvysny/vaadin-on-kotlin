package com.example.pokusy

import com.example.pokusy.kotlinee.*
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ClickableRenderer
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Vaadin UI which demoes all the features. If not familiar with Vaadin, please check out the Vaadin tutorial first.
 * @author mvy
 */
@Theme("valo")
@Title("Pokusy")
@Push(transport = Transport.WEBSOCKET_XHR)
class MyUI : UI() {

    private val log = LoggerFactory.getLogger(javaClass)

    private var personGrid: Grid? = null
    private val personGridDS = createContainer(Person::class.java)
    private var createButton: Button? = null
    private var timerLabel: Label? = null
    private val timer = AtomicInteger()
    private var timerHandle: ScheduledFuture<*>? = null

    override fun init(request: VaadinRequest?) {
        log.info("UI.init()")

        // the Vaadin DSL demo - build your UI, builder-style!
        verticalLayout {
            setSizeFull()
            createButton = button("Create New Person") {
                addClickListener { createOrEditPerson(Person()) }
            }
            timerLabel = label()

            // the JPA list demo - shows all instances of a particular JPA entity, allow sorting. @todo filtering
            personGrid = grid(dataSource = personGridDS) {
                expandRatio = 1f
                addButtonColumn("edit", "Edit", ClickableRenderer.RendererClickListener {
                    db { createOrEditPerson(em.get(it.itemId)) }
                })
                addButtonColumn("delete", "Delete", ClickableRenderer.RendererClickListener {
                    db { em.deleteById<Person>(it.itemId) }
                    refreshGrid()
                })
                setColumns("id", "name", "age", "edit", "delete")
                setSizeFull()
            }
        }

        // async and Push demo - show a label and periodically update its value from the server.
        timerHandle = scheduleAtFixedRate(0, 1 * SECONDS) {
            timer.incrementAndGet()
            db {
                // you can use DB even in background threads :)
            }
            access {
                timerLabel!!.value = "Timer: $timer; last added = ${lastAddedPersonCache.lastAdded}"
            }
        }
    }

    private fun refreshGrid() {
        personGridDS.refresh()
    }

    private fun createOrEditPerson(person: Person) {
        CreateEditPerson(person).apply {
            addCloseListener(Window.CloseListener { refreshGrid() })
            addWindow(this)
        }
    }

    override fun detach() {
        log.info("UI.detach()")
        timerHandle?.cancel(false)
        super.detach()
    }
}
