package com.github.vok.security

/**
 * Intended to be attached to Vaadin8 `View` or Vaadin10 `@Route`-annotated component, to restrict which user can see the page.
 *
 * * If this annotation is absent from the view, nobody can see that view. This is to prevent views accidentally exposed by omitting the security annotation on them.
 * * If this annotation is present and has no roles, then anybody *including logged-out/anonymous* users may see the view. This is useful for general-purpose views such as
 *   login/error views, or views accessible by anybody.
 * * `@HasRoles("maintainer", "operator")` - only the user that is both a `maintainer` and `operator` may see this page.
 *
 * The logic which checks this annotation is present in the `vok-util-vaadin8` or `vok-util-vaadin10` project.
 * @property roles the current user must possess all of the roles listed here, in order to pass the authorization.
 */
@Target(AnnotationTarget.CLASS)
annotation class HasRoles(vararg val roles: String)
