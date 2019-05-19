package net.wuillemin.jds.common.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource
import org.h2.Driver
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder




@ComponentScan(basePackages = ["net.wuillemin.jds.common.repository"])
@Import(CommonConfigTest::class)
class CommonConfigDataBaseTest {

    @Bean(name = ["commonJdbcTemplate"])
    internal fun commonJdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(commonDataSource())
    }

    @Bean(name = ["commonDatabaseJDBC"])
    fun commonDataSource(): DataSource {

        /*val ds = EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:jdbc/schema.sql")
            .build()
*/
        val datasource = DataSourceBuilder
            .create()
            .url("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build()

        datasource.connection.use { connection ->

            connection.createStatement().use { statement ->

                statement.execute(
                    "CREATE TABLE IF NOT EXISTS jds_user(" +
                        "    id bigint auto_increment," +
                        "    name VARCHAR NOT NULL," +
                        "    password VARCHAR NOT NULL," +
                        "    first_name VARCHAR NOT NULL," +
                        "    last_name VARCHAR NOT NULL," +
                        "    enabled BOOLEAN NOT NULL," +
                        "    profile VARCHAR NOT NULL" +
                        ")")
            }

            connection.createStatement().use { statement ->

                statement.execute(
                    "CREATE TABLE IF NOT EXISTS jds_group(" +
                        "    id bigint auto_increment," +
                        "    name VARCHAR NOT NULL," +
                        ")")
            }

            connection.createStatement().use { statement ->

                statement.execute(
                    "CREATE TABLE IF NOT EXISTS jds_group_user(" +
                        "    group_id bigint," +
                        "    user_id bigint," +
                        "    is_admin INT," +
                        ")")
            }

        }


        return datasource

    }
}