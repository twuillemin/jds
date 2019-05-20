package net.wuillemin.jds.dataserver.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource


@Configuration
@ComponentScan(basePackages = ["net.wuillemin.jds.dataserver.repository"])
class DataServerDatabaseConfig(private val dataServerProperties: DataServerProperties) {

    @Bean(name = ["dataserverJdbcTemplate"])
    internal fun dataserverJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(dataserverDataSource())
    }

    @Bean(name = ["dataserverDataSource"])
    fun dataserverDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .url(dataServerProperties.configurationDatabase.jdbcConnectionUrl)
            .username(dataServerProperties.configurationDatabase.user)
            .password(dataServerProperties.configurationDatabase.password)
            .driverClassName(dataServerProperties.configurationDatabase.driverClassName)
            .build();
    }
}
