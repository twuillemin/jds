package net.wuillemin.jds.dataserver.supplier.sql.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.ServerService
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Create and maintain connection with the various data bases
 *
 * @param serverService The service for retrieving [Server]
 * @param logger The logger
 */
@Service
class SQLConnectionCache(
    private val serverService: ServerService,
    private val logger: Logger
) {

    private val connectionPoolBySchemaId = ConcurrentHashMap<Long, HikariDataSource>()

    private val connectionPoolByConnectionInformation = ConcurrentHashMap<ConnectionInformation, HikariDataSource>()

    /**
     * Get a connection for a [Schema]. A new [Connection] is returned each time. The connection limiter to the use of
     * the schema.
     * @param schema The schema
     * @return the connection
     */
    fun getConnection(schema: SchemaSQL): Connection {

        logger.debug("getConnection(${schema.getLoggingId()})")

        // Get the provider
        val connectionProvider = getConnectionPoolForSchema(schema)
        // Get a connection
        val connection = connectionProvider.connection

        // Enforce the schema
        schema.roleName?.let { roleName ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SET SESSION ROLE $roleName")
            }
            connection.createStatement().use { statement ->
                statement.executeQuery("SET SEARCH_PATH TO ROLE $roleName")
            }
        }

        return (connection)
    }

    /**
     * Get a connection pool for a [Schema].
     * @param schema The server
     * @return the connection pool
     */
    private fun getConnectionPoolForSchema(schema: Schema): HikariDataSource {

        logger.debug("getConnectionPoolForSchema(${schema.getLoggingId()})")

        return schema.id
            ?.let { schemaId ->
                connectionPoolBySchemaId[schemaId]
                    ?: run {

                        // Load the server
                        val server = serverService.getServerById(schema.serverId).let {
                            it as? ServerSQL
                                ?: throw BadParameterException(E.supplier.sql.service.serverIsNotSql, it)
                        }

                        getConnectionPoolForServer(server)
                            .also {
                                connectionPoolBySchemaId[schemaId] = it
                            }
                    }

            }
            ?: throw BadParameterException(E.supplier.sql.service.cacheSchemaNotPersisted)

    }

    /**
     * Get a connection pool for a [ServerSQL].
     * @param server The server
     * @return the connection pool
     */
    private fun getConnectionPoolForServer(server: ServerSQL): HikariDataSource {

        logger.info("getConnectionPoolForServer: Adding connection pool for server ${server.getLoggingId()}")

        val connectionInformation = ConnectionInformation(server.jdbcURL, server.userName, server.password, server.driverClassName)

        return connectionPoolByConnectionInformation[connectionInformation]
            ?: run {

                val config = HikariConfig()
                config.jdbcUrl = server.jdbcURL
                config.username = server.userName
                config.password = server.password
                config.driverClassName = server.driverClassName
                config.addDataSourceProperty("cachePrepStmts", "true")
                config.addDataSourceProperty("prepStmtCacheSize", "250")
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

                val dataSourceProvider = HikariDataSource(config)

                connectionPoolByConnectionInformation[connectionInformation] = dataSourceProvider

                dataSourceProvider
            }
    }

    /**
     * An internal class to keep the unique information about a connection so that it could
     * be associated with a connection pool
     *
     * @param jdbcUrl The url of the connection
     * @param username The user of the connection
     * @param password The password of the connection
     * @param driverClassName The driver of the connection
     */
    data class ConnectionInformation(
        val jdbcUrl: String,
        val username: String?,
        val password: String?,
        val driverClassName: String
    )
}