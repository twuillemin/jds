package net.wuillemin.jds.common.security.client

import com.nhaarman.mockitokotlin2.any
import net.wuillemin.jds.common.config.CommonProperties
import net.wuillemin.jds.common.config.CommonProperties.BasicAuthentication
import net.wuillemin.jds.common.config.CommonProperties.Client
import net.wuillemin.jds.common.config.CommonProperties.JWTAuthentication
import net.wuillemin.jds.common.config.CommonProperties.NoAuthentication
import net.wuillemin.jds.common.config.CommonProperties.Server
import net.wuillemin.jds.common.security.JWTConstant.Companion.HTTP_HEADER_AUTHORIZATION
import net.wuillemin.jds.common.security.ServerReference
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import java.net.URI

@ExtendWith(SpringExtension::class)
class ClientHttpRequestInterceptorAuthenticationTest {

    @Test
    fun `Authentication is applied to request`() {

        // Mock the authenticator
        val authenticatorJwt = Mockito.mock(ClientHttpRequestAuthenticatorJwt::class.java)
        val authenticatorBasic = Mockito.mock(ClientHttpRequestAuthenticatorBasic::class.java)
        val authenticatorNo = Mockito.mock(ClientHttpRequestAuthenticatorNoAuth::class.java)

        val interceptor = ClientHttpRequestInterceptorAuthentication(
            CommonProperties(
                MongoProperties(),
                Server(null),
                Client(
                    listOf(JWTAuthentication(URI.create("auth"), "user", "password", listOf(ServerReference("localhost", 8080)))),
                    listOf(BasicAuthentication("user", "password", listOf(ServerReference("localhost", 8081)))),
                    NoAuthentication(listOf(ServerReference("localhost", 8082))))),
            authenticatorJwt,
            authenticatorBasic,
            authenticatorNo,
            LoggerFactory.getLogger(ClientHttpRequestInterceptorAuthenticationTest::class.java))

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        //
        // Prepare the calls to the server
        //

        // JWT
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8080/test"))
            .andRespond(MockRestResponseCreators.withSuccess("response0", MediaType.TEXT_PLAIN))

        // Basic
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8081/test"))
            .andRespond(MockRestResponseCreators.withSuccess("response1", MediaType.TEXT_PLAIN))

        // NoAuth
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8082/test"))
            .andRespond(MockRestResponseCreators.withSuccess("response2", MediaType.TEXT_PLAIN))

        // Undefined
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8083/test"))
            .andRespond(MockRestResponseCreators.withSuccess("response3", MediaType.TEXT_PLAIN))

        //
        // JWT
        //

        // Do the query
        val responseJwt = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body

        // Check the response
        Assertions.assertThat(responseJwt).isNotNull()
        Assertions.assertThat(responseJwt).isEqualTo("response0")

        //
        // Basic
        //

        // Do the query
        val responseBasic = restTemplate.getForEntity(URI.create("http://localhost:8081/test"), String::class.java).body

        // Check the response
        Assertions.assertThat(responseBasic).isNotNull()
        Assertions.assertThat(responseBasic).isEqualTo("response1")

        //
        // No Auth
        //

        // Do the query
        val responseNo = restTemplate.getForEntity(URI.create("http://localhost:8082/test"), String::class.java).body

        // Check the response
        Assertions.assertThat(responseNo).isNotNull()
        Assertions.assertThat(responseNo).isEqualTo("response2")

        //
        // Undefined
        //

        // Do the query
        val responseUndefined = restTemplate.getForEntity(URI.create("http://localhost:8083/test"), String::class.java).body

        // Check the response
        Assertions.assertThat(responseUndefined).isNotNull()
        Assertions.assertThat(responseUndefined).isEqualTo("response3")

        // Check the server
        mockServer.verify()
        verify(authenticatorJwt, times(1)).addAuthentication(any(), any(), any())
        verify(authenticatorBasic, times(1)).addAuthentication(any(), any(), any())
        verify(authenticatorNo, times(1)).addAuthentication(any(), any(), any())
    }

    @Test
    fun `Interceptor does not modify previous Authentication`() {

        // Mock the authenticator
        val authenticatorJwt = Mockito.mock(ClientHttpRequestAuthenticatorJwt::class.java)
        val authenticatorBasic = Mockito.mock(ClientHttpRequestAuthenticatorBasic::class.java)
        val authenticatorNo = Mockito.mock(ClientHttpRequestAuthenticatorNoAuth::class.java)

        val interceptor = ClientHttpRequestInterceptorAuthentication(
            CommonProperties(
                MongoProperties(),
                Server(null),
                Client(
                    listOf(JWTAuthentication(URI.create("auth"), "user", "password", listOf(ServerReference("localhost", 8080)))),
                    listOf(BasicAuthentication("user", "password", listOf(ServerReference("localhost", 8081)))),
                    NoAuthentication(listOf(ServerReference("localhost", 8082))))),
            authenticatorJwt,
            authenticatorBasic,
            authenticatorNo,
            LoggerFactory.getLogger(ClientHttpRequestInterceptorAuthenticationTest::class.java))

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()

        // Prepare the calls to the server
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8080/test"))
            .andExpect(MockRestRequestMatchers.header(HTTP_HEADER_AUTHORIZATION, "Basic dXNlck5hbWU6cGFzc3cwcmQ="))
            .andRespond(MockRestResponseCreators.withSuccess("response", MediaType.TEXT_PLAIN))

        // Prepare the query that wil generate "Basic dXNlck5hbWU6cGFzc3cwcmQ="
        val headers = HttpHeaders()
        headers.setBasicAuth("userName", "passw0rd")
        val httpEntity = HttpEntity<Void>(headers)

        // Do the query
        val response = restTemplate
            .exchange(
                URI.create("http://localhost:8080/test"),
                HttpMethod.GET,
                httpEntity,
                String::class.java)
            .body

        // Check the response
        Assertions.assertThat(response).isNotNull()
        Assertions.assertThat(response).isEqualTo("response")

        // Check the server
        mockServer.verify()
        // Check no call made
        verify(authenticatorBasic, times(0)).addAuthentication(any(), any(), any())
    }
}