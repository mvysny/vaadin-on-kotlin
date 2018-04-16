package com.github.vok.security

/**
 * An exception thrown when the current user does not have access to given view.
 *
 * You are responsible for catching of this exception in Vaadin 8's `ErrorHandler` or Vaadin 10's `HasErrorParameter`.
 *
 * Also, you can throw this exception when you implement your custom authorization logic in the `View`'s `AfterNavigationHandler.afterNavigation()` (Vaadin 10) or `View.enter` (Vaadin 8).
 * For example, often the View takes an ID of a document as a parameter and you need to check whether
 * the current user can access that particular document. This case can not be handled by the simple [HasRoles] logic.
 */
open class AccessRejectedException(message: String) : Exception(message)
