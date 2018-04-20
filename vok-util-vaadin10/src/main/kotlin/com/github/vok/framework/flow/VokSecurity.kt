package com.github.vok.framework.flow

import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.security.loggedInUserResolver
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.Route

/**
 * The security provider. Since Vaadin 10 applications typically don't define their own UIs but use the approach of setting [Route.layout],
 * we will provide support for that as well.
 *
 * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 *
 * ## If you have your own UI class
 *
 * Simply call [install] - the function will add the [UI.addBeforeEnterListener] which will check permissions on all views.
 *
 * ## If you do not have your own UI class
 *
 * Then you typically have a main layout to which all your [Route]s reference by the means of [Route.layout]. You just need to call the [checkPermissionsOfView]
 * function before the View is displayed.
 *
 * Make your `MainLayout` class implement the `BeforeEnterObserver`, then implement the `beforeEnter()` function as follows:
 * ```
 * override fun beforeEnter(event: BeforeEnterEvent) {
 *   if (!Session.loginManager.isLoggedIn) {
 *     event.rerouteTo(LoginView::class.java)
 *   } else {
 *     VokSecurity.checkPermissionsOfView(event.navigationTarget)
 *   }
 * }
 * ```
 */
object VokSecurity {
    /**
     * Checks the permissions of given view. If the parent layout is also annotated with [Route], we'll recursively check that as well.
     * @param viewClass the Vaadin 10 view. Must be a [Component] annotated with [Route].
     */
    fun checkPermissionsOfView(viewClass: Class<*>) {
        require(Component::class.java.isAssignableFrom(viewClass)) { "$viewClass is not a subclass of Component" }
        val route = requireNotNull(viewClass.getAnnotation(Route::class.java)) { "The view $viewClass is not annotated with @Route" }
        val user = checkNotNull(VaadinOnKotlin.loggedInUserResolver) { "The VaadinOnKotlin.loggedInUserResolver has not been set" }
        user.checkPermissionsOnClass(viewClass)
        if (route.layout != UI::class && route.layout.java.getAnnotation(Route::class.java) != null) {
            // recursively check for permissions on the parent layout
            checkPermissionsOfView(route.layout.java)
        }
    }

    /**
     * If you have your own custom UI, then simply call this function from your [UI.init] method. It will install [UI.addBeforeEnterListener]
     * and will check all views.
     */
    fun install() {
        UI.getCurrent().addBeforeEnterListener({ e -> checkPermissionsOfView(e.navigationTarget) })
    }
}
