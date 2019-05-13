package net.wuillemin.jds.common.dto

/**
 * Message for asking the authentication service to refresh a token
 *
 * @param refreshToken The refresh token
 */
data class RefreshRequest(
    val refreshToken: String
)