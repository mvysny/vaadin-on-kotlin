package eu.vaadinonkotlin.security

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows

object DummyUserResolver : LoggedInUserResolver {
    var userWithRoles: Set<String>? = null
    override fun isLoggedIn(): Boolean = userWithRoles != null
    override fun getCurrentUserRoles(): Set<String> = userWithRoles ?: setOf()
}

private fun checkPermissionsOnClass(clazz: Class<*>) {
    DummyUserResolver.checkPermissionsOnClass(clazz, clazz)
}

class LoggedInUserResolverTest : DynaTest({
    @AllowAll
    class MyAllowAll
    @AllowRoles
    class MyAllowNobody
    @AllowRoles("admin") class MyAllowAdmin
    @AllowRoles("admin", "user") class MyAllowAdminOrUser
    @AllowAllUsers
    class MyAllowAllUsers

    test("test logged out") {
        DummyUserResolver.userWithRoles = null
        checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, you're not logged in") {
            checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAdmin, you're not logged in") {
            checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAdminOrUser, you're not logged in") {
            checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowAllUsers, you're not logged in") {
            checkPermissionsOnClass(MyAllowAllUsers::class.java)
        }
    }

    test("test logged in but with no roles") {
        DummyUserResolver.userWithRoles = setOf()
        checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdminOrUser, you are not admin or user") {
            checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in user") {
        DummyUserResolver.userWithRoles = setOf("user")
        checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in admin") {
        DummyUserResolver.userWithRoles = setOf("admin")
        checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        checkPermissionsOnClass(MyAllowAdmin::class.java)
        checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }

    test("test logged in other") {
        DummyUserResolver.userWithRoles = setOf("other")
        checkPermissionsOnClass(MyAllowAll::class.java)
        expectThrows(AccessRejectedException::class, "Cannot access MyAllowNobody, nobody can access it") {
            checkPermissionsOnClass(MyAllowNobody::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdmin, you are not admin") {
            checkPermissionsOnClass(MyAllowAdmin::class.java)
        }
        expectThrows(AccessRejectedException::class, "Can not access MyAllowAdminOrUser, you are not admin or user") {
            checkPermissionsOnClass(MyAllowAdminOrUser::class.java)
        }
        checkPermissionsOnClass(MyAllowAllUsers::class.java)
    }
})
