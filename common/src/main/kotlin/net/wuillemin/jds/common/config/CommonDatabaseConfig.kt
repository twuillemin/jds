package net.wuillemin.jds.common.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * The configuration for the database of the common services
 */
@Configuration
@ComponentScan(basePackages = ["net.wuillemin.jds.common.repository"])
class CommonDatabaseConfig(private val commonProperties: CommonProperties) {

    /**
     * Create the JDBC template for common repositories
     *
     * @return the JDBC template for common repositories
     */
    @Bean(name = ["commonJdbcTemplate"])
    fun commonJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(commonDataSource())
    }

    /**
     * Create the JDBC [DataSource] for common repositories
     *
     * @return the JDBC DataSource for common repositories
     */
    @Bean(name = ["commonDatabaseJDBC"])
    fun commonDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .url(commonProperties.database.jdbcConnectionUrl)
            .username(commonProperties.database.user)
            .password(commonProperties.database.password)
            .driverClassName(commonProperties.database.driverClassName)
            .build()
    }
}

