package eu.vaadinonkotlin.security

import java.io.Serializable
import java.security.Principal

/**
 * A very simple [Principal] implementation.
 */
public data class BasicUserPrincipal(val username: String) : Principal, Serializable {
    override fun getName(): String = username
}
