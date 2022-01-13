package eu.vaadinonkotlin.security

import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.ViewAccessChecker
import eu.vaadinonkotlin.VaadinOnKotlin
import javax.servlet.http.HttpServletRequest

/**
 * Completely bypasses the Servlet Container security. Instead of checking the outcome of
 * [HttpServletRequest.getUserPrincipal] and [HttpServletRequest.isUserInRole],
 * it checks the [VaadinOnKotlin.loggedInUserResolver] instead.
 */
public class VokAccessAnnotationChecker : AccessAnnotationChecker() {
    override fun hasAccess(
        cls: Class<*>,
        request: HttpServletRequest
    ): Boolean {
        val userResolver: LoggedInUserResolver =
            VaadinOnKotlin.loggedInUserResolver ?: LoggedInUserResolver.NO_USER
        return hasAccess(cls, userResolver.getPrincipal()) { role -> userResolver.hasRole(role) }
    }
}

/**
 * Checks that an user is logged in. Uses standard Vaadin [ViewAccessChecker] but
 * obtains the user from [VaadinOnKotlin.loggedInUserResolver] rather than from
 * [HttpServletRequest.getUserPrincipal] and [HttpServletRequest.isUserInRole].
 *
 * * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 * * Install this as a [com.vaadin.flow.router.BeforeEnterListener] into your UI,
 *   usually via the [com.vaadin.flow.server.VaadinServiceInitListener].
 */
public class VokViewAccessChecker : ViewAccessChecker(VokAccessAnnotationChecker()) {
    init {
        enable()
    }
}
