package example.crudflow

import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.BodySize
import com.vaadin.flow.component.page.Viewport
import com.vaadin.flow.router.RouterLayout
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo

/**
 * The main layout, defines the general outlook of the app. All views are then nested inside of this layout.
 */
@BodySize(width = "100vw", height = "100vh")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(Lumo::class)
class MainLayout : VerticalLayout(), RouterLayout {
    init {
        setSizeFull()
        VaadinSession.getCurrent().locale = VaadinSession.getCurrent().browser.locale
    }
}