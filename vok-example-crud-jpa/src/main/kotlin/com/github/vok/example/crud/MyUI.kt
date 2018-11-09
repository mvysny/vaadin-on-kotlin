package com.github.vok.example.crud

import com.github.vok.example.crud.personeditor.CrudView
import eu.vaadinonkotlin.vaadin8.Session
import eu.vaadinonkotlin.vaadin8.jpa.db
import com.github.vok.framework.scheduleAtFixedRate
import com.github.vok.framework.seconds
import com.github.mvysny.karibudsl.v8.*
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.annotations.Viewport
import com.vaadin.icons.VaadinIcons
import com.vaadin.navigator.Navigator
import com.vaadin.navigator.PushStateNavigation
import com.vaadin.navigator.ViewDisplay
import com.vaadin.server.ClassResource
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.Label
import com.vaadin.ui.UI
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Vaadin UI which demoes all the features. If not familiar with Vaadin, please check out the Vaadin tutorial first.
 * @author mvy
 */
@Theme("valo")
@Title("VaadinOnKotlin Demo")
@Push(transport = Transport.WEBSOCKET_XHR)
@Viewport("width=device-width, initial-scale=1.0")
@PushStateNavigation
class MyUI : UI() {

    private lateinit var statusTicker: Label
    private val timer = AtomicInteger()
    @Transient
    private var timerHandle: ScheduledFuture<*>? = null

    override fun init(request: VaadinRequest?) {
        val content = valoMenu {
            appTitle = "<h3>Vaadin-on-Kotlin <strong>Sample App</strong></h3>"
            userMenu {
                item("John Doe", ClassResource("profilepic300px.jpg")) {
                    item("Edit Profile")
                    item("Preferences")
                    addSeparator()
                    item("Sign Out")
                }
            }
            menuButton("Welcome", VaadinIcons.MENU, "3", WelcomeView::class.java)
            menuButton("CRUD Demo", VaadinIcons.NOTEBOOK, view = CrudView::class.java)

            verticalLayout {
                setSizeFull(); expandRatio = 1f; primaryStyleName = "valo-content"
                this@valoMenu.viewPlaceholder = cssLayout {
                    addStyleName("v-scrollable"); setSizeFull(); expandRatio = 1f
                }
                statusTicker = label()
            }
        }
        navigator = Navigator(this, content as ViewDisplay)
        navigator.addProvider(autoViewProvider)
    }

    override fun attach() {
        super.attach()
        // async and Push demo - show a label and periodically update its value from the server.
        timerHandle = scheduleAtFixedRate(0.seconds, 1.seconds) {
            timer.incrementAndGet()
            db {
                // you can use the database even in background threads :)
            }
            ui.access {
                statusTicker.value = "Timer: $timer; Last Added = ${Session.lastAddedPersonCache.lastAdded}"
            }
        }
    }

    override fun detach() {
        timerHandle?.cancel(false)
        timerHandle = null
        super.detach()
    }
}
