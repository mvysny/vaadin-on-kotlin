package eu.vaadinonkotlin.security

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.router.ParentLayout
import com.vaadin.flow.router.Route
import eu.vaadinonkotlin.VaadinOnKotlin
import java.io.Serializable
import java.security.Principal

/**
 * The security provider. Since Vaadin 10 applications typically don't define their own UIs but use the approach of setting [Route.layout],
 * we will provide support for that as well.
 *
 * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 *
 * See [vok-security README](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-security/README.md)
 * on how to use this class properly.
 */

public data class BasicUserPrincipal(val username: String) : Principal, Serializable {
    override fun getName(): String = username
}
