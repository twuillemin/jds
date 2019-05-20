package net.wuillemin.jds.dataserver.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource


/**
 * The configuration for the database of the data server repositories
 */
@Configuration
@ComponentScan(basePackages = ["net.wuillemin.jds.dataserver.repository"])
class DataServerDatabaseConfig(private val dataServerProperties: DataServerProperties) {

    /**
     * Create the JDBC template for the data server configuration repositories
     *
     * @return the JDBC template for the data server configuration repositories
     */
    @Bean(name = ["dataserverJdbcTemplate"])
    internal fun dataserverJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(dataserverDataSource())
    }

    /**
     * Create the JDBC [DataSource] for the data server configuration repositories
     *
     * @return the JDBC DataSource for the data server configuration repositories
     */
    @Bean(name = ["dataserverDataSource"])
    fun dataserverDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .url(dataServerProperties.configurationDatabase.jdbcConnectionUrl)
            .username(dataServerProperties.configurationDatabase.user)
            .password(dataServerProperties.configurationDatabase.password)
            .driverClassName(dataServerProperties.configurationDatabase.driverClassName)
            .build()
    }
}
