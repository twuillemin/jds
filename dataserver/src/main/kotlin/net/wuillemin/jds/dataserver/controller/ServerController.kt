package net.wuillemin.jds.dataserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

/**
 * Controller for managing servers
 *
 * @param schemaService The service for managing [Schema]
 * @param serverService The service for managing [Server]
 * @param groupService  The service for managing [Group]
 * @param logger The logger
 */
@RestController
@RequestMapping("api/dataserver/v1/configuration/servers")
@Api(tags = ["DataServer - Configuration"], description = "Configure the data model")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
class ServerController(
    val serverService: ServerService,
    val schemaService: SchemaService,
    val groupService: GroupService,
    val logger: Logger
) {

    /**
     * Return all the servers
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Return all the servers.")
    @GetMapping
    fun getAllServers(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Server>> {

        logger.debug("getAllServers: ${authentication.getLoggingId()}")

        // If the user is not an admin, only retrieve server for its groups and filter them
        val servers = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            serverService
                .getServersForGroupIds(authentication.permission.adminGroupIds)
                .map { ensureSecuredServer(it) }
        }
        else {
            serverService.getServers()
        }

        return ResponseEntity(
            servers,
            HttpStatus.OK)
    }

    /**
     * Retrieve the detail of a single server.
     *
     * @param serverId The id of the server to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a Server by its id.")
    @GetMapping("{id}")
    fun getServerById(
        @PathVariable("id") serverId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Server> {

        logger.debug("getServerById($serverId): ${authentication.getLoggingId()}")

        val server = serverService
            .getServerById(serverId)
            .let { server ->
                // If the user is not an admin, check parameters thoroughly
                if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

                    if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                        throw DeniedPermissionException(E.controller.server.getDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
                    }

                    ensureSecuredServer(server)
                }
                // Otherwise use the object
                else {
                    server
                }
            }

        return ResponseEntity(server, HttpStatus.OK)
    }

    /**
     * Create a new server
     *
     * @param server     The server to create. If an id is given in the object, an exception will be thrown
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a new Server.")
    @PostMapping
    fun createServer(
        @RequestBody server: Server,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Server> {

        logger.debug("createServer($server): ${authentication.getLoggingId()}")

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.server.createDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!server.customerDefined) {
                throw DeniedPermissionException(E.controller.server.createInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(serverService.addServer(server), HttpStatus.CREATED)
    }

    /**
     * Update an existing server. Ids of the object and the query must be equals
     *
     * @param serverId The id of the server to update
     * @param server The server to create. If an id is given in the object, it will be ignored
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update an existing server.")
    @PutMapping("{id}")
    fun updateServer(
        @PathVariable("id") serverId: Long,
        @RequestBody server: Server,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Server> {

        logger.debug("updateServer($serverId,$server): ${authentication.getLoggingId()}")

        if (server.id != serverId) {
            throw BadParameterException(E.controller.server.updateDifferentIds)
        }

        val existingServer = serverService.getServerById(serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.server.updateDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!existingServer.customerDefined) {
                throw DeniedPermissionException(E.controller.server.updateInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(serverService.updateServer(server), HttpStatus.OK)
    }

    /**
     * Delete a server
     *
     * @param serverId The id of the server to delete
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete a Server.")
    @DeleteMapping("{id}")
    fun deleteServer(
        @PathVariable("id") serverId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.debug("deleteServer($serverId): ${authentication.getLoggingId()}")

        val server = serverService.getServerById(serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.server.deleteDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!server.customerDefined) {
                throw DeniedPermissionException(E.controller.server.deleteInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        serverService.deleteServer(server)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    /**
     * Retrieve the [Schema]s of a single server.
     *
     * @param serverId The id of the server from which to retrieve tables
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the schemas for a server.")
    @GetMapping("{id}/schemas")
    fun getSchemasById(
        @PathVariable("id") serverId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Schema>> {

        logger.debug("getSchemas($serverId): ${authentication.getLoggingId()}")

        val server = serverService.getServerById(serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.server.getSchemasDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        val schemas = schemaService.getSchemasForServer(server)

        return ResponseEntity(schemas, HttpStatus.OK)
    }

    /**
     * Ensure that sensitive information are removed from a server.
     *
     * @param server The server to clean
     * @return a cleaned server
     */
    private fun ensureSecuredServer(server: Server): Server {

        return if (server.customerDefined) {
            // If server was defined by the customer, it can be returned safely
            server
        }
        else {
            // Otherwise use the "safe" version
            server.copyWithoutSensitiveInformation()
        }
    }
}

