package net.wuillemin.jds.dataserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.dataserver.dto.Preview
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.ModelService
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
 * @param dataProviderService The service for managing [DataProvider]
 * @param schemaService The service for managing [Schema]
 * @param serverService The service for managing [Server]
 * @param groupService  The service for managing [Group]
 * @param modelService The service for reading model
 * @param logger The logger
 */
@RestController
@RequestMapping("api/dataserver/v1/configuration/schemas")
@Api(tags = ["DataServer - Configuration"], description = "Configure the data model")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
class SchemaController(
    val schemaService: SchemaService,
    val serverService: ServerService,
    val dataProviderService: DataProviderService,
    val groupService: GroupService,
    val modelService: ModelService,
    val logger: Logger
) {

    /**
     * Return all the schemas
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Return all the schemas.")
    @GetMapping
    fun getAllSchemas(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Schema>> {

        logger.debug("getAllSchemas: ${authentication.getLoggingId()}")

        val schemas = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            val servers = serverService.getServersForGroupIds(authentication.permission.adminGroupIds)
            schemaService.getSchemasForServers(servers)
        }
        else {
            schemaService.getSchemas()
        }

        return ResponseEntity(
            schemas,
            HttpStatus.OK)
    }

    /**
     * Retrieve the detail of a single schema.
     *
     * @param schemaId The id of the schema to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a Schema by its id.")
    @GetMapping("{id}")
    fun getSchemaById(
        @PathVariable("id") schemaId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Schema> {

        logger.debug("getSchemaById($schemaId): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.schema.getDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(schema, HttpStatus.OK)
    }

    /**
     * Create a new schema
     *
     * @param schema The schema to create. If an id is given in the object, an exception will be thrown
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a new Schema.")
    @PostMapping
    fun createSchema(
        @RequestBody schema: Schema,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Schema> {

        logger.debug("createSchema($schema): ${authentication.getLoggingId()}")

        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.schema.createDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!server.customerDefined) {
                throw DeniedPermissionException(E.controller.schema.createInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(schemaService.addSchema(schema), HttpStatus.CREATED)
    }

    /**
     * Update an existing schema. Ids of the object and the query must be equals
     *
     * @param schemaId The id of the schema to update
     * @param schema The schema to create. If an id is given in the object, it will be ignored
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update an existing schema.")
    @PutMapping("{id}")
    fun updateSchema(
        @PathVariable("id") schemaId: Long,
        @RequestBody schema: Schema,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Schema> {

        logger.debug("updateSchema($schemaId,$schema): ${authentication.getLoggingId()}")

        val existingSchema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(existingSchema.serverId)

        if (schema.id != schemaId) {
            throw BadParameterException(E.controller.schema.updateDifferentIds)
        }

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.schema.updateDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!server.customerDefined) {
                throw DeniedPermissionException(E.controller.schema.updateInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(schemaService.updateSchema(schema), HttpStatus.OK)
    }

    /**
     * Delete a schema
     *
     * @param schemaId The id of the schema to delete
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete a Schema.")
    @DeleteMapping("{id}")
    fun deleteSchema(
        @PathVariable("id") schemaId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.debug("deleteSchema($schemaId): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.schema.deleteDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }

            if (!server.customerDefined) {
                throw DeniedPermissionException(E.controller.schema.deleteInternalDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        schemaService.deleteSchema(schema)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    /**
     * Retrieve the [DataProvider]s of a schema.
     *
     * @param schemaId The id of the schema from which to retrieve data providers
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the data providers for a schema.")
    @GetMapping("{id}/dataProviders")
    fun getDataProviders(
        @PathVariable("id") schemaId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<DataProvider>> {

        logger.debug("getDataProviders($schemaId): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.schema.getDataProvidersDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        // If the user is not an admin, only retrieve server for its groups and filter them
        val dataProviders = dataProviderService.getDataProvidersForSchema(schema)

        return ResponseEntity(dataProviders, HttpStatus.OK)
    }

    /**
     * Retrieve the tables of a single schema.
     *
     * @param schemaId The id of the schema from which to retrieve tables
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the tables existing in a schema.")
    @GetMapping("{id}/tables")
    fun getSchemaTables(
        @PathVariable("id") schemaId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<String>> {

        logger.debug("getSchemaTables($schemaId): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" } && !authentication.permission.adminGroupIds.contains(server.groupId)) {
            throw DeniedPermissionException(E.controller.schema.getTablesDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
        }

        return ResponseEntity(modelService.getTables(schema), HttpStatus.OK)
    }

    /**
     * Retrieve the preview of table.
     *
     * @param schemaId The id of the schema from which to retrieve tables
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get the preview of table (columns and data).")
    @GetMapping("{id}/tables/{tableName}/preview")
    fun getSchemaPreviewTable(
        @PathVariable("id") schemaId: Long,
        @PathVariable("tableName") tableName: String,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Preview> {

        logger.debug("getPreviewTable($schemaId, $tableName): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" } && !authentication.permission.adminGroupIds.contains(server.groupId)) {
            throw DeniedPermissionException(E.controller.schema.previewTableDenied, authentication.permission.userId, groupService.getGroupById(server.groupId), tableName)
        }

        return ResponseEntity(modelService.getPreview(schema, tableName), HttpStatus.OK)
    }
}

