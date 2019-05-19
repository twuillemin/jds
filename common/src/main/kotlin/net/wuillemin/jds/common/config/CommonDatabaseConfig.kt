package net.wuillemin.jds.common.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@ComponentScan(basePackages = ["net.wuillemin.jds.common.repository"])
class CommonDatabaseConfig(private val commonProperties: CommonProperties){

    @Bean(name = ["commonJdbcTemplate"])
    internal fun commonJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(commonDataSource())
    }

    @Bean(name = ["commonDatabaseJDBC"])
    fun commonDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .url(commonProperties.database.jdbcConnectionUrl)
            .username(commonProperties.database.user)
            .password(commonProperties.database.password)
            .driverClassName(commonProperties.database.driverClassName)
            .build();
    }
}

