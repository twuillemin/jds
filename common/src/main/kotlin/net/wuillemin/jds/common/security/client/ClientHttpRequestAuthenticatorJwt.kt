package net.wuillemin.jds.common.security.client

import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.dto.RefreshRequest
import net.wuillemin.jds.common.dto.TokenRequest
import net.wuillemin.jds.common.dto.TokenResponse
import net.wuillemin.jds.common.exception.ClientException
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_BEARER_PREFIX
import net.wuillemin.jds.common.security.ServerReference
import org.slf4j.Logger
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant

/**
 * Service for authenticating against server needing a JWT authentication
 *
 * @param commonProperties The properties of the application
 * @param logger The logger
 */
@Service
class ClientHttpRequestAuthenticatorJwt(
    private val commonProperties: CommonProperties,
    private val logger: Logger
) : ClientHttpRequestAuthenticator {

    private var restTemplate = RestTemplate()

    // Keep the authentication information
    private val authenticationByServer = buildAuthenticatorMap()

    override fun addAuthentication(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ) {

        // Get the id of the server
        val serverId = ServerReference(request.uri.host, request.uri.port)

        // Get the authentication if any
        val existingAuthentication = authenticationByServer[serverId]

        // Apply the authentication if any available
        existingAuthentication?.let { request.headers[HTTP_HEADER_AUTHORIZATION] = HTTP_HEADER_BEARER_PREFIX + getValidJWTToken(it) }
    }

    /**
     * Construct the map referencing all authentication method by their server
     */
    private fun buildAuthenticatorMap(): MutableMap<ServerReference, TokenForServer> {


        return commonProperties.client.jwtAuthentications
            .flatMap { (authenticationServerURI, userName, password, targets) ->
                targets
                    .map { serverId ->
                        TokenForServer(
                            serverId,
                            authenticationServerURI,
                            userName,
                            password,
                            null)
                    }
            }
            .map { it.serverReference to it }
            .toMap()
            .toMutableMap()
    }

    /**
     * Get a valid authentication token to be add to the query
     *
     * @param currentState The current state of the authentication
     * @return the authentication token to be added to the header
     */
    private fun getValidJWTToken(currentState: TokenForServer): String {

        // Add 15 seconds to ensure that the query can be sent
        val now = Instant.now().plusSeconds(15).epochSecond
        // Make whatever date in the past so that now < past
        val past = Instant.now().minusSeconds(1).epochSecond

        return when {
            // If there is a token and it is not expired
            now < currentState.tokenResponse?.authenticationTokenExpiration ?: past ->
                currentState.tokenResponse!!.authenticationToken

            // If there is a token and its refresh is not expired
            now < currentState.tokenResponse?.refreshTokenExpiration ?: past        ->
                refreshAndGetToken(currentState)

            // Otherwise do full login
            else                                                                    ->
                loginAndGetToken(currentState)
        }
    }

    /**
     * Retrieve a full token from the authentication server
     *
     * @param currentState The current state of the authentication
     * @return the authentication token to be added to the header
     */
    private fun loginAndGetToken(currentState: TokenForServer): String {

        logger.debug("loginAndGetToken: request full login for user ${currentState.userName}")

        // Grab a new token
        val newToken = postForToken(
            UriComponentsBuilder
                .fromUri(currentState.authenticationServerURI)
                .pathSegment("login")
                .build()
                .toUri(),
            TokenRequest(
                currentState.userName,
                currentState.password))

        // Set the information in cache
        authenticationByServer[currentState.serverReference] = TokenForServer(
            currentState.serverReference,
            currentState.authenticationServerURI,
            currentState.userName,
            currentState.password,
            newToken)

        // Return the current token
        return newToken.authenticationToken
    }


    /**
     * Refresh a token from the authentication server. Note that it is mandatory to
     * call this function if the current state already holds a previous response
     *
     * @param currentState The current state of the authentication
     * @return the authentication token to be added to the header
     */
    private fun refreshAndGetToken(currentState: TokenForServer): String {

        logger.debug("refreshAndGetToken: request refresh token for user ${currentState.userName}")

        // Grab a new token
        val newToken = postForToken(
            UriComponentsBuilder
                .fromUri(currentState.authenticationServerURI)
                .pathSegment("refresh")
                .build()
                .toUri(),
            RefreshRequest(currentState.tokenResponse!!.refreshToken))

        // Set the information in cache
        authenticationByServer[currentState.serverReference] = TokenForServer(
            currentState.serverReference,
            currentState.authenticationServerURI,
            currentState.userName,
            currentState.password,
            newToken)

        // Return the current token
        return newToken.authenticationToken
    }

    /**
     * Make a post query to the authentication server. We can not use the JSON connector service
     * at this point because it would lead to circular dependencies. So use our local RestTemplate
     *
     * @param uri The URI of the endpoint for authentication
     * @param data The payload of the query
     * @return a TokenResponse
     */
    private fun <T> postForToken(uri: URI, data: T): TokenResponse {

        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(data, headers)

        return restTemplate
            .exchange(
                uri,
                HttpMethod.POST,
                entity,
                TokenResponse::class.java)
            .body
            ?: throw ClientException(E.security.authenticatorJWT.noResponse, uri)
    }

    /**
     * Internal classes for keeping cached information about authentication by server
     */
    private data class TokenForServer(
        val serverReference: ServerReference,
        val authenticationServerURI: URI,
        val userName: String,
        val password: String,
        val tokenResponse: TokenResponse?
    )

}

