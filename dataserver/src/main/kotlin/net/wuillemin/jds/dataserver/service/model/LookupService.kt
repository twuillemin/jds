package net.wuillemin.jds.dataserver.service.model

import com.fasterxml.jackson.databind.ObjectMapper
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.service.LocalisationService
import net.wuillemin.jds.dataserver.dto.query.PromoteColumnToLookupQuery
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderGSheet
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.query.And
import net.wuillemin.jds.dataserver.entity.query.ColumnName
import net.wuillemin.jds.dataserver.entity.query.Equal
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.access.DataAccessService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for managing lookup
 *
 * @param dataSourceService The service for retrieving DataSource
 * @param dataProviderService The service for retrieving DataProvider
 * @param dataAccessService The service for retrieving data
 * @param localisationService The service for getting translated message
 * @param objectMapper The JSON converter
 */
@Service
class LookupService(
    private val dataSourceService: DataSourceService,
    private val dataProviderService: DataProviderService,
    private val dataAccessService: DataAccessService,
    private val localisationService: LocalisationService,
    private val objectMapper: ObjectMapper
) {

    /**
     * Promote a column to as a lookup column. The process starts with basic tests (existence of the columns, etc.) that will
     * fail immediately. Then the existing data are converted to lookup data, saved and the definition of the column is updated.
     * If the existing data can not be converted, an error message is generated and returned. In case of any error, the update
     * is not done
     *
     * @param dataProvider The data provider holding the column to be promoted
     * @param query The details of the promotion
     * @param locale The locale for error messages
     *
     * @return a pair of the new DataProvider and the errors. If an error was raised a list of errors. If empty, then the conversion was successful
     */
    fun promoteColumnToLookup(
        dataProvider: DataProvider,
        query: PromoteColumnToLookupQuery,
        locale: Locale
    ): DataProvider {

        // Retrieve the needed objects (or die trying)
        val targetDataSource = dataSourceService.getDataSourceById(query.dataSourceId)
        val targetDataProvider = dataProviderService.getDataProviderById(targetDataSource.dataProviderId)

        // Get the columns involved
        val columnToUpdate = dataProvider.columns.firstOrNull { it.name == query.columnName }
            ?: throw BadParameterException(E.service.model.lookup.columnDoesNotExist, query.columnName)
        val targetKeyColumn = targetDataProvider.columns.firstOrNull { it.name == query.keyColumnName }
            ?: throw BadParameterException(E.service.model.lookup.columnDoesNotExist, query.columnName)
        val targetValueColumn = targetDataProvider.columns.firstOrNull { it.name == query.valueColumnName }
            ?: throw BadParameterException(E.service.model.lookup.columnDoesNotExist, query.columnName)

        // Ensure that column to update is a string
        if (columnToUpdate.dataType != DataType.STRING) {
            throw BadParameterException(E.service.model.lookup.columnIsNotString, columnToUpdate.name, columnToUpdate.dataType)
        }
        // Ensure that target key column is a string
        if (targetKeyColumn.dataType != DataType.STRING) {
            throw BadParameterException(E.service.model.lookup.columnIsNotString, targetKeyColumn.name, targetKeyColumn.dataType)
        }
        // Ensure that target value column is a string
        if (targetValueColumn.dataType != DataType.STRING) {
            throw BadParameterException(E.service.model.lookup.columnIsNotString, targetValueColumn.name, targetValueColumn.dataType)
        }

        // Get the list of primary keys of the table to update
        val primaryKeys = dataProvider.columns.filter { it.storageDetail.primaryKey }.map { it.name }
        if (primaryKeys.isEmpty()) {
            throw BadParameterException(E.service.model.lookup.dataProviderWithoutPrimaryKey, dataProvider)
        }

        // Retrieve all data for the possible Lookup
        val lookupValueKey = dataAccessService.getData(targetDataSource)
            .mapNotNull { line ->
                line[targetKeyColumn.name]
                    ?.let { key ->
                        line[targetValueColumn.name]
                            ?.let { value ->
                                value to key
                            }
                    }
            }
            .toMap()

        // Prepare an array for storing soft errors during conversion
        val errors = ArrayList<String>()

        // Retrieve all data for the data provider
        val dataToUpdate = dataAccessService.getData(dataProvider)
            .mapNotNull { line ->
                line[columnToUpdate.name]
                    ?.let { originalColumnValue ->

                        // Split the original value
                        val originalValues = originalColumnValue.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toList()

                        // Get the values primary keys. As these are the values of the primary
                        // they will always be present, but the as data are received as a map,
                        // they could be null, so use getOrElse with an empty string
                        val lineId = primaryKeys
                            .map { primaryKey ->
                                val value = line[primaryKey]
                                    ?: throw ConstraintException(E.service.model.lookup.primaryKeyValueIsNull, dataProvider, primaryKey)
                                primaryKey to value
                            }
                            .toList()

                        // Convert to new values
                        val newValues = if (originalValues.isEmpty()) {
                            null
                        }
                        else {
                            if (originalValues.size > query.maximumNumberOfLookups) {
                                errors.add(localisationService.getMessage(E.service.model.lookup.tooManyValues, arrayOf(lineId, originalValues.size), locale))
                                null
                            }
                            else {
                                originalValues.mapNotNull { originalValue ->
                                    lookupValueKey[originalValue]
                                        ?.toString()
                                        ?: run {
                                            errors.add(localisationService.getMessage(E.service.model.lookup.unknownLookupValue, arrayOf(lineId, originalValue), locale))
                                            null
                                        }
                                }
                            }
                        }

                        // Make a predicate for the unique id
                        val predicates = lineId
                            .map { Equal(ColumnName(it.first), Value(it.second)) }
                            .toList()

                        val predicate = if (predicates.size == 1) {
                            predicates[0]
                        }
                        else {
                            And(predicates)
                        }

                        // Build the new value for the attribute
                        val newStringValue = newValues?.let { objectMapper.writeValueAsString(newValues) }

                        // Ensure that it is not too large
                        if (newStringValue?.length ?: 0 > columnToUpdate.size) {
                            // If too large, keep the value as in any case it won't be written
                            errors.add(localisationService.getMessage(
                                E.service.model.lookup.tooLargeLookup,
                                arrayOf(lineId, newStringValue?.length ?: 0, columnToUpdate.size),
                                locale))
                        }

                        // Make a map holding the value to set
                        val values = mapOf(columnToUpdate.name to newStringValue)

                        predicate to values
                    }
            }

        // If there are errors, throw a constraint exception
        if (errors.isNotEmpty()) {
            throw ConstraintException(E.service.model.lookup.failedToPromoteLookup, query.columnName, errors.joinToString("\n"))
        }

        // Process all the updates in a long transaction
        dataAccessService.createLongTransaction(dataProvider).use { transaction ->

            // Update the data
            dataToUpdate.forEach {
                dataAccessService.updateData(dataProvider, it.first, it.second, transaction)
            }

            transaction.commit()
        }

        // Update the column definition
        val newColumns = dataProvider.columns.map {
            if (it.name != columnToUpdate.name) {
                it
            }
            else {
                ColumnLookup(
                    it.name,
                    DataType.LIST_OF_STRINGS,
                    it.size,
                    it.storageDetail,
                    query.maximumNumberOfLookups,
                    query.dataSourceId,
                    query.keyColumnName,
                    query.valueColumnName)
            }
        }

        // Save the new data provider
        return dataProviderService.updateDataProvider(
            when (dataProvider) {
                is DataProviderSQL    -> dataProvider.copy(columns = newColumns)
                is DataProviderGSheet -> dataProvider.copy(columns = newColumns)
            })
    }
}