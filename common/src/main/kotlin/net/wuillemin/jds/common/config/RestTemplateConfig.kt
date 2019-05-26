package net.wuillemin.jds.common.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate


/**
 * Configure the RestTemplateBuilder so that it will includes the various interceptor needed by the application.
 */
@Configuration
class RestTemplateConfig(
    private val restTemplateBuilder: RestTemplateBuilder
) {

    /**
     * restTemplate create a new RestTemplate bean that can be used in the whole application. The RestTemplate bean
     * will follow the authentication rules given in the configuration.
     */
    @Bean
    fun restTemplate(): RestTemplate {
        return restTemplateBuilder.build()
    }
}