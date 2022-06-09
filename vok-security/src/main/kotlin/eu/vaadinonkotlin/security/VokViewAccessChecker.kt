package eu.vaadinonkotlin.security

import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.ViewAccessChecker
import eu.vaadinonkotlin.VaadinOnKotlin
import java.security.Principal
import java.util.function.Function
import javax.servlet.http.HttpServletRequest

/**
 * Completely bypasses the Servlet Container security. Instead of checking the outcome of
 * [HttpServletRequest.getUserPrincipal] and [HttpServletRequest.isUserInRole],
 * it checks the [VaadinOnKotlin.loggedInUserResolver] instead.
 */
public object VokAccessAnnotationChecker : AccessAnnotationChecker() {
    override fun hasAccess(
        cls: Class<*>,
        request: HttpServletRequest
    ): Boolean {
        val userResolver: LoggedInUserResolver = VaadinOnKotlin.loggedInUserResolver
        return hasAccess(cls, userResolver.getCurrentUser()) { role -> userResolver.hasRole(role) }
    }
}

/**
 * Checks that a user is logged in. Uses standard Vaadin [ViewAccessChecker] but
 * obtains the user from [VaadinOnKotlin.loggedInUserResolver] rather than from
 * [HttpServletRequest.getUserPrincipal] and [HttpServletRequest.isUserInRole].
 *
 * * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 * * Install this as a [com.vaadin.flow.router.BeforeEnterListener] into your UI,
 *   usually via the [com.vaadin.flow.server.VaadinServiceInitListener].
 *
 * See [vok-security README](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-security/README.md)
 * on how to use this class properly.
 */
public class VokViewAccessChecker : ViewAccessChecker(VokAccessAnnotationChecker) {
    init {
        enable()
    }

    // Vaadin 23.0.x uses AccessAnnotationChecker to get principal.
    // Vaadin 23.1.0 retrieves the principal this way.
    override fun getPrincipal(request: VaadinRequest?): Principal? = VaadinOnKotlin.loggedInUserResolver.getCurrentUser()
    override fun getRolesChecker(request: VaadinRequest?): Function<String, Boolean> = Function { role ->
        VaadinOnKotlin.loggedInUserResolver.getCurrentUserRoles().contains(role)
    }
}
