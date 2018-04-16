package com.github.vok.security.simple

/**
 * This mixin interface makes sure that the database-stored passwords are properly hashed and not stored in plaintext,
 * so that it is impossible to guess the user's password.
 *
 * Simply create an `User` entity and make it implement this interface. The [hashedPassword] field should be stored in the database
 * as-is: it is the user's database hashed and salted and there is no practical way to obtain the original password from it.
 *
 * When the user registers, simply call [setPassword] with the user-provided password. The password will be hashed and the [hashedPassword] field will be populated.
 *
 * After the registration, when the user tries to log in, simply call [passwordMatches] with the user-provided password, to check whether
 * the user provided a correct password or not.
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
