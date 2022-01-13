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
    /**
     * Checks whether there is an user currently logged in.
     * @return logged-in user if any, null if no user is currently logged in.
     * You can use [BasicUserPrincipal] for convenience.
     */
    public fun getPrincipal(): Principal?

    /**
     * Returns the roles assigned to the currently logged-in user. If there is no user logged in or the user has no roles, the function
     * returns an empty set.
     */
    public fun getCurrentUserRoles(): Set<String>

    /**
     * Checks whether the currently logged-in user contains given [role]. If there is no user currently logged in, the function will always
     * return false.
     */
    public fun hasRole(role: String): Boolean = getCurrentUserRoles().contains(role)

    public companion object {
        /**
         * Represents a resolver with no user logged in.
         */
        public val NO_USER: LoggedInUserResolver =
            object : LoggedInUserResolver {
                override fun getPrincipal(): Principal? = null
                override fun getCurrentUserRoles(): Set<String> = setOf()
            }
    }
}

private var resolver: LoggedInUserResolver? = null

/**
 * The global instance of the resolver.
 *
 * In order for the [AllowRoles]/[AllowAll]/[AllowAllUsers] interfaces to be checked, you need to implement [LoggedInUserResolver] properly and set it here.
 * Yet this is just a helper class. To make Vaadin actually check for permissions, see the `vok-security` `README.md` for more details.
 */
public var VaadinOnKotlin.loggedInUserResolver: LoggedInUserResolver?
    get() = resolver
    set(value) { resolver = value }
