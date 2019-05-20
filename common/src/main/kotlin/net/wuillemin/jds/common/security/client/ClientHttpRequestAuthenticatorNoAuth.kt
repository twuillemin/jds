package net.wuillemin.jds.common.security.client

import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.ServerReference
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.stereotype.Service

/**
 * Service for authenticating against server needing no authentication
 *
 * @param commonProperties The properties of the application
 */
@Service
class ClientHttpRequestAuthenticatorNoAuth(
    private val commonProperties: CommonProperties
) : ClientHttpRequestAuthenticator {

    private val authenticationByServer = buildAuthenticatorMap()

    override fun addAuthentication(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ) {

        // Get the id of the server
        val serverId = request.uri.let { ServerReference(it.host, it.port) }

        // If the server is in the no auth list
        if (authenticationByServer.contains(serverId)) {
            // Ensure header is absent
            request.headers.remove(HTTP_HEADER_AUTHORIZATION)
        }
    }

    /**
     * Construct the map referencing all authentication method by their server
     */
    private fun buildAuthenticatorMap(): Set<ServerReference> {

        // Build all definition
        return commonProperties.client.noAuthentications.targets.toHashSet()
    }
}
