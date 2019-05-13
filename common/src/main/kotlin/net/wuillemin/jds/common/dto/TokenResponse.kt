package net.wuillemin.jds.common.dto

/**
 * Define a response from the authentication service. This response have all the information necessary
 * to make subsequent calls to the services
 *
 * @param authenticationToken The authentication token that must be sent with each query. This token is a JWT token
 * @param authenticationTokenExpiration The expiration of the authentication token. The expiration is given in seconds
 * since EPOCH
 * @param refreshToken The refresh token that must be sent to the service for refreshing the authentication token.
 * There is no specific format
 * @param refreshTokenExpiration The expiration of the refresh token. The expiration is given in seconds since EPOCH
 */
data class TokenResponse(
    val authenticationToken: String,
    val authenticationTokenExpiration: Long,
    val refreshToken: String,
    val refreshTokenExpiration: Long
)
