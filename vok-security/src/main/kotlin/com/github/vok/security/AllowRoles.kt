package com.github.vok.security

/**
 * Allows any user with one of the listed roles to see the view.
 *
 * Examples:
 * * `@AllowRoles` (the [roles] property is an empty array) - no users will qualify and the view remains inaccessible.
 * * `@AllowRoles("maintainer", "operator")` - only users that has either the `maintainer` or `operator` role may see this page.
 *
 * Intended to be attached to Vaadin8 `View` or Vaadin10 `@Route`-annotated component, to restrict which users can see the page.
 * The logic which checks this annotation is present in the `vok-util-vaadin8` or `vok-util-vaadin10` project.
 *
 * Only one of the [AllowRoles], [AllowAll] and [AllowAllUsers] can be present. If more than one is present, it's a configuration error and a [RuntimeException] will be thrown.
 * If none is present, nobody can see that view. This is to prevent views accidentally exposed by omitting the security annotation on them.
 * @property roles the current user must possess any of the roles listed here, in order to pass the authorization. If the [roles] array is empty,
 * no users will qualify and the view remains inaccessible.
 */
@Target(AnnotationTarget.CLASS)
annotation class AllowRoles(vararg val roles: String)

/**
 * Allows anybody to see this view, even if there is no user logged in.
 *
 * Intended to be attached to Vaadin8 `View` or Vaadin10 `@Route`-annotated component, to restrict which users can see the page.
 * The logic which checks this annotation is present in the `vok-util-vaadin8` or `vok-util-vaadin10` project.
 *
 * Only one of the [AllowRoles], [AllowAll] and [AllowAllUsers] can be present. If more than one is present, it's a configuration error and a [RuntimeException] will be thrown.
 * If none is present, nobody can see that view. This is to prevent views accidentally exposed by omitting the security annotation on them.
 */
@Target(AnnotationTarget.CLASS)
annotation class AllowAll

/**
 * Allows any logged-in user to see this view.
 *
 * Intended to be attached to Vaadin8 `View` or Vaadin10 `@Route`-annotated component, to restrict which users can see the page.
 * The logic which checks this annotation is present in the `vok-util-vaadin8` or `vok-util-vaadin10` project.
 *
 * Only one of the [AllowRoles], [AllowAll] and [AllowAllUsers] can be present. If more than one is present, it's a configuration error and a [RuntimeException] will be thrown.
 * If none is present, nobody can see that view. This is to prevent views accidentally exposed by omitting the security annotation on them.
 */
@Target(AnnotationTarget.CLASS)
annotation class AllowAllUsers
