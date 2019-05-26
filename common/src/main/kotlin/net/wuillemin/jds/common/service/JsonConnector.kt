package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.exception.ClientException
import net.wuillemin.jds.common.exception.E
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * A service for executing HTTP query sending and receiving JSON data
 *
 * @param restTemplate The rest template
 */
@Service
class JsonConnector(private val restTemplate: RestTemplate) {

    /**
     * Simply retrieve an object from a JSON server
     *
     * @param uri            The uri for getting the object
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object requested
     * @return The object or null
     */
    fun <T> getSingleObjectFromJSON(uri: URI, clazz: Class<T>): T {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.GET,
                    acceptJsonHeader(),
                    clazz)
                .body
                ?: throw ClientException(E.service.jsonConnector.emptyResponse, "GET", uri)
        }
        catch (exception: Exception) {
            throw logAndRethrow("GET", uri, clazz.simpleName, exception)
        }
    }


    /**
     * Simply retrieve an object from a JSON server
     *
     * @param uri            The uri for getting the object
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object requested
     * @return The object or null
     */
    fun <T> getSingleObjectFromJSON(uri: URI, clazz: ParameterizedTypeReference<T>): T {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.GET,
                    acceptJsonHeader(),
                    clazz)
                .body
                ?: throw ClientException(E.service.jsonConnector.emptyResponse, "GET", uri)
        }
        catch (exception: Exception) {
            throw logAndRethrow("GET", uri, clazz.type.typeName, exception)
        }
    }


    /**
     * Simply retrieve a list of object from a JSON server
     *
     * @param uri            The uri for getting the object
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object requested
     * @return The object or null
     */
    fun <T> getMultipleObjectsFromJSON(uri: URI, clazz: Class<T>): List<T> {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.GET,
                    acceptJsonHeader(),
                    typeRef<List<T>>())
                .body
                ?: throw ClientException(E.service.jsonConnector.emptyResponse, "GET", uri)

        }
        catch (exception: Exception) {
            throw logAndRethrow("GET", uri, clazz.simpleName, exception)
        }
    }

    /**
     * Put a single object. An object of the different or same type is expected to be returned
     *
     * @param uri            The uri for getting the object
     * @param objectToSend   The object to be sent on request
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object sent
     * @param <S>            The class of the object returned
     * @return The response object or null
     */
    fun <T, S> putJson(uri: URI, objectToSend: T, clazz: Class<S>): S? {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.PUT,
                    acceptAndSendJsonHeader(objectToSend),
                    clazz)
                .body
        }
        catch (exception: Exception) {
            throw logAndRethrow("PUT", uri, clazz.simpleName, exception)
        }
    }

    /**
     * Put an object. An object of the different or same type is expected to be returned
     *
     * @param uri            The uri for getting the object
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object requested
     * @return The object or null
     */
    fun <T, S> putJson(uri: URI, objectToSend: T, clazz: ParameterizedTypeReference<S>): S? {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.PUT,
                    acceptAndSendJsonHeader(objectToSend),
                    clazz)
                .body
        }
        catch (exception: Exception) {
            throw logAndRethrow("PUT", uri, clazz.type.typeName, exception)
        }
    }

    /**
     * Post a single object. An object of the different or same type is expected to be returned
     *
     * @param uri            The uri for getting the object
     * @param objectToSend   The object to be sent on request
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object sent
     * @param <S>            The class of the object requested
     * @return The response object or null
     */
    fun <T, S> postJson(uri: URI, objectToSend: T, clazz: Class<S>): S? {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.POST,
                    acceptAndSendJsonHeader(objectToSend),
                    clazz)
                .body
        }
        catch (exception: Exception) {
            throw logAndRethrow("POST", uri, clazz.simpleName, exception)
        }
    }

    /**
     * Post a single object. An object of the different or same type is expected to be returned
     *
     * @param uri            The uri for getting the object
     * @param objectToSend   The object to be sent on request
     * @param clazz          The class of the object requested
     * @param <T>            The class of the object sent
     * @param <S>            The class of the object requested
     * @return The response object or null
     */
    fun <T, S> postJson(uri: URI, objectToSend: T, clazz: ParameterizedTypeReference<S>): S? {

        return try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.POST,
                    acceptAndSendJsonHeader(objectToSend),
                    clazz)
                .body
        }
        catch (exception: Exception) {
            throw logAndRethrow("POST", uri, clazz.type.typeName, exception)
        }
    }

    /**
     * Delete an object by its URI
     *
     * @param uri The uri for getting the object
     */
    fun delete(uri: URI) {

        try {
            restTemplate
                .exchange(
                    uri,
                    HttpMethod.DELETE,
                    HttpEntity<Void>(HttpHeaders()),
                    Void::class.java)
        }
        catch (exception: HttpClientErrorException) {
            throw ClientException(E.service.jsonConnector.deleteHttpException, uri, exception.responseBodyAsString)
        }
        catch (exception: RestClientException) {
            throw ClientException(E.service.jsonConnector.deleteRestException, uri, exception.message)
        }
        catch (exception: Exception) {
            throw ClientException(E.service.jsonConnector.deleteOtherException, uri, exception.message)
        }
    }

    /**
     * Build an HTTP header object, indicating that the query will accept JSON object
     *
     * @return an HTTP header
     */
    private fun acceptJsonHeader(): HttpEntity<Void> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        return HttpEntity(headers)
    }

    /**
     * Build an HTTP header object, indicating that the query will send JSON data and accept a JSON object back
     *
     * @param objectToSend The object to be sent
     * @return an HTTP header
     * <T> The class of the object to send
     */
    private fun <T> acceptAndSendJsonHeader(objectToSend: T): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(objectToSend, headers)
    }

    /**
     * Properly log the given exception and simply rethrow it
     * @param httpMethod The http method used
     * @param uri The uri called
     * @param className The result class expected
     * @param exception The exception thrown
     * @return [never reached]
     */
    private fun logAndRethrow(httpMethod: String, uri: URI, className: String, exception: Exception): Exception {
        when (exception) {
            is HttpClientErrorException -> {
                throw ClientException(E.service.jsonConnector.retrieveHttpException, className, httpMethod, uri, exception.responseBodyAsString)
            }
            is RestClientException      -> {
                throw ClientException(E.service.jsonConnector.retrieveRestException, className, httpMethod, uri, exception.message)
            }
            else                        -> {
                throw ClientException(E.service.jsonConnector.retrieveOtherException, className, httpMethod, uri, exception.message)
            }
        }
    }
}

/**
 * The typeRef function is returning the ParameterizedTypeReference of the expected object. It is often not even
 * necessary to give the Generic as it is inferred from the calling part. Instead of sending the class expected
 * to be returned, using typeRef() is a safer option
 */
inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
