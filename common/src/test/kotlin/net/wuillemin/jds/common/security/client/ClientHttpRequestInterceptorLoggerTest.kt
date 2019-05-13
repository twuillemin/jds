package net.wuillemin.jds.common.security.client

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import java.net.URI

@ExtendWith(SpringExtension::class)
class ClientHttpRequestInterceptorLoggerTest {

    @Test
    fun `Logging is applied to request`() {

        val interceptor = ClientHttpRequestInterceptorLogger(LoggerFactory.getLogger(ClientHttpRequestInterceptorLoggerTest::class.java))

        // Make the rest template
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(interceptor)

        // Mock the server
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://localhost:8080/test"))
            .andRespond(MockRestResponseCreators.withSuccess("response", MediaType.TEXT_PLAIN))

        // Do the query
        val responseJwt = restTemplate.getForEntity(URI.create("http://localhost:8080/test"), String::class.java).body

        // Check the response
        Assertions.assertThat(responseJwt).isNotNull()
        Assertions.assertThat(responseJwt).isEqualTo("response")

        // No test about what is actually logged because it change too frequently
    }
}
