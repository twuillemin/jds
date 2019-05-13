package net.wuillemin.jds.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

/**
 * Configuration of the ObjectMapper to be used inside the application
 */
@Configuration
class ObjectMapperConfig {

    /**
     * Create a new ObjectMapper, that could be injected in the various services
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}