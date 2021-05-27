package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import eu.vaadinonkotlin.VaadinOnKotlin
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.router.ParentLayout
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLayout
import eu.vaadinonkotlin.security.*

/**
 * A view with no parent layout.
 */
@AllowRoles("admin")
@Route("admin")
class AdminView : VerticalLayout()

@AllowAll
class MyLayout : VerticalLayout(), RouterLayout

@AllowRoles()
class RejectAllLayout : VerticalLayout(), RouterLayout

/**
 * This layout can not be effectively viewed with anybody, because of [RejectAllLayout]
 */
@ParentLayout(RejectAllLayout::class)
@AllowAll
class MyIntermediateLayout: VerticalLayout(), RouterLayout

/**
 * A view with parent layout.
 */
@Route("user", layout = MyLayout::class)
@AllowRoles("user")
class UserView : VerticalLayout()

@AllowRoles("sales")
class SalesLayout : VerticalLayout(), RouterLayout

/**
 * This view can not be effectively viewed with 'user' since its parent layout lacks the 'user' role.
 */
@AllowRoles("sales", "user")
@Route("sales/sale", layout = SalesLayout::class)
class SalesView : VerticalLayout()

/**
 * This view can not be effectively viewed with anybody, because of [RejectAllLayout]
 */
@AllowRoles("sales", "user")
@Route("rejectall", layout = MyIntermediateLayout::class)
class RejectAllView : VerticalLayout()

object DummyUserResolver : LoggedInUserResolver {
    var userWithRoles: Set<String>? = null
    override fun isLoggedIn(): Boolean = userWithRoles != null
    override fun getCurrentUserRoles(): Set<String> = userWithRoles ?: setOf()
}

class VokSecurityTest : DynaTest({
    beforeGroup { VaadinOnKotlin.loggedInUserResolver = DummyUserResolver }
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    group("checkPermissionsOfView()") {
        test("no user logged in") {
            DummyUserResolver.userWithRoles = null
            expectThrows(AccessRejectedException::class, "Route AdminView: Cannot access AdminView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Route UserView: Cannot access UserView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Route SalesView: Cannot access SalesView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
            VokSecurity.checkPermissionsOfView(InternalServerError::class.java) // always allow to display this
            expectThrows(AccessRejectedException::class, "Route RejectAllView: Cannot access RejectAllView, you're not logged in") {
                VokSecurity.checkPermissionsOfView(RejectAllView::class.java)
            }
        }
        test("admin logged in") {
            DummyUserResolver.userWithRoles = setOf("admin")
            VokSecurity.checkPermissionsOfView(AdminView::class.java)
            expectThrows(AccessRejectedException::class, "Route UserView: Can not access UserView, you are not user") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Route SalesView: Can not access SalesView, you are not sales or user") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
            VokSecurity.checkPermissionsOfView(InternalServerError::class.java) // always allow to display this
            expectThrows(AccessRejectedException::class, "Can not access RejectAllView, you are not sales or user") {
                VokSecurity.checkPermissionsOfView(RejectAllView::class.java)
            }
        }
        test("user logged in") {
            DummyUserResolver.userWithRoles = setOf("user")
            expectThrows(AccessRejectedException::class, "Route AdminView: Can not access AdminView, you are not admin") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            VokSecurity.checkPermissionsOfView(UserView::class.java)
            expectThrows(AccessRejectedException::class, "Route SalesView: Can not access SalesLayout, you are not sales") {
                VokSecurity.checkPermissionsOfView(SalesView::class.java)
            }
            VokSecurity.checkPermissionsOfView(InternalServerError::class.java) // always allow to display this
            expectThrows(AccessRejectedException::class, "Route RejectAllView: Cannot access RejectAllLayout, nobody can access it") {
                VokSecurity.checkPermissionsOfView(RejectAllView::class.java)
            }
        }
        test("sales logged in") {
            DummyUserResolver.userWithRoles = setOf("sales")
            expectThrows(AccessRejectedException::class, "Route AdminView: Can not access AdminView, you are not admin") {
                VokSecurity.checkPermissionsOfView(AdminView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Route UserView: Can not access UserView, you are not user") {
                VokSecurity.checkPermissionsOfView(UserView::class.java)
            }
            expectThrows(AccessRejectedException::class, "Route RejectAllView: Cannot access RejectAllLayout, nobody can access it") {
                VokSecurity.checkPermissionsOfView(RejectAllView::class.java)
            }
            VokSecurity.checkPermissionsOfView(SalesView::class.java)
            VokSecurity.checkPermissionsOfView(InternalServerError::class.java) // always allow to display this
        }
    }
})
