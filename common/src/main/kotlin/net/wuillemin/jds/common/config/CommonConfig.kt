package net.wuillemin.jds.common.config

import net.wuillemin.jds.common.security.utils.CertificateFileReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder


/**
 * The configuration for the common services
 */
@Configuration
@ComponentScan(basePackages = [
    "net.wuillemin.jds.common.controller",
    "net.wuillemin.jds.common.security",
    "net.wuillemin.jds.common.service"])
@EnableConfigurationProperties(CommonSpringProperties::class)
class CommonConfig {

    /**
     * Create a new Bean being the logger to be used in the application. This logger
     * can then be injected in all services
     *
     * @param injectionPoint The injection point where to inject the logger
     * @return an instance of the logger
     */
    @Bean
    @Scope("prototype")
    fun logger(injectionPoint: InjectionPoint): Logger {
        return LoggerFactory.getLogger(injectionPoint.methodParameter!!.containingClass)
    }

    /**
     * Create a CertificateFileReader Bean as requested by spring to validate password. The password are hashed with
     * BCrypt
     *
     * @return an instance of the BCryptPasswordEncoder
     */
    @Bean
    fun pemFileReader(): CertificateFileReader {
        return CertificateFileReader()
    }

    /**
     * Create a PasswordEncoder Bean as requested by spring to validate password. The password are hashed with
     * BCrypt
     *
     * @return an instance of the BCryptPasswordEncoder
     */
    @Bean
    fun bCryptPasswordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * Expose a bean that allows each module to declare its own translation that could all be handled by the
     * LocalizationService
     *
     * @return a MessageSource bean
     */
    @Bean
    fun messageSource(): ResourceBundleMessageSource {
        val bundler = ResourceBundleMessageSource()
        bundler.addBasenames("messages/common_messages")
        return bundler
    }
}