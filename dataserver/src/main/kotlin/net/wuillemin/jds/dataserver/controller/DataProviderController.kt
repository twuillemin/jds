package net.wuillemin.jds.dataserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.dataserver.dto.Preview
import net.wuillemin.jds.dataserver.dto.query.ImportCSVQuery
import net.wuillemin.jds.dataserver.dto.query.ImportSQLQuery
import net.wuillemin.jds.dataserver.dto.query.PromoteColumnToLookupQuery
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.importation.CSVImporter
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.LookupService
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
import java.util.*

/**
 * Controller for managing dataProviders
 * @param dataProviderService The service for managing [DataProvider]
 * @param dataSourceService The service for managing [DataSource]
 * @param schemaService The service for managing [Schema]
 * @param serverService The service for managing [Server]
 * @param groupService  The service for managing [Group]
 * @param lookupService The service for managing the lookup
 * @param modelService The service for reading model
 * @param csvImport The service for importing CSV Files
 * @param logger The logger
 */
@RestController
@RequestMapping("api/dataserver/v1/configuration/dataProviders")
@Api(tags = ["DataServer - Configuration"], description = "Configure the data model")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
class DataProviderController(
    val dataProviderService: DataProviderService,
    val dataSourceService: DataSourceService,
    val schemaService: SchemaService,
    val serverService: ServerService,
    val groupService: GroupService,
    val lookupService: LookupService,
    val modelService: ModelService,
    val csvImport: CSVImporter,
    val logger: Logger
) {

    /**
     * Return all the dataProviders
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Return all the dataProviders.")
    @GetMapping
    fun getAllDataProviders(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<DataProvider>> {

        logger.debug("getAllDataProviders: ${authentication.getLoggingId()}")

        val dataProviders = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            val servers = serverService.getServersForGroupIds(authentication.permission.adminGroupIds)
            val schemas = schemaService.getSchemasForServers(servers)
            dataProviderService.getDataProvidersForSchemas(schemas)
        }
        else {
            dataProviderService.getDataProviders()
        }

        return ResponseEntity(
            dataProviders,
            HttpStatus.OK)
    }

    /**
     * Retrieve the detail of a single dataProvider.
     *
     * @param dataProviderId The id of the dataProvider to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a DataProvider by its id.")
    @GetMapping("{id}")
    fun getDataProviderById(
        @PathVariable("id") dataProviderId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataProvider> {

        logger.debug("getDataProviderById($dataProviderId): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.getDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataProvider, HttpStatus.OK)
    }

    /**
     * Create a new dataProvider
     *
     * @param dataProvider The dataProvider to create. If an id is given in the object, an exception will be thrown
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a new DataProvider.")
    @PostMapping
    fun createDataProvider(
        @RequestBody dataProvider: DataProvider,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataProvider> {

        logger.debug("createDataProvider($dataProvider): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.createDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataProviderService.addDataProvider(dataProvider), HttpStatus.CREATED)
    }

    /**
     * Update an existing dataProvider. Ids of the object and the query must be equals
     *
     * @param dataProviderId The id of the dataProvider to update
     * @param dataProvider The dataProvider to create. If an id is given in the object, it will be ignored
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update an existing dataProvider.")
    @PutMapping("{id}")
    fun updateDataProvider(
        @PathVariable("id") dataProviderId: Long,
        @RequestBody dataProvider: DataProvider,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataProvider> {

        logger.debug("updateDataProvider($dataProviderId, $dataProvider): ${authentication.getLoggingId()}")

        val existingDataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(existingDataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (dataProvider.id != dataProviderId) {
            throw BadParameterException(E.controller.dataProvider.updateDifferentIds)
        }

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.updateDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(dataProviderService.updateDataProvider(dataProvider), HttpStatus.OK)
    }

    /**
     * Delete a dataProvider
     *
     * @param dataProviderId The id of the dataProvider to delete
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete a DataProvider.")
    @DeleteMapping("{id}")
    fun deleteDataProvider(
        @PathVariable("id") dataProviderId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.debug("deleteDataProvider($dataProviderId): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)


        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.deleteDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        dataProviderService.deleteDataProvider(dataProvider)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    /**
     * Retrieve the [DataSource]s of a dataProvider.
     *
     * @param dataProviderId The id of the dataProvider from which to retrieve data providers
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the data providers for a dataProvider.")
    @GetMapping("{id}/dataSources")
    fun getDataSources(
        @PathVariable("id") dataProviderId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<DataSource>> {

        logger.debug("getDataSources($dataProviderId): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.getDataSourcesDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        // If the user is not an admin, only retrieve server for its groups and filter them
        val dataSources = dataSourceService.getDataSourcesForDataProvider(dataProvider)

        return ResponseEntity(dataSources, HttpStatus.OK)
    }

    /**
     * Retrieve the preview of the data of a DataProviderSQL
     *
     * @param dataProviderId The id of the server from which to retrieve tables
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Preview the data and the columns of a DataProvider")
    @GetMapping("{id}/preview")
    fun getDataProviderPreview(
        @PathVariable("id") dataProviderId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Preview> {

        logger.debug("getDataProviderPreview($dataProviderId): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" } && !authentication.permission.adminGroupIds.contains(server.groupId)) {
            throw DeniedPermissionException(E.controller.dataProvider.previewDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
        }

        return ResponseEntity(modelService.getPreview(dataProvider), HttpStatus.OK)
    }

    /**
     * Create a DataProvider (SQL) from a query
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a DataProvider from a SQL query")
    @PostMapping("importFromSQL")
    fun importFromSQL(
        @RequestBody importSQLQuery: ImportSQLQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataProvider> {

        logger.debug("importFromSQL($importSQLQuery): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(importSQLQuery.schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" } && !authentication.permission.adminGroupIds.contains(server.groupId)) {
            throw DeniedPermissionException(E.controller.dataProvider.importSqlDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
        }

        return ResponseEntity(
            modelService.createDataProviderFromSQL(
                schema,
                importSQLQuery.name,
                importSQLQuery.query),
            HttpStatus.OK)
    }

    /**
     * Create a DataProvider (SQL) from a CSV file
     *
     * @param importCSVQuery The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create auto-magically a DataProvider and a DataSource from a CSV file")
    @PostMapping("autoImportFromCSV")
    fun autoImportFromCSV(
        @RequestBody importCSVQuery: ImportCSVQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataSource> {

        logger.debug("autoImportFromCSV($importCSVQuery): ${authentication.getLoggingId()}")

        val schema = schemaService.getSchemaById(importCSVQuery.schemaId)
        val server = serverService.getServerById(schema.serverId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" } && !authentication.permission.adminGroupIds.contains(server.groupId)) {
            throw DeniedPermissionException(E.controller.dataProvider.importSqlDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
        }

        // Parse the String
        val data = String(Base64.getDecoder().decode(importCSVQuery.dataBase64))

        return ResponseEntity(
            csvImport.autoImportCSV(schema, importCSVQuery.tableName, data),
            HttpStatus.OK)
    }

    /**
     * Promote a column as a lookup
     *
     * @param dataProviderId The id of the dataProvider to update
     * @param query The details of the operation requested
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Promote a column as a lookup.")
    @PutMapping("{id}/promoteColumnAsLookup")
    fun promoteColumnAsLookup(
        @PathVariable("id") dataProviderId: Long,
        @RequestBody query: PromoteColumnToLookupQuery,
        @ApiIgnore locale: Locale,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<DataProvider> {

        logger.debug("promoteColumnAsLookup($dataProviderId, $query): ${authentication.getLoggingId()}")

        val dataProvider = dataProviderService.getDataProviderById(dataProviderId)
        val schema = schemaService.getSchemaById(dataProvider.schemaId)
        val server = serverService.getServerById(schema.serverId)

        // If the user is not an admin, check parameters thoroughly
        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!authentication.permission.adminGroupIds.contains(server.groupId)) {
                throw DeniedPermissionException(E.controller.dataProvider.updateDenied, authentication.permission.userId, groupService.getGroupById(server.groupId))
            }
        }

        return ResponseEntity(lookupService.promoteColumnToLookup(dataProvider, query, locale), HttpStatus.OK)
    }
}

