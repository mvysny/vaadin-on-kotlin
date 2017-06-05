package com.github.vok.example.crud

import com.github.vok.example.crud.personeditor.CrudView
import com.github.vok.framework.Session
import com.github.vok.framework.db
import com.github.vok.framework.scheduleAtFixedRate
import com.github.vok.framework.seconds
import com.github.vok.karibudsl.*
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.icons.VaadinIcons
import com.vaadin.navigator.Navigator
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewDisplay
import com.vaadin.server.ClassResource
import com.vaadin.server.FontAwesome
import com.vaadin.server.Resource
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Vaadin UI which demoes all the features. If not familiar with Vaadin, please check out the Vaadin tutorial first.
 * @author mvy
 */
@Theme("valo")
@Title("VaadinOnKotlin Demo")
@Push(transport = Transport.WEBSOCKET_XHR)
class MyUI : UI() {

    private val content = ValoMenuLayout()

    override fun init(request: VaadinRequest?) {
        setContent(content)
        navigator = Navigator(this, content as ViewDisplay)
        navigator.addProvider(autoViewProvider)
    }
}

/**
 * The main screen with the menu and a view placeholder, where the view contents will go.
 */
private class ValoMenuLayout: HorizontalLayout(), ViewDisplay {
    /**
     * Tracks the registered menu items associated with view; when a view is shown, highlight appropriate menu item button.
     */
    private val views = mutableMapOf<Class<out View>, Button>()

    private val menuArea: CssLayout
    private lateinit var menu: CssLayout
    private lateinit var viewPlaceholder: CssLayout
    private lateinit var statusTicker: Label
    init {
        setSizeFull();isSpacing = false

        menuArea = cssLayout {
            primaryStyleName = ValoTheme.MENU_ROOT
            menu = cssLayout { // menu
                horizontalLayout {
                    w = fillParent; isSpacing = false; defaultComponentAlignment = Alignment.MIDDLE_LEFT
                    styleName = ValoTheme.MENU_TITLE
                    label {
                        html("<h3>Vaadin-on-Kotlin <strong>Sample App</strong></h3>")
                        w = wrapContent
                        expandRatio = 1f
                    }

                }
                button("Menu") { // only visible when the top bar is shown
                    onLeftClick {
                        menu.toggleStyleName("valo-menu-visible", !menu.hasStyleName("valo-menu-visible"))
                    }
                    addStyleNames(ValoTheme.BUTTON_PRIMARY, ValoTheme.BUTTON_SMALL, "valo-menu-toggle")
                    icon = VaadinIcons.MENU
                }
                menuBar { // the user menu, settings
                    styleName = "user-menu"
                    addItem("John Doe", ClassResource("profilepic300px.jpg"), null).apply {
                        item("Edit Profile")
                        item("Preferences")
                        addSeparator()
                        item("Sign Out")
                    }
                }
                // the navigation buttons
                cssLayout {
                    primaryStyleName = "valo-menuitems"
                    menuButton(VaadinIcons.MENU, "Welcome", "3", WelcomeView::class.java)
                    menuButton(VaadinIcons.NOTEBOOK, "CRUD Demo", view = CrudView::class.java)
                }
            }
        }

        verticalLayout {
            setSizeFull(); expandRatio = 1f
            viewPlaceholder = cssLayout {
                primaryStyleName = "valo-content"
                addStyleName("v-scrollable")
                setSizeFull()
                expandRatio = 1f
            }
            statusTicker = label()
        }
    }

    private fun CssLayout.section(caption: String, badge: String? = null, block: Label.()->Unit = {}) {
        label {
            if (badge == null) {
                this.caption = caption
            } else {
                html("""$caption <span class="valo-menu-badge">$badge</span>""")
            }
            primaryStyleName = ValoTheme.MENU_SUBTITLE
            w = wrapContent
            addStyleName(ValoTheme.LABEL_H4)
            block()
        }
    }

    /**
     * Registers a button to a menu with given [icon] and [caption], which launches given [view].
     * @param badge optional badge which is displayed in the button's top-right corner. Usually this is a number, showing number of notifications or such.
     * @param view optional view; if not null, clicking this menu button will launch this view with no parameters; also the button will be marked selected
     * when the view is shown.
     */
    private fun CssLayout.menuButton(icon: Resource, caption: String, badge: String? = null, view: Class<out View>? = null, block: Button.()->Unit = {}) {
        val b = button {
            primaryStyleName = ValoTheme.MENU_ITEM
            this.icon = icon
            if (badge != null) {
                isCaptionAsHtml = true
                this.caption = """$caption <span class="valo-menu-badge">$badge</span>"""
            } else {
                this.caption = caption
            }
            if (view != null) {
                onLeftClick { navigateToView(view) }
                views[view] = this
            }
        }
        b.block()
    }

    override fun showView(view: View) {
        // show the view itself
        viewPlaceholder.removeAllComponents()
        viewPlaceholder.addComponent(view as Component)

        // make the appropriate menu button selected, to show the current view
        views.values.forEach { it.removeStyleName("selected") }
        views[view.javaClass as Class<*>]?.addStyleName("selected")
    }

    private val timer = AtomicInteger()
    @Transient
    private var timerHandle: ScheduledFuture<*>? = null

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
        super.detach()
    }
}
