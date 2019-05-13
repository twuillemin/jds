package net.wuillemin.jds.common.security.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.dto.TokenResponse
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_BEARER_PREFIX
import net.wuillemin.jds.common.security.ServerReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Instant


@ExtendWith(SpringExtension::class)
class ClientHttpRequestAuthenticatorJwtTest {

    private val objectMapper = ObjectMapper()

    private val logger = LoggerFactory.getLogger(ClientHttpRequestAuthenticatorJwtTest::class.java)

    @Test
    fun `JWT Authenticator adds an Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.jwtAuthentications).thenReturn(listOf(CommonProperties.JWTAuthentication(URI.create("http://authserver"), "user", "password", listOf(ServerReference("localhost", 8080)))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorJwt(properties, logger)

        // Make the authenticator
        val interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            authenticator.addAuthentication(request, body, execution)
            execution.execute(request, body)
        }

        // Make the rest template as a client
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the external server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        // Mock the authentication server
        val mockAuthServer = MockRestServiceServer.bindTo(stealRestTemplateFromAuthenticator(authenticator)).build()

        // First query is for retrieving the token (make an expired token, so next call will be refresh)
        mockAuthServer.expect(once(), requestTo("http://authserver/login"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue1",
                            Instant.now().minusSeconds(1).epochSecond,
                            "refreshTokenValue1",
                            Instant.now().plusSeconds(6000).epochSecond)),
                    MediaType.APPLICATION_JSON))


        // Then we retrieve the data
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue1"))
            .andRespond(withSuccess("response1", MediaType.TEXT_PLAIN))

        // Second query is for retrieving the expired token (make an expired token and refresh, so next call will be full login)
        mockAuthServer.expect(once(), requestTo("http://authserver/refresh"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue2",
                            Instant.now().minusSeconds(1).epochSecond,
                            "refreshTokenValue2",
                            Instant.now().minusSeconds(1).epochSecond)),
                    MediaType.APPLICATION_JSON))

        // Then we retrieve the data
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue2"))
            .andRespond(withSuccess("response2", MediaType.TEXT_PLAIN))

        // Third query is for retrieving a valid token
        mockAuthServer.expect(once(), requestTo("http://authserver/login"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue3",
                            Instant.now().plusSeconds(600).epochSecond,
                            "refreshTokenValue3",
                            Instant.now().plusSeconds(6000).epochSecond)),
                    MediaType.APPLICATION_JSON))

        // Then we retrieve data
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue3"))
            .andRespond(withSuccess("response3", MediaType.TEXT_PLAIN))

        // Fourth query will send another time the valid token
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue3"))
            .andRespond(withSuccess("response4", MediaType.TEXT_PLAIN))

        // Do the query 1
        val response1 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response1).isNotNull()
        assertThat(response1).isEqualTo("response1")

        // Do the query 2
        val response2 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response2).isNotNull()
        assertThat(response2).isEqualTo("response2")

        // Do the query 3
        val response3 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response3).isNotNull()
        assertThat(response3).isEqualTo("response3")

        // Do the query 4
        val response4 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response4).isNotNull()
        assertThat(response4).isEqualTo("response4")

        // Check the server
        mockServer.verify()
    }

    @Test
    fun `JWT Authenticator overwrites an existing Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.jwtAuthentications).thenReturn(listOf(CommonProperties.JWTAuthentication(URI.create("http://authserver"), "user", "password", listOf(ServerReference("localhost", 8080)))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorJwt(properties, logger)

        // Make the authenticator
        val interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            authenticator.addAuthentication(request, body, execution)
            execution.execute(request, body)
        }

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the target server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        // Mock the authentication server
        val mockAuthServer = MockRestServiceServer.bindTo(stealRestTemplateFromAuthenticator(authenticator)).build()

        // First query is for retrieving the token
        mockAuthServer.expect(once(), requestTo("http://authserver/login"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue1",
                            Instant.now().plusSeconds(60).epochSecond,
                            "refreshTokenValue1",
                            Instant.now().plusSeconds(6000).epochSecond)),
                    MediaType.APPLICATION_JSON))

        // Then the query to the client
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue1"))
            .andRespond(withSuccess("response1", MediaType.TEXT_PLAIN))

        // Prepare the query
        val headers = HttpHeaders()
        headers.setBasicAuth("user", "password")
        val httpEntity = HttpEntity<Void>(headers)

        val response = restTemplate
            .exchange(
                URI.create("http://localhost:8080/test"),
                GET,
                httpEntity,
                String::class.java)
            .body

        assertThat(response).isNotNull()
        assertThat(response).isEqualTo("response1")

        // Check the server
        mockServer.verify()
    }

    @Test
    fun `JWT Authenticator fails if no response is given by the server`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.jwtAuthentications).thenReturn(listOf(CommonProperties.JWTAuthentication(URI.create("http://authserver"), "user", "password", listOf(ServerReference("localhost", 8080)))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorJwt(properties, logger)

        // Make the authenticator
        val interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            authenticator.addAuthentication(request, body, execution)
            execution.execute(request, body)
        }

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        // Mock the authentication server
        val mockAuthServer = MockRestServiceServer.bindTo(stealRestTemplateFromAuthenticator(authenticator)).build()

        // First query will not be received (as the token retrieving should fail)
        mockAuthServer.expect(once(), requestTo("http://authserver/login"))
            .andExpect(method(POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        // Second query will be received (but token is expired)
        mockAuthServer.expect(once(), requestTo("http://authserver/login"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue1",
                            Instant.now().minusSeconds(60).epochSecond,
                            "refreshTokenValue1",
                            Instant.now().plusSeconds(6000).epochSecond)),
                    MediaType.APPLICATION_JSON))

        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue1"))
            .andRespond(withSuccess("response1", MediaType.TEXT_PLAIN))

        // Third query will not be received (as the token refresh should fail)
        mockAuthServer.expect(once(), requestTo("http://authserver/refresh"))
            .andExpect(method(POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))


        // Fourth query will be received
        mockAuthServer.expect(once(), requestTo("http://authserver/refresh"))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(
                        TokenResponse(
                            "authTokenValue2",
                            Instant.now().plusSeconds(60).epochSecond,
                            "refreshTokenValue2",
                            Instant.now().plusSeconds(6000).epochSecond)),
                    MediaType.APPLICATION_JSON))

        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_BEARER_PREFIX + "authTokenValue2"))
            .andRespond(withSuccess("response2", MediaType.TEXT_PLAIN))

        // Do the query 1 (that will fail)
        Assertions.assertThrows(Exception::class.java) { restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java) }

        // Do the query 2 (that will have an expired token)
        val response1 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response1).isNotNull()
        assertThat(response1).isEqualTo("response1")

        // Do the query 3 (that will fail)
        Assertions.assertThrows(Exception::class.java) { restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java) }

        // Do the query 4 (that will have a good token)
        val response2 = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body
        assertThat(response2).isNotNull()
        assertThat(response2).isEqualTo("response2")

        // Check the server
        mockServer.verify()
    }

    private fun stealRestTemplateFromAuthenticator(authenticator: ClientHttpRequestAuthenticatorJwt): RestTemplate {
        val field = ClientHttpRequestAuthenticatorJwt::class.java.getDeclaredField("restTemplate")
        field.isAccessible = true
        val raw = field.get(authenticator)
        return raw as RestTemplate
    }
}