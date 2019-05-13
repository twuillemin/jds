package net.wuillemin.jds.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport

/**
 * Configuration of the basic Spring WebApplication
 */
@Configuration
class WebAppConfig : WebMvcConfigurationSupport() {

    /**
     * Add resource handlers for serving static resources.
     * @param registry The registry of resource handlers for serving static resources
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/")
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/")
    }
}