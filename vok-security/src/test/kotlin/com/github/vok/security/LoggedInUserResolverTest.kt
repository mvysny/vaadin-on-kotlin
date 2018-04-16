package com.github.vok.security

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows

object DummyUserResolver : LoggedInUserResolver {
    var userWithRoles: Set<String>? = null
    override fun isLoggedIn(): Boolean = userWithRoles != null
    override fun getCurrentUserRoles(): Set<String> = userWithRoles ?: setOf()
}

class LoggedInUserResolverTest : DynaTest({
    @AllowAll class MyAllowAll
    @AllowRoles class MyAllowNobody
    @AllowRoles("admin") class MyAllowAdmin
    @AllowRoles("admin", "user") class MyAllowAdminOrUser
    @AllowAllUsers class MyAllowAllUsers

    test("test logged out") {
        DummyUserResolver.userWithRoles = null
        DummyUserResolver.checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, you're not logged in") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAdmin, you're not logged in") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAdminOrUser, you're not logged in") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAllUsers, you're not logged in") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAllUsers::class.java)
        }
    }

    test("test logged in but with no roles") {
        DummyUserResolver.userWithRoles = setOf()
        DummyUserResolver.checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdminOrUser, you are not admin or user") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        DummyUserResolver.checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in user") {
        DummyUserResolver.userWithRoles = setOf("user")
        DummyUserResolver.checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        DummyUserResolver.checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        DummyUserResolver.checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in admin") {
        DummyUserResolver.userWithRoles = setOf("admin")
        DummyUserResolver.checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        DummyUserResolver.checkPermissionsOnClass(MyAllowAdmin::class.java)
        DummyUserResolver.checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        DummyUserResolver.checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in other") {
        DummyUserResolver.userWithRoles = setOf("other")
        DummyUserResolver.checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdminOrUser, you are not admin or user") {
            DummyUserResolver.checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        DummyUserResolver.checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }
})
