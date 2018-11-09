package eu.vaadinonkotlin.security

import eu.vaadinonkotlin.VaadinOnKotlin

/**
 * Resolves various properties of the current user. Typically the currently logged-in user is stored in Vaadin session, so this function
 * can simply look up the user from the session.
 *
 * Methods on this interface can only be called from the UI thread.
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
     * Checks whether the currently logged-in user contains given [role]. If there is no user currently logged in, the function will always
     * return false.
     */
    fun hasRole(role: String): Boolean = getCurrentUserRoles().contains(role)

    /**
     * Checks that given `View` can be viewed by current user.
     */
    fun checkPermissionsOnClass(viewClass: Class<*>) {
        val annotationClasses = listOf(AllowRoles::class.java, AllowAll::class.java, AllowAllUsers::class.java)
        val annotations: List<Annotation> = annotationClasses.mapNotNull { viewClass.getAnnotation(it) }
        if (annotations.isEmpty()) {
            throw AccessRejectedException("The view ${viewClass.simpleName} is missing one of the ${annotationClasses.map { it.simpleName }} annotation", viewClass, setOf())
        }
        require(annotations.size == 1) {
            "The view ${viewClass.simpleName} contains multiple security annotations which is illegal: $annotations"
        }
        val annotation = annotations[0]
        when(annotation) {
            is AllowAll -> {} // okay
            is AllowAllUsers -> if (!isLoggedIn()) throw AccessRejectedException("Cannot access ${viewClass.simpleName}, you're not logged in", viewClass, setOf())
            is AllowRoles -> {
                if (!isLoggedIn()) {
                    throw AccessRejectedException("Cannot access ${viewClass.simpleName}, you're not logged in", viewClass, setOf())
                }
                val requiredRoles: Set<String> = annotation.roles.toSet()
                if (requiredRoles.isEmpty()) {
                    throw AccessRejectedException("Cannot access ${viewClass.simpleName}, nobody can access it", viewClass, setOf())
                }
                val currentUserRoles: Set<String> = getCurrentUserRoles()
                if (requiredRoles.intersect(currentUserRoles).isEmpty()) {
                    throw AccessRejectedException("Can not access ${viewClass.simpleName}, you are not ${requiredRoles.joinToString(" or ")}", viewClass, requiredRoles)
                }
            }
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
var VaadinOnKotlin.loggedInUserResolver: LoggedInUserResolver?
    get() = resolver
    set(value) { resolver = value }
