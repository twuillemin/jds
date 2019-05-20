package net.wuillemin.jds.common.config

import org.slf4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

/**
 * Configure the security of the application
 */
@Configuration
@Order(9)
class WebSecurity09Config(
    private val logger: Logger
) : WebSecurityConfigurerAdapter() {

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

        http.antMatcher("/**")
            .cors()
            .and()
            .csrf().disable()
            .authorizeRequests().anyRequest().permitAll()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        logger.info("configure: /** => permitAll")
    }
}