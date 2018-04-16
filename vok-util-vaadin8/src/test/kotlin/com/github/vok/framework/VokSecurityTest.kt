package com.github.vok.framework

import com.github.karibu.testing.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.github.vok.karibudsl.AutoView
import com.github.vok.karibudsl.autoDiscoverViews
import com.github.vok.karibudsl.autoViewProvider
import com.github.vok.karibudsl.navigateToView
import com.github.vok.security.AccessRejectedException
import com.github.vok.security.HasRoles
import com.github.vok.security.LoggedInUserResolver
import com.github.vok.security.loggedInUserResolver
import com.vaadin.navigator.Navigator
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.Label
import com.vaadin.ui.UI

class MySecuredUI : UI() {
    override fun init(request: VaadinRequest) {
        navigator = Navigator(this, this)
        navigator.addProvider(autoViewProvider)
        VokSecurity.install()
    }
}

@AutoView("") @HasRoles
class UnsecuredView : View, Label("Anybody can see this")
@AutoView("secured") @HasRoles("admin")
class SecuredView : View, Label("Only admin can see this")
@AutoView("omitted")
class OmittedAnnotationView : View, Label("A new view which has the @HasRoles annotation omitted. Nobody should be able to navigate here")
@AutoView("document") @HasRoles
class CustomAuthorizationLogicView : View, Label("A custom authorization logic") {
    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
        throw AccessRejectedException("Simulated access rejected exception", javaClass, setOf("document-viewer"))
    }
}

object DummyUserResolver : LoggedInUserResolver {
    var userWithRoles: Set<String>? = null
    override fun isLoggedIn(): Boolean = userWithRoles != null
    override fun getCurrentUserRoles(): Set<String> = userWithRoles ?: setOf()
}

class VokSecurityTest : DynaTest({
    beforeGroup { VaadinOnKotlin.loggedInUserResolver = DummyUserResolver }
    beforeEach {
        autoDiscoverViews("com.github.vok")
        MockVaadin.setup({ MySecuredUI() })
    }
    group("test no user / anonymous") {
        beforeGroup { DummyUserResolver.userWithRoles = null }
        test("unsecured succeeds") {
            navigateToView(UnsecuredView::class.java)
        }
        test("secured fails") {
            expectThrows(AccessRejectedException::class, "Can not access SecuredView, you are not [admin]") {
                navigateToView(SecuredView::class.java)
            }
        }
        test("omitted fails") {
            expectThrows(AccessRejectedException::class, "The view OmittedAnnotationView is missing the @HasRoles annotation, can not access") {
                navigateToView(OmittedAnnotationView::class.java)
            }
        }
        test("document fails") {
            expectThrows(AccessRejectedException::class, "Simulated access rejected exception") {
                navigateToView(CustomAuthorizationLogicView::class.java)
            }
        }
    }

    group("test user with no roles") {
        beforeGroup { DummyUserResolver.userWithRoles = setOf() }
        test("unsecured succeeds") {
            navigateToView(UnsecuredView::class.java)
        }
        test("secured fails") {
            expectThrows(AccessRejectedException::class, "Can not access SecuredView, you are not [admin]") {
                navigateToView(SecuredView::class.java)
            }
        }
        test("omitted fails") {
            expectThrows(AccessRejectedException::class, "The view OmittedAnnotationView is missing the @HasRoles annotation, can not access") {
                navigateToView(OmittedAnnotationView::class.java)
            }
        }
        test("document fails") {
            expectThrows(AccessRejectedException::class, "Simulated access rejected exception") {
                navigateToView(CustomAuthorizationLogicView::class.java)
            }
        }
    }

    group("test user with role of 'user'") {
        beforeGroup { DummyUserResolver.userWithRoles = setOf("user") }
        test("unsecured succeeds") {
            navigateToView(UnsecuredView::class.java)
        }
        test("secured fails") {
            expectThrows(AccessRejectedException::class, "Can not access SecuredView, you are not [admin]") {
                navigateToView(SecuredView::class.java)
            }
        }
        test("omitted fails") {
            expectThrows(AccessRejectedException::class, "The view OmittedAnnotationView is missing the @HasRoles annotation, can not access") {
                navigateToView(OmittedAnnotationView::class.java)
            }
        }
        test("document fails") {
            expectThrows(AccessRejectedException::class, "Simulated access rejected exception") {
                navigateToView(CustomAuthorizationLogicView::class.java)
            }
        }
    }

    group("test user with role of 'user', 'admin'") {
        beforeGroup { DummyUserResolver.userWithRoles = setOf("user", "admin") }
        test("unsecured succeeds") {
            navigateToView(UnsecuredView::class.java)
        }
        test("secured succeeds") {
            navigateToView(SecuredView::class.java)
        }
        test("omitted fails") {
            expectThrows(AccessRejectedException::class, "The view OmittedAnnotationView is missing the @HasRoles annotation, can not access") {
                navigateToView(OmittedAnnotationView::class.java)
            }
        }
        test("document fails") {
            expectThrows(AccessRejectedException::class, "Simulated access rejected exception") {
                navigateToView(CustomAuthorizationLogicView::class.java)
            }
        }
    }
})
