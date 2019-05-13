package net.wuillemin.jds.dataserver.app.monolith.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * General configuration of the application
 */
@Configuration
@ComponentScan(basePackages = [
    "net.wuillemin.jds.common.config",
    "net.wuillemin.jds.authserver.config",
    "net.wuillemin.jds.dataserver.config"])
class MonolithApplicationConfig