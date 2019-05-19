package net.wuillemin.jds.authserver.config

import java.security.PrivateKey

/**
 * Main class for the configuration of the Authentication
 *
 * @param privateKey The private key to sign the token
 * @param tokenTimeToLiveInSeconds The time to live of an authentication token
 * @param refreshTimeToLiveInSeconds The time to live of a refresh token
 * @param localUsers The definition of the local user
 */
data class AuthServerProperties(
    val privateKey: PrivateKey,
    val tokenTimeToLiveInSeconds: Long,
    val refreshTimeToLiveInSeconds: Long,
    val localUsers: List<LocalUserProperties>
) {

    /**
     * The definition of locally given user
     *
     * @param userId The id of the user
     * @param userName The name of the user
     * @param password The password of the user
     * @param profile The profile of the user
     */
    class LocalUserProperties(
        var userId: Long,
        var userName: String,
        var password: String,
        var profile: String
    )
}
