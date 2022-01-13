package eu.vaadinonkotlin.security

import eu.vaadinonkotlin.VaadinOnKotlin

/**
 * Resolves various properties of the current user. Typically the currently logged-in user is stored in Vaadin session, so this function
 * can simply look up the user from the session.
 *
 * Methods on this interface can only be called from the UI thread.
 */
public interface LoggedInUserResolver {
    /**
     * Checks whether there is an user currently logged in.
     * @return true if an user is logged in, false if no user is currently logged in.
     */
    public fun isLoggedIn(): Boolean
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

    /**
     * Checks that given [checkedClass] can be viewed by current user.
     * The [routeClass] is just for a reference: if an exception is thrown, the message
     * will contain [routeClass] as the target route.
     * @param routeClass only for display purposes
     * @param checkedClass the class being checked, usually a `@Route` or a `RouterLayout`.
     */
    public fun checkPermissionsOnClass(routeClass: Class<*>, checkedClass: Class<*>) {
        val annotationClasses = listOf(AllowRoles::class.java, AllowAll::class.java, AllowAllUsers::class.java)
        val annotations: List<Annotation> = annotationClasses.mapNotNull { checkedClass.getAnnotation(it) }
        if (annotations.isEmpty()) {
            throw AccessRejectedException("Route ${routeClass.simpleName}: The class ${checkedClass.simpleName} is missing one of the ${annotationClasses.map { it.simpleName }} annotation", routeClass, checkedClass, setOf())
        }
        require(annotations.size == 1) {
            "The class ${checkedClass.simpleName} contains multiple security annotations which is illegal: $annotations"
        }
        val annotation = annotations[0]
        when(annotation) {
            is AllowAll -> {} // okay
            is AllowAllUsers -> if (!isLoggedIn()) {
                throw AccessRejectedException("Route ${routeClass.simpleName}: Cannot access ${checkedClass.simpleName}, you're not logged in", routeClass, checkedClass, setOf())
            }
            is AllowRoles -> {
                if (!isLoggedIn()) {
                    throw AccessRejectedException("Route ${routeClass.simpleName}: Cannot access ${checkedClass.simpleName}, you're not logged in", routeClass, checkedClass, setOf())
                }
                val requiredRoles: Set<String> = annotation.value.toSet()
                if (requiredRoles.isEmpty()) {
                    throw AccessRejectedException("Route ${routeClass.simpleName}: Cannot access ${checkedClass.simpleName}, nobody can access it", routeClass, checkedClass, setOf())
                }
                val currentUserRoles: Set<String> = getCurrentUserRoles()
                if (requiredRoles.intersect(currentUserRoles).isEmpty()) {
                    throw AccessRejectedException("Route ${routeClass.simpleName}: Can not access ${checkedClass.simpleName}, you are not ${requiredRoles.joinToString(" or ")}", routeClass, checkedClass, requiredRoles)
                }
            }
        }
    }

    public companion object {
        public val NO_USER: LoggedInUserResolver =
            object : LoggedInUserResolver {
                override fun isLoggedIn(): Boolean = false
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
