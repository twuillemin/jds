package net.wuillemin.jds.dataserver.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import javax.sql.DataSource


@ComponentScan(basePackages = ["net.wuillemin.jds.dataserver.repository"])
@Import(DataServerConfigTest::class)
class DataServerConfigDataBaseTest {

    @Bean(name = ["dataserverJdbcTemplate"])
    internal fun commonJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(commonDataSource())
    }

    @Bean(name = ["dataserverDatabaseJDBC"])
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
                    ScriptUtils.executeSqlScript(connection, ClassPathResource("/jdbc/datserver_create_db_test.sql"))
                }
            }
    }
}