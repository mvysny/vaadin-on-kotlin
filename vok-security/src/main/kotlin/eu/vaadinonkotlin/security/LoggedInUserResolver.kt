package eu.vaadinonkotlin.security

import eu.vaadinonkotlin.VaadinOnKotlin
import java.security.Principal

/**
 * Resolves various properties of the current user. Typically, the currently
 * logged-in user is stored in Vaadin session, so this function
 * can simply look up the user from the session.
 *
 * Methods on this interface can only be called from the UI thread.
 */
public interface LoggedInUserResolver {
    public val isLoggedIn: Boolean get() = getCurrentUser() != null

    /**
     * Checks whether there is a user currently logged in.
     * @return logged-in user if any, null if no user is currently logged in.
     * You can use [BasicUserPrincipal] for convenience.
     */
    public fun getCurrentUser(): Principal?

    /**
     * Returns the roles assigned to the currently logged-in user.
     * If there is no user logged in or the user has no roles, the function
     * returns an empty set.
     */
    public fun getCurrentUserRoles(): Set<String>

    /**
     * Checks whether the currently logged-in user contains given [role].
     * If there is no user currently logged in, the function will always
     * return false.
     */
    public fun hasRole(role: String): Boolean = getCurrentUserRoles().contains(role)

    public companion object {
        /**
         * Represents a resolver with no user logged in.
         */
        public val NO_USER: LoggedInUserResolver =
            object : LoggedInUserResolver {
                override fun getCurrentUser(): Principal? = null
                override fun getCurrentUserRoles(): Set<String> = setOf()
            }
    }
}

private var resolver: LoggedInUserResolver = LoggedInUserResolver.NO_USER

/**
 * The global instance of the resolver.
 *
 * In order for the [AllowRoles]/[AllowAll]/[AllowAllUsers] interfaces to be checked,
 * you need to implement [LoggedInUserResolver] properly and set it here.
 * To make Vaadin actually check for permissions, you need to register [VokViewAccessChecker]
 * as a `BeforeEnterListener`.
 */
public var VaadinOnKotlin.loggedInUserResolver: LoggedInUserResolver
    get() = resolver
    set(value) { resolver = value }
