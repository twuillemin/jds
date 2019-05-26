package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.config.CommonConfigTest
import net.wuillemin.jds.common.config.RestTemplateConfigTest
import net.wuillemin.jds.common.exception.ClientException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    CommonConfigTest::class,
    JsonConnector::class,
    RestTemplateConfigTest::class])
class JsonConnectorTest {

    companion object {

        private val customizer: MockServerRestTemplateCustomizer = MockServerRestTemplateCustomizer()

        @TestConfiguration
        internal class RestTemplateBuilderProvider {
            @Bean
            fun provideBuilder(): RestTemplateBuilder {
                return RestTemplateBuilder(customizer)
            }
        }
    }

    @Autowired
    private lateinit var jsonConnector: JsonConnector

    @Autowired
    private val restTemplate: RestTemplate? = null

    @Test
    fun `Get single object to ParameterizedTypeReference`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andRespond(withSuccess("{\"a\":\"A\"}", MediaType.APPLICATION_JSON))

        }
        val read = jsonConnector.getSingleObjectFromJSON(URI.create("/testEndPoint"), object : ParameterizedTypeReference<Map<String, String>>() {

        })

        Assertions.assertEquals("A", read["a"])

        mockServer.verify()
    }

    @Test
    fun `Get single object to class`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andRespond(withSuccess("TheString", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.getSingleObjectFromJSON(URI.create("/testEndPoint"), String::class.java)

        Assertions.assertEquals("TheString", read)

        mockServer.verify();
    }

    @Test
    fun `Get multiple objects to class`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andRespond(withSuccess("[\"TheString1\",\"TheString2\"]", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.getMultipleObjectsFromJSON(URI.create("/testEndPoint"), String::class.java)

        Assertions.assertEquals(2, read.size)
        Assertions.assertEquals("TheString1", read[0])
        Assertions.assertEquals("TheString2", read[1])

        mockServer.verify();
    }

    @Test
    fun `Put object to class`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.PUT)))
                .andExpect { content().contentType(MediaType.APPLICATION_JSON) }
                .andExpect { content().json("{\"a\":\"A\"}") }
                .andRespond(withSuccess("TheString", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.putJson(URI.create("/testEndPoint"), mapOf("a" to "A"), String::class.java)

        Assertions.assertEquals("TheString", read)

        mockServer.verify();
    }

    @Test
    fun `Put object to ParameterizedTypeReference`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.PUT)))
                .andExpect { content().contentType(MediaType.APPLICATION_JSON) }
                .andExpect { content().json("{\"a\":\"A\"}") }
                .andRespond(withSuccess("{\"b\":\"B\"}", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.putJson(URI.create("/testEndPoint"), mapOf("a" to "A"), object : ParameterizedTypeReference<Map<String, String>>() {

        })

        Assertions.assertNotNull(read)
        Assertions.assertEquals(mapOf("b" to "B"), read)

        mockServer.verify();
    }

    @Test
    fun `Post object to class`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.POST)))
                .andExpect { content().contentType(MediaType.APPLICATION_JSON) }
                .andExpect { content().json("{\"a\":\"A\"}") }
                .andRespond(withSuccess("TheString", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.postJson(URI.create("/testEndPoint"), mapOf("a" to "A"), String::class.java)

        Assertions.assertEquals("TheString", read)

        mockServer.verify();
    }

    @Test
    fun `Post object to ParameterizedTypeReference`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.POST)))
                .andExpect { content().contentType(MediaType.APPLICATION_JSON) }
                .andExpect { content().json("{\"a\":\"A\"}") }
                .andRespond(withSuccess("{\"b\":\"B\"}", MediaType.APPLICATION_JSON))
        }

        val read = jsonConnector.postJson(URI.create("/testEndPoint"), mapOf("a" to "A"), object : ParameterizedTypeReference<Map<String, String>>() {

        })

        Assertions.assertNotNull(read)
        Assertions.assertEquals(mapOf("b" to "B"), read)

        mockServer.verify();
    }

    @Test
    fun `Delete at URI`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.DELETE)))
                .andRespond(withSuccess())
        }

        jsonConnector.delete(URI.create("/testEndPoint"))

        mockServer.verify();
    }

    @Test
    fun `Delete returning an error`() {
        val mockServer = MockRestServiceServer.createServer(restTemplate!!).apply {
            expect(requestTo("/testEndPoint"))
                .andExpect(method((HttpMethod.DELETE)))
                .andRespond(withServerError())
        }

        Assertions.assertThrows(ClientException::class.java) { jsonConnector.delete(URI.create("/testEndPoint")) }

        mockServer.verify();
    }
}
