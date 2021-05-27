package eu.vaadinonkotlin.security

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.router.ParentLayout
import com.vaadin.flow.router.Route
import eu.vaadinonkotlin.VaadinOnKotlin

/**
 * The security provider. Since Vaadin 10 applications typically don't define their own UIs but use the approach of setting [Route.layout],
 * we will provide support for that as well.
 *
 * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 *
 * See [vok-security README](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-security/README.md)
 * on how to use this class properly.
 */
public object VokSecurity {
    /**
     * Checks the permissions of given view. If the parent layout is also annotated with [Route], we'll recursively check that as well.
     * @param viewClass the Vaadin 10 view. Must be a [Component] annotated with [Route].
     */
    public fun checkPermissionsOfView(viewClass: Class<*>) {
        if (viewClass == InternalServerError::class.java) {
            // allow
            return
        }
        requireNotNull(viewClass.getAnnotation(Route::class.java)) { "The view $viewClass is not annotated with @Route" }
        checkPermissions(viewClass, viewClass)
    }

    private fun checkPermissions(routeClass: Class<*>, checkedClazz: Class<*>) {
        require(Component::class.java.isAssignableFrom(checkedClazz)) { "$checkedClazz is not a subclass of Component" }
        // check permissions
        val user = checkNotNull(VaadinOnKotlin.loggedInUserResolver) { "The VaadinOnKotlin.loggedInUserResolver has not been set" }
        user.checkPermissionsOnClass(routeClass, checkedClazz)

        // check parent layout if necessary
        val route: Route? = checkedClazz.getAnnotation(Route::class.java)
        val parentLayout: ParentLayout? = checkedClazz.getAnnotation(ParentLayout::class.java)
        if (route != null && route.layout != UI::class) {
            // recursively check for permissions on the parent layout
            checkPermissions(routeClass, route.layout.java)
        }
        if (parentLayout != null) {
            // recursively check for permissions on the parent layout
            checkPermissions(routeClass, parentLayout.value.java)
        }
    }
}
