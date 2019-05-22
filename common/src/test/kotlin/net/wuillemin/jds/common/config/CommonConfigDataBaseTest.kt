package net.wuillemin.jds.common.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import javax.sql.DataSource


@ComponentScan(basePackages = ["net.wuillemin.jds.common.repository"])
@Import(CommonConfigTest::class)
class CommonConfigDataBaseTest {

    @Bean(name = ["commonJdbcTemplate"])
    internal fun commonJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(commonDataSource())
    }

    @Bean(name = ["commonDatabaseJDBC"])
    fun commonDataSource(): DataSource {

        return DataSourceBuilder
            .create()
            .url("jdbc:h2:mem:common;DB_CLOSE_DELAY=-1")
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build()
            .also {
                it.connection.use { connection ->
                    ScriptUtils.executeSqlScript(connection, ClassPathResource("/jdbc/common_create_db_test.sql"))
                }
            }
    }
}