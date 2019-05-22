package net.wuillemin.jds.dataserver.service.importation

import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.config.DataServerProperties
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.sql.DriverManager

/**
 * A service for creating initial environment at group creation
 *
 * @param serverService The service for managing [Server]
 * @param schemaService The service for managing [Schema]
 * @param dataServerProperties The configuration of the DataServer
 * @param logger The logger
 */
@Service
class GroupInitializer(
    private val serverService: ServerService,
    private val schemaService: SchemaService,
    private val dataServerProperties: DataServerProperties,
    private val logger: Logger
) {

    /**
     * Create the original SQL environment for a group: Server, Schema, roles, etc.
     *
     * @param group the group
     */
    fun createSQLEnvironmentForGroup(group: Group) {

        group.id
            ?.let { groupId ->

                // Decide of a server
                val hostingServer = dataServerProperties.storageDatabase.random()

                // Get a name
                val roleName = if (group.name.length > 8) {
                    group.name.substring(0, 7)
                }
                else {
                    group.name
                }.toUpperCase()


                DriverManager
                    .getConnection(
                        hostingServer.jdbcConnectionUrl,
                        hostingServer.user,
                        hostingServer.password)
                    .use { connection ->
                        val databaseName = connection.catalog

                        // Create a role with limited rights
                        connection
                            .prepareStatement("CREATE ROLE $roleName WITH NOSUPERUSER NOCREATEDB NOCREATEROLE NOCREATEUSER NOINHERIT NOLOGIN")
                            .use { statement ->
                                statement.execute()
                            }

                        // Remove the rights of the role on the database
                        connection
                            .prepareStatement("REVOKE ALL ON DATABASE $databaseName FROM $roleName")
                            .use { statement ->
                                statement.execute()
                            }

                        // Remove the rights of the role on the public schema
                        connection
                            .prepareStatement("REVOKE ALL ON SCHEMA public FROM $roleName")
                            .use { statement ->
                                statement.execute()
                            }

                        // Give the role to the user used to connect to db (so that it can do SET SESSION ROLE)
                        connection
                            .prepareStatement("GRANT $roleName TO ${hostingServer.user}")
                            .use { statement ->
                                statement.execute()
                            }

                        // Create a schema for the role
                        connection
                            .prepareStatement("CREATE SCHEMA $roleName AUTHORIZATION $roleName")
                            .use { statement ->
                                statement.execute()
                            }
                    }

                // Create a server object
                val server = serverService.addServer(
                    ServerSQL(
                        null,
                        "Internal Server for ${group.name}",
                        groupId,
                        false,
                        hostingServer.jdbcConnectionUrl,
                        hostingServer.user,
                        hostingServer.password,
                        hostingServer.driverClassName))

                val schema = schemaService.addSchema(
                    SchemaSQL(
                        null,
                        "Internal Schema for ${group.name}",
                        server.id!!,
                        roleName))

                logger.info("createSQLEnvironmentForGroup: The SQL environment was created successfully for group ${group.getLoggingId()}: server: ${server.getLoggingId()}, schema: ${schema.getLoggingId()} ")
            }
            ?: throw BadParameterException(E.service.import.groupNotPersisted)

    }
}