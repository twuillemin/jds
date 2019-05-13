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
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.util.Assert
import org.springframework.web.client.RestTemplate
import java.net.URI


@ExtendWith(SpringExtension::class)
class ClientHttpRequestAuthenticatorNoAuthTest {

    @Test
    fun `NoAuth does not add an Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.noAuthentications).thenReturn(CommonProperties.NoAuthentication(listOf(ServerReference("localhost", 8080))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorNoAuth(properties)

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
            .andExpect { request ->
                val headerValues = request.headers[HTTP_HEADER_AUTHORIZATION]
                Assert.isNull(headerValues, "Authorization header is present")
            }
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
    fun `NoAuth overwrites an existing Authorization header`() {

        // Properties
        val properties = Mockito.mock(CommonProperties::class.java)
        val client = Mockito.mock(CommonProperties.Client::class.java)
        whenever(client.noAuthentications).thenReturn(CommonProperties.NoAuthentication(listOf(ServerReference("localhost", 8080))))
        whenever(properties.client).thenReturn(client)

        // Make the authenticator
        val authenticator = ClientHttpRequestAuthenticatorNoAuth(properties)

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
            .andExpect { request ->
                val headerValues = request.headers[HTTP_HEADER_AUTHORIZATION]
                Assert.isNull(headerValues, "Authorization header is present")
            }
            .andRespond(withSuccess("response", MediaType.TEXT_PLAIN))

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
        assertThat(response).isEqualTo("response")

        // Check the server
        mockServer.verify()
    }
}