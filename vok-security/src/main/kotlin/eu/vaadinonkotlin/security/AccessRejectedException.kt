package eu.vaadinonkotlin.security

/**
 * An exception thrown when the current user does not have access to given view.
 * Note that vok-security will not throw this exception:
 * you should throw this exception when you implement your custom authorization logic in
 * the `View`'s `AfterNavigationHandler.afterNavigation()`.
 * For example, often the View takes an ID of a document as a parameter and you need to check whether
 * the current user can access that particular document. This case can not be handled by the simple [AllowRoles] logic.
 *
 * You are responsible for catching of this exception in Vaadin's `HasErrorParameter`.
 * @property viewClass the view which was navigated to. null if the exception was not thrown upon navigation but rather on e.g. button click
 * @property missingRoles which roles were missing. May be empty if the exception is thrown because the [AllowRoles] annotation is missing on the view, or there
 * is some other reason for which the set of missing roles can not be provided.
 */
public open class AccessRejectedException(
    message: String,
    public val viewClass: Class<*>?,
    public val missingRoles: Set<String>
) : Exception(message)
