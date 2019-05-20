package net.wuillemin.jds.common.security.client

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution

/**
 * The interface that all authenticators of the application must implement
 */
interface ClientHttpRequestAuthenticator {

    /**
     * Add the authentication in the given query
     * @param request the request, containing method, URI, and headers
     * @param body the body of the request
     * @param execution the request execution
     */
    fun addAuthentication(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    )
}