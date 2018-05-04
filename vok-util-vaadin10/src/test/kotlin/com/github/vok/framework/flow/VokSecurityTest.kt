package com.github.vok.framework.flow

import com.github.karibu.testing.v10.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.security.AccessRejectedException
import com.github.vok.security.AllowRoles
import com.github.vok.security.LoggedInUserResolver
import com.github.vok.security.loggedInUserResolver
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLayout

/**
 * A view with no parent layout.
 */
@AllowRoles("admin")
@Route("admin")
class AdminView : VerticalLayout()

class MyLayout : VerticalLayout(), RouterLayout

/**
 * A view with parent layout. The parent layout is missing the AllowRoles but also it's not a Route so it be ignored.
 */
@Route("user", layout=MyLayout::class)
@AllowRoles("user")
class UserView : VerticalLayout()

@Route("sales")
@AllowRoles("sales")
class SalesLayout : VerticalLayout(), RouterLayout

/**
 * This view can not be effectively viewed with 'user' since its parent layout lacks the 'user' role.
 */
@AllowRoles("sales", "user")
@Route("sales/sale", layout=SalesLayout::class)
class SalesView : VerticalLayout()

object DummyUserResolver : LoggedInUserResolver {
    var userWithRoles: Set<String>? = null
    override fun isLoggedIn(): Boolean = userWithRoles != null
    override fun getCurrentUserRoles(): Set<String> = userWithRoles ?: setOf()
}

class VokSecurityTest : DynaTest({
    beforeGroup { VaadinOnKotlin.loggedInUserResolver = DummyUserResolver }
    group("checkPermissionsOfView()") {
        beforeEach { MockVaadin.setup() }
        test("no user logged in") {
            DummyUserResolver.userWithRoles = null
            expectThrows(AccessRejectedException::class, "Cannot access AdminView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Cannot access UserView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Cannot access SalesLayout, you're not logged in") {
                VokSecurity.checkPermissionsOfView(SalesLayout::class.java)
            }
            expectThrows(AccessRejectedException::class, "Cannot access SalesView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
        }
        test("admin logged in") {
            DummyUserResolver.userWithRoles = setOf("admin")
            VokSecurity.checkPermissionsOfView(AdminView::class.java)
            expectThrows(AccessRejectedException::class, "Can not access UserView, you are not user") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Can not access SalesLayout, you are not sales") {
                VokSecurity.checkPermissionsOfView(SalesLayout::class.java)
            }
            expectThrows(AccessRejectedException::class, "Can not access SalesView, you are not sales or user") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
        }
        test("user logged in") {
            DummyUserResolver.userWithRoles = setOf("user")
            expectThrows(AccessRejectedException::class, "Can not access AdminView, you are not admin") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            VokSecurity.checkPermissionsOfView(UserView::class.java)
            expectThrows(AccessRejectedException::class, "Can not access SalesLayout, you are not sales") {
                VokSecurity.checkPermissionsOfView(SalesLayout::class.java)
            }
            expectThrows(AccessRejectedException::class, "Can not access SalesLayout, you are not sales") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
        }
        test("sales logged in") {
            DummyUserResolver.userWithRoles = setOf("sales")
            expectThrows(AccessRejectedException::class, "Can not access AdminView, you are not admin") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Can not access UserView, you are not user") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            VokSecurity.checkPermissionsOfView(SalesLayout::class.java)
            VokSecurity.checkPermissionsOfView(SalesView::class.java)
        }
    }
})
