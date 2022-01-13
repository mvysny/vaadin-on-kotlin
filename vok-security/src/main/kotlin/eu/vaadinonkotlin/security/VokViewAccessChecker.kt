package eu.vaadinonkotlin.security

import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.ViewAccessChecker
import eu.vaadinonkotlin.VaadinOnKotlin
import java.security.Principal
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
        // instead of using
        val userResolver: LoggedInUserResolver =
            VaadinOnKotlin.loggedInUserResolver ?: LoggedInUserResolver.NO_USER
        val principal: Principal? =
            if (userResolver.isLoggedIn()) BasicUserPrincipal("Dummy") else null
        return hasAccess(cls, principal) { role -> userResolver.hasRole(role) }
    }
}

public class VokViewAccessChecker : ViewAccessChecker(VokAccessAnnotationChecker()) {
    init {
        enable()
    }
}
