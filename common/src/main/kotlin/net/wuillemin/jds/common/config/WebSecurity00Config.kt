package net.wuillemin.jds.common.config

import net.wuillemin.jds.common.security.server.HttpJWTConfigurer
import net.wuillemin.jds.common.service.LocalisationService
import org.slf4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

/**
 * Configure the security of the application
 */
@Configuration
@EnableWebSecurity
@Order(0)
class WebSecurity00Config(
    private val logger: Logger,
    private val localisationService: LocalisationService,
    private val commonProperties: CommonProperties) : WebSecurityConfigurerAdapter() {

    /**
     * Make a configure not pointing at nothing existing so that
     * there is no default configuration
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {

        http.antMatcher("/api/**")
            .cors()
            .and()
            .csrf().disable()
            .authorizeRequests().anyRequest().authenticated()
            .and()
            .apply(HttpJWTConfigurer(localisationService, commonProperties.server.publicKey))
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        logger.info("configure: /api/** => JWT")
    }
}