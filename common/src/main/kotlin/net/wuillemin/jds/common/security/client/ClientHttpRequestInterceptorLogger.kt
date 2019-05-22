package net.wuillemin.jds.common.security.client

import org.slf4j.Logger
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Service for intercepting output query and log them
 *
 * @param logger The logger
 */
@Service
class ClientHttpRequestInterceptorLogger(private val logger: Logger) : ClientHttpRequestInterceptor {

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

        logRequestDetails(request)
        return execution.execute(request, body)
    }

    private fun logRequestDetails(request: HttpRequest) {
        logger.debug("Headers: {}", request.headers)
        logger.debug("Request Method: {}", request.method)
        logger.debug("Request URI: {}", request.uri)
    }
}