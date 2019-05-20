package net.wuillemin.jds.common.security.client

import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.ServerReference
import org.slf4j.Logger
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Service for intercepting output query and add them the needed authentication information
 *
 * @param commonProperties The properties of the application
 * @param networkAuthenticatorJwt Service for authenticating against server needing a JWT authentication
 * @param networkAuthenticatorBasic Service for authenticating against server needing a Basic authentication
 * @param networkAuthenticatorNoAuth Service for authenticating against server needing no authentication
 * @param logger The logger
 */
@Service
class ClientHttpRequestInterceptorAuthentication(
    private val commonProperties: CommonProperties,
    private val networkAuthenticatorJwt: ClientHttpRequestAuthenticatorJwt,
    private val networkAuthenticatorBasic: ClientHttpRequestAuthenticatorBasic,
    private val networkAuthenticatorNoAuth: ClientHttpRequestAuthenticatorNoAuth,
    private val logger: Logger
) : ClientHttpRequestInterceptor {

    private val authenticationByServer = buildAuthenticatorMap()

    /**
     * Intercept a request and add the authentication
     *
     * @param request the request, containing method, URI, and headers
     * @param body the body of the request
     * @param execution the request execution
     * @return the response
     * @throws IOException in case of I/O errors
     */
    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        // Check if there is not already an authentication
        if (request.headers[HTTP_HEADER_AUTHORIZATION] == null) {

            // Get the schemaId
            val serverId = request.uri.let { ServerReference(it.host, it.port) }

            // Apply the authenticator or warn
            authenticationByServer[serverId]
                ?.addAuthentication(request, body, execution)
                ?: logger.warn("A request was made to URI ${request.uri} without authentication defined. No authentication used.")
        }

        // Finish the execution of the query
        return execution.execute(request, body)
    }

    /**
     * Construct the map referencing all authentication method by their server
     */
    private fun buildAuthenticatorMap(): Map<ServerReference, ClientHttpRequestAuthenticator> {

        val authenticators = HashMap<ServerReference, ClientHttpRequestAuthenticator>()

        authenticators.putAll(
            commonProperties.client.jwtAuthentications.flatMap { property -> property.targets.map { it to networkAuthenticatorJwt } })
        authenticators.putAll(
            commonProperties.client.basicAuthentications.flatMap { property -> property.targets.map { it to networkAuthenticatorBasic } })
        authenticators.putAll(
            commonProperties.client.noAuthentications.targets.map { it to networkAuthenticatorNoAuth })

        return authenticators.toMap()
    }
}