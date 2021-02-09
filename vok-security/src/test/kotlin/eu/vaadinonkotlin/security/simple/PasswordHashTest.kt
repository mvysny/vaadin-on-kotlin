package eu.vaadinonkotlin.security.simple

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class PasswordHashTest : DynaTest({
    test("matching password") {
        expect(true) {
            PasswordHash.validatePassword("foo", PasswordHash.createHash("foo"))
        }
    }

    test("non-matching password") {
        expect(false) {
            PasswordHash.validatePassword("bar", PasswordHash.createHash("foo"))
        }
    }

    test("different salt") {
        val hash1 = PasswordHash.createHash("foo".toCharArray(), "salt1".toByteArray())
        val hash2 = PasswordHash.createHash("foo".toCharArray(), "salt2".toByteArray())
        expect(false, "$hash1, $hash2") { hash1 == hash2 }
    }

    test("different password") {
        val hash1 = PasswordHash.createHash("foo".toCharArray(), "salt1".toByteArray())
        val hash2 = PasswordHash.createHash("bar".toCharArray(), "salt1".toByteArray())
        expect(false, "$hash1, $hash2") { hash1 == hash2 }
    }
})
