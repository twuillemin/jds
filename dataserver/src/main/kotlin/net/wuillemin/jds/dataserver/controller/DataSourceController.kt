package net.wuillemin.jds.dataserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
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
 * Controller for managing dataSources
 * @param dataProviderService The service for managing [DataProvider]
 * @param dataSourceService The service for managing [DataSource]
 * @param schemaService The service for managing [Schema]
 * @param serverService The service for managing [Server]
 * @param groupService  The service for managing [Group]
 * @param logger The logger
 */
@RestController
@RequestMapping("api/dataserver/v1/configuration/dataSources")
@Api(tags = ["DataServer - Configuration"], description = "Configure the data model")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
class DataSourceController(
    val dataProviderService: DataProviderService,
    val dataSourceService: DataSourceService,
    val schemaService: SchemaService,
    val serverService: ServerService,
    val groupService: GroupService,
    val logger: Logger
) {

    /**
     * Return all the dataSources
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Return all the dataSources.")
    @GetMapping
    fun getAllDataSources(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<DataSource>> {

        logger.debug("getAllDataSources: ${authentication.getLoggingId()}")

        val dataSources = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            val servers = serverService.getServersForGroupIds(authentication.permission.adminGroupIds)
            val schemas = schemaService.getSchemasForServers(servers)
            val dataProviders = dataProviderService.getDataProvidersForSchemas(schemas)
            dataSourceService.getDataSourcesForDataProviders(dataProviders)
        }
        else {
            dataSourceService.getDataSources()
        }

        return ResponseEntity(
            dataSources,
            HttpStatus.OK)
    }

    /**
     * Retrieve the detail of a single dataSource.
     *
     * @param dataSourceId The id of the dataSource to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a DataSource by its id.")
    @GetMapping("{id}")
    fun getDataSourceById(
        @PathVariable("id") dataSourceId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataSource> {

        logger.debug("getDataSourceById($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)
        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataSource.getDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataSource, HttpStatus.OK)
    }

    /**
     * Create a new dataSource
     *
     * @param dataSource The dataSource to create. If an id is given in the object, an exception will be thrown
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a new DataSource.")
    @PostMapping
    fun createDataSource(
        @RequestBody dataSource: DataSource,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataSource> {

        logger.debug("createDataSource($dataSource): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataSource.createDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataSourceService.addDataSource(dataSource), HttpStatus.CREATED)
    }

    /**
     * Update an existing dataSource. Ids of the object and the query must be equals
     *
     * @param dataSourceId The id of the dataSource to update
     * @param dataSource The dataSource to create. If an id is given in the object, it will be ignored
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update an existing dataSource.")
    @PutMapping("{id}")
    fun updateDataSource(
        @PathVariable("id") dataSourceId: Long,
        @RequestBody dataSource: DataSource,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataSource> {

        logger.debug("updateDataSource($dataSourceId,$dataSource): ${authentication.getLoggingId()}")

        val existingDataSource = dataSourceService.getDataSourceById(dataSourceId)
        val dataProvider = dataProviderService.getDataProviderById(existingDataSource.dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (dataSource.id != dataSourceId) {
            throw BadParameterException(E.controller.dataSource.updateDifferentIds)
        }

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataSource.updateDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataSourceService.updateDataSource(dataSource), HttpStatus.OK)
    }

    /**
     * Delete a dataSource
     *
     * @param dataSourceId The id of the dataSource to delete
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete a DataSource.")
    @DeleteMapping("{id}")
    fun deleteDataSource(
        @PathVariable("id") dataSourceId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.debug("deleteDataSource($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)
        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)


        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataSource.deleteDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        dataSourceService.deleteDataSource(dataSource)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}

