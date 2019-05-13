package net.wuillemin.jds.dataserver.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

/**
 * The configuration for the common part of
 */
@Configuration
@ComponentScan(basePackages = [
    "net.wuillemin.jds.dataserver.controller",
    "net.wuillemin.jds.dataserver.event",
    "net.wuillemin.jds.dataserver.listener",
    "net.wuillemin.jds.dataserver.service",
    "net.wuillemin.jds.dataserver.supplier"])
@EnableConfigurationProperties(DataServerSpringProperties::class)
class DataServerConfig(
    resourceBundleMessageSource: ResourceBundleMessageSource) {

    init {
        resourceBundleMessageSource.addBasenames("messages/dataserver_messages")
    }
}