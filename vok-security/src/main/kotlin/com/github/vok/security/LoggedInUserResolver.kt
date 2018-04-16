package com.github.vok.security

import com.github.vok.framework.VaadinOnKotlin

/**
 * Resolves various properties of the current user. Typically the currently logged-in user is stored in Vaadin session, so this function
 * can simply look up the user from the session.
 */
interface LoggedInUserResolver {
    /**
     * Checks whether there is an user currently logged in.
     * @return true if an user is logged in, false if no user is currently logged in.
     */
    fun isLoggedIn(): Boolean
    /**
     * Returns the roles assigned to the currently logged-in user. If there is no user logged in or the user has no roles, the function
     * returns an empty set.
     */
    fun getCurrentUserRoles(): Set<String>

    /**
     * Checks whether the currently logged-in user contains all of given [roles]. If [roles] is empty, this function is a no-op and always returns true.
     */
    fun hasRoles(vararg roles: String): Boolean = getCurrentUserRoles().containsAll(roles.toSet())
}

private var resolver: LoggedInUserResolver? = null

/**
 * The global instance of the resolver.
 */
var VaadinOnKotlin.loggedInUserResolver: LoggedInUserResolver?
    get() = resolver
    set(value) { resolver = value }
