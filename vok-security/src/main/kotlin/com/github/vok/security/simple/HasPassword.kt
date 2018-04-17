package com.github.vok.security.simple

/**
 * This mixin interface makes sure that the database-stored passwords are properly hashed and not stored in plaintext.
 * This makes it impossible to guess the user's password even if the database gets compromised.
 *
 * Simply create an `User` entity and make it implement this interface. The [hashedPassword] field should be stored in the database
 * as-is: it is the user's database hashed and salted and there is no practical way to obtain the original password from it.
 * When the user registers, simply call [setPassword] with the user-provided password. The password will be hashed and the [hashedPassword] field will be populated.
 *
 * After the registration, when the user tries to log in, simply call [passwordMatches] with the user-provided password, to check whether
 * the user provided a correct password or not.
 *
 * You can see the example of this mixin interface in the [vok-security-demo User.kt](https://github.com/mvysny/vok-security-demo/blob/master/web/src/main/kotlin/com/example/vok/User.kt)
 * class.
 */
interface HasPassword {
    var hashedPassword: String

    /**
     * Checks if the [password] provided by the user at login matches with whatever password user provided during the registration.
     */
    fun passwordMatches(password: String) = PasswordHash.validatePassword(password, hashedPassword)

    /**
     * When the user attempts to change the password, or a new user is created, call this function with the user-provided [password]
     * to populate the [hashedPassword] field properly.
     */
    fun setPassword(password: String) {
        hashedPassword = PasswordHash.createHash(password)
    }
}
