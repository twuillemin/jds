package net.wuillemin.jds.authserver.config

import org.slf4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

/**
 * Configure the security of the application
 */
@Configuration
@Order(6)
class WebSecurity06Config(
    private val logger: Logger) : WebSecurityConfigurerAdapter() {

    /**
     * Override this method to configure the {@link HttpSecurity}. Typically subclasses
     * should not invoke this method by calling super as it may override their
     * configuration.
     *
     * @param http the {@link HttpSecurity} to modify
     * @throws Exception if an error occurs
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {

        http.antMatcher("/login/oauth2/**")
            .authorizeRequests().anyRequest().authenticated()
            .and()
            .oauth2Login().and().oauth2Client()

        logger.info("configure: /login/oauth2/** => OAuth")
    }
}