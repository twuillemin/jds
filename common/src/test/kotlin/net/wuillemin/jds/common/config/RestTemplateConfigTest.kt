package net.wuillemin.jds.common.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/**
 * Configure the RestTemplate so that it will includes the various interceptor needed by the application.
 */
@Configuration
class RestTemplateConfigTest {

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplateBuilder = RestTemplateBuilder()
        return restTemplateBuilder.build()
    }
}