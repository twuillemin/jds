package net.wuillemin.jds.authserver.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

/**
 * Configuration of the Authentication components
 */
@Configuration
@ComponentScan(basePackages = [
    "net.wuillemin.jds.authserver.controller",
    "net.wuillemin.jds.authserver.listener",
    "net.wuillemin.jds.authserver.service"])
@EnableConfigurationProperties(AuthServerSpringProperties::class)
class AuthServerConfig(
    resourceBundleMessageSource: ResourceBundleMessageSource) {

    init {
        resourceBundleMessageSource.addBasenames("messages/authserver_messages")
    }
}