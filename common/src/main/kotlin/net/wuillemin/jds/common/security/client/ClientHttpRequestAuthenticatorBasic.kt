package net.wuillemin.jds.common.security.client

import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_BASIC_PREFIX
import net.wuillemin.jds.common.security.ServerReference
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for authenticating against server needing a basic authentication
 *
 * @param commonProperties The properties of the application
 */
@Service
class ClientHttpRequestAuthenticatorBasic(
    private val commonProperties: CommonProperties) : ClientHttpRequestAuthenticator {

    private val authenticationByServer = buildAuthenticatorMap()

    override fun addAuthentication(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution) {

        // Get the id of the server
        val serverId = request.uri.let { ServerReference(it.host, it.port) }

        // Get the authentication if any
        val existingAuthentication = authenticationByServer[serverId]

        // Apply the authentication if any available
        existingAuthentication?.let { request.headers[HTTP_HEADER_AUTHORIZATION] = it.authentication }
    }

    /**
     * Construct the map referencing all authentication method by their server
     */
    private fun buildAuthenticatorMap(): Map<ServerReference, AuthenticationForServer> {

        // The function for getting an authentication from a definition
        val authenticationStringBuilder: (CommonProperties.BasicAuthentication) -> String = {
            val authentication = "${it.userName}:${it.password}"
            val encodedAuthentication = Base64.getEncoder().encodeToString(authentication.toByteArray())
            HTTP_HEADER_BASIC_PREFIX + encodedAuthentication
        }

        // Build all definitions
        return commonProperties.client.basicAuthentications
            .flatMap { authentication ->
                val authenticationString = authenticationStringBuilder(authentication)
                authentication.targets.map { serverId ->
                    AuthenticationForServer(
                        serverId,
                        authenticationString
                    )
                }
            }
            .map { it.serverReference to it }
            .toMap()
    }

    /**
     * Internal classes for keeping cached information about authentication by server
     */
    private data class AuthenticationForServer(
        val serverReference: ServerReference,
        val authentication: String
    )
}