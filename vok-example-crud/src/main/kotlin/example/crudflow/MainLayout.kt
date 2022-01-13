package example.crudflow

import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.RouterLayout
import eu.vaadinonkotlin.vaadin.Session
import java.util.*

/**
 * The main layout, defines the general outlook of the app. All views are then nested inside of this layout.
 */
class MainLayout : VerticalLayout(), RouterLayout {
    init {
        setSizeFull()
        Session.current.locale = Session.current.browser.locale ?: Locale.getDefault()
    }
}
