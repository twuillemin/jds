package net.wuillemin.jds.dataserver.service.model

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.C
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.event.ServerCreatedEvent
import net.wuillemin.jds.dataserver.event.ServerDeletedEvent
import net.wuillemin.jds.dataserver.event.ServerUpdatedEvent
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.repository.ServerRepository
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Service for managing the servers (the basic supplier of data)
 *
 * @param serverRepository The repository with the server configuration
 * @param schemaService The service for [Schema]
 * @param applicationEventPublisher The application's events
 * @param logger The logger
 */
@Service
class ServerService(
    private val serverRepository: ServerRepository,
    private val schemaService: SchemaService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger
) {

    /**
     * Get the list of all servers.
     *
     * @return the list of all servers
     */
    fun getServers(): List<Server> {
        return serverRepository.findAll()
    }

    /**
     * Get the list of all servers referencing the group id
     *
     * @param groupIds The id of the group for which to retrieve the servers
     * @return the servers
     */
    fun getServersForGroupIds(groupIds: List<Long>): List<Server> {
        return serverRepository.findByGroupIdIn(groupIds)
    }

    /**
     * Get a server by its id.
     *
     * @param id The id of the server
     * @return the server
     * @throws NotFoundException if the server does not exist
     */
    fun getServerById(id: Long): Server {
        return serverRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, Server::class)
        }
    }

    /**
     * Create a new server in the database. Send a [ServerCreatedEvent] in case of success.
     *
     * @param server The server to create
     * @return the server created
     * @throws BadParameterException if the server parameter has it id property not null
     */
    fun addServer(server: Server): Server {

        return server.id
            ?.let {
                throw BadParameterException(E.service.model.server.createAlreadyPersisted)
            }
            ?: run {
                serverRepository
                    .save(server)
                    .also { createdServer ->
                        applicationEventPublisher.publishEvent(ServerCreatedEvent(server.id!!))
                        logger.info("addServer: The server ${createdServer.getLoggingId()} has been created")
                    }
            }
    }

    /**
     * Update a server by its its id. Send a [ServerUpdatedEvent] in case of success.
     *
     * @param server the new information for the server
     * @return the updated server
     * @throws NotFoundException if there is no existing server
     * @throws ConstraintException if the customerDefined property or the groupId property is updated
     * @throws BadParameterException if the server parameter has it id property to null
     */
    fun updateServer(server: Server): Server {

        return server.id
            ?.let { serverId ->

                val existingServer = getServerById(serverId)

                if (server.customerDefined != existingServer.customerDefined) {
                    throw ConstraintException(E.service.model.server.updateCustomerDefined)
                }

                if (server.groupId != existingServer.groupId) {
                    throw ConstraintException(E.service.model.server.updateGroupId)
                }

                serverRepository
                    .save(server)
                    .also {
                        applicationEventPublisher.publishEvent(ServerUpdatedEvent(serverId))
                    }
            }
            ?: throw BadParameterException(E.service.model.server.updateNotPersisted)

    }

    /**
     * Delete a server. Send a [ServerDeletedEvent] in case of success.
     *
     * @param server The server delete
     * @throws ConstraintException if the server is referenced by one ore more [Schema]
     * @throws BadParameterException if the server parameter has it id property to null
     */
    fun deleteServer(server: Server) {

        server.id
            ?.let { serverId ->

                if (schemaService.getSchemasForServer(server).isNotEmpty()) {
                    throw ConstraintException(E.service.model.server.deleteReferenced, server)
                }

                serverRepository.delete(server)

                applicationEventPublisher.publishEvent(ServerDeletedEvent(serverId))

                logger.info("deleteServer: The server $server has been deleted")
            }
            ?: throw BadParameterException(E.service.model.server.deleteNotPersisted)

    }
}
