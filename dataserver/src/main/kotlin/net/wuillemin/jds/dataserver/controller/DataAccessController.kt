package net.wuillemin.jds.dataserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.dataserver.dto.query.DeleteDataQuery
import net.wuillemin.jds.dataserver.dto.query.GetDataQuery
import net.wuillemin.jds.dataserver.dto.query.InsertDataQuery
import net.wuillemin.jds.dataserver.dto.query.MassInsertDataQuery
import net.wuillemin.jds.dataserver.dto.query.UpdateDataQuery
import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.access.DataAccessService
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

/**
 * API for accessing the data
 *
 * @param dataAccessService The basic service
 * @param dataSourceService The service for retrieving data source
 * @param dataProviderServer The service for retrieving data provider
 * @param logger The logger
 */
@RestController
@RequestMapping("api/dataserver/v1/client")
@Api(tags = ["DataServer - Access"], description = "Deliver and update data")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN", "ROLE_SERVICE"])
class DataAccessController(
    private val dataAccessService: DataAccessService,
    private val dataSourceService: DataSourceService,
    private val dataProviderServer: DataProviderService,
    private val logger: Logger
) {

    /**
     * Retrieve all the readable DataSources
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Get all the DataSources readable by the user")
    @GetMapping("readableDataSources")
    fun getReadableDataSources(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<DataSource>> {

        logger.debug("getReadableDataSources: ${authentication.getLoggingId()}")

        val dataSources = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            dataSourceService.getDataSourcesForReaderId(authentication.permission.userId)
        }
        else {
            dataSourceService.getDataSources()
        }

        return ResponseEntity(dataSources, HttpStatus.OK)
    }

    /**
     * Retrieve the data for a data source
     *
     * @param body The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Get all the data from a DataSource")
    @PostMapping("{dataSourceId}/data")
    fun getData(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @RequestBody body: GetDataQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Map<String, Any>>> {

        logger.debug("getData($dataSourceId,$body): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToReadIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.getDataDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.getData(
                dataSource,
                body.filter,
                body.orders
            ),
            HttpStatus.OK)
    }

    /**
     * Retrieve the lookups values for a data source
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Get all the lookups values for a DataSource")
    @PostMapping("{dataSourceId}/lookupValues")
    fun getLookupValues(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Map<String, Map<String, Any>>> {

        logger.debug("getLookupValues($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToReadIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.getDataDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.retrieveAllLookups(dataProviderServer.getDataProviderById(dataSource.dataProviderId)),
            HttpStatus.OK)
    }

    /**
     * Retrieve the columns for a data source
     *
     * @id The id of the dataSource to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Get all the columns of a DataSource")
    @GetMapping("{dataSourceId}/columns")
    fun getColumns(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Column>> {

        logger.debug("getColumns($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToReadIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.getColumnsDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.getColumns(dataSource),
            HttpStatus.OK)
    }

    /**
     * Add new entry in the database
     *
     * @param body The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Create a new entry in a DataSource")
    @PostMapping("{dataSourceId}/insert")
    fun insertData(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @RequestBody body: InsertDataQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Int> {

        logger.debug("insertData($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToWriteIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.insertDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.insertData(dataSource, body.data),
            HttpStatus.OK)
    }

    /**
     * Add multiple new entries in the database
     *
     * @param body The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Create multiple new entries in a DataSource")
    @PostMapping("{dataSourceId}/massInsert")
    fun massInsertData(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @RequestBody body: MassInsertDataQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Int> {

        logger.debug("massInsertData($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToWriteIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.massInsertDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.massInsertData(dataSource, body.data),
            HttpStatus.OK)
    }

    /**
     * Update existing entries in the database
     *
     * @param body The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Update existing entries in a DataSource")
    @PutMapping("{dataSourceId}/update")
    fun updateData(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @RequestBody body: UpdateDataQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Int> {

        logger.debug("updateData($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToWriteIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.updateDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.updateData(
                dataSource,
                body.filter,
                body.data),
            HttpStatus.OK)
    }

    /**
     * Delete existing entries in the database
     *
     * @param body The query
     * @param authentication The authentication of the requester
     */
    @ApiOperation(value = "Delete existing entries in a DataSource")
    @PutMapping("{dataSourceId}/delete")
    fun deleteData(
        @PathVariable("dataSourceId") dataSourceId: Long,
        @RequestBody body: DeleteDataQuery,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Int> {

        logger.debug("deleteData($dataSourceId): ${authentication.getLoggingId()}")

        val dataSource = dataSourceService.getDataSourceById(dataSourceId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            if (!dataSource.userAllowedToDeleteIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.dataAccess.deleteDenied, authentication.permission.userId, dataSource.getLoggingId())
            }
        }

        return ResponseEntity(
            dataAccessService.deleteData(dataSource, body.filter),
            HttpStatus.OK
        )
    }
}

