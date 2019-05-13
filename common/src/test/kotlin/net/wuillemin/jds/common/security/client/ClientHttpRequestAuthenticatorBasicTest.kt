package net.wuillemin.jds.common.security.client

import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.ServerReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
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


@ExtendWith(SpringExtension::class)
class ClientHttpRequestAuthenticatorBasicTest {

    @Test
    fun `Basic Authenticator adds an Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.basicAuthentications).thenReturn(listOf(CommonProperties.BasicAuthentication("user", "password", listOf(ServerReference("localhost", 8080)))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorBasic(properties)

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

        // Prepare the server
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA=="))
            .andRespond(withSuccess("response", MediaType.TEXT_PLAIN))

        // Do the query
        val response = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body

        // Check the response
        assertThat(response).isNotNull()
        assertThat(response).isEqualTo("response")

        // Check the server
        mockServer.verify()
    }

    @Test
    fun `Basic Authenticator overwrites an existing Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.basicAuthentications).thenReturn(listOf(CommonProperties.BasicAuthentication("user", "password", listOf(ServerReference("localhost", 8080)))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorBasic(properties)
        val interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            authenticator.addAuthentication(request, body, execution)
            execution.execute(request, body)
        }

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        // Prepare the server
        mockServer.expect(once(), requestTo("http://localhost:8080/test"))
            .andExpect(method(GET))
            .andExpect(header(HTTP_HEADER_AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA=="))
            .andRespond(withSuccess("response", MediaType.TEXT_PLAIN))

        // Prepare the query, that will generate "Basic dXNlck5hbWU6cGFzc3cwcmQ="
        val headers = HttpHeaders()
        headers.setBasicAuth("userName", "passw0rd")
        val httpEntity = HttpEntity<Void>(headers)

        val response = restTemplate
            .exchange(
                URI.create("http://localhost:8080/test"),
                GET,
                httpEntity,
                String::class.java)
            .body

        assertThat(response).isNotNull()
        assertThat(response).isEqualTo("response")

        // Check the server
        mockServer.verify()
    }
}