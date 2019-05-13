package net.wuillemin.jds.common.config

import net.wuillemin.jds.common.security.client.ClientHttpRequestInterceptorAuthentication
import net.wuillemin.jds.common.security.client.ClientHttpRequestInterceptorLogger
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn

/**
 * Configure the RestTemplateBuilder so that it will includes the various interceptor needed by the application.
 */
@Configuration
class RestTemplateBuilderConfig(
    private val clientHttpRequestInterceptorAuthentication: ClientHttpRequestInterceptorAuthentication,
    private val clientHttpRequestInterceptorLogger: ClientHttpRequestInterceptorLogger) {

    /**
     * Create a RestTemplateCustomizer bean that will be taken automatically for creating RestTemplate objects
     *
     * @return an instance of the RestTemplateCustomizer
     */
    @Bean
    fun customRestTemplateCustomizer(): RestTemplateCustomizer {
        return RestTemplateCustomizer { restTemplate ->
            restTemplate.interceptors.add(clientHttpRequestInterceptorAuthentication)
            restTemplate.interceptors.add(clientHttpRequestInterceptorLogger)
        }
    }

    /**
     * Create a new RestTemplateBuilder bean that could be used application wide. This RestTemplateBuilder
     * is configured with the authentication
     */
    @Bean
    @DependsOn(value = ["customRestTemplateCustomizer"])
    fun restTemplateBuilder(): RestTemplateBuilder {
        return RestTemplateBuilder(customRestTemplateCustomizer())
    }
}