package net.wuillemin.jds.dataserver.service.access

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderGSheet
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaGSheet
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.entity.transaction.LongTransaction
import net.wuillemin.jds.dataserver.entity.transaction.sql.LongTransactionSQL
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLConnectionCache
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataWriter
import org.springframework.stereotype.Service

/**
 * Very simple and rough service for retrieving data and column
 *
 * @param dataSourceService The service for managing [DataSource]
 * @param dataProviderService The service for managing [DataProvider]
 * @param schemaService The service for managing [Schema]
 * @param sqlDataReader The connector to SQL
 * @param sqlDataWriter The connector to SQL
 * @param sqlConnectionCache The connections
 */
@Service
class DataAccessService(
    private val dataSourceService: DataSourceService,
    private val dataProviderService: DataProviderService,
    private val schemaService: SchemaService,
    private val sqlDataReader: SQLDataReader,
    private val sqlDataWriter: SQLDataWriter,
    private val sqlConnectionCache: SQLConnectionCache
) {

    /**
     * Get the data from a [DataSource]
     *
     * @param dataSource The data source to retrieve
     * @param filter The predicate to apply
     * @param orders the order to apply,
     * @param indexFirstRecord The index of the first record
     * @param numberOfRecords The number of records to retrieve
     * @return The results of the query
     */
    fun getData(
        dataSource: DataSource,
        filter: Predicate? = null,
        orders: List<Order>? = null,
        indexFirstRecord: Int? = null,
        numberOfRecords: Int? = null
    ): List<Map<String, Any>> {

        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)

        return getData(
            dataProvider,
            filter,
            orders,
            indexFirstRecord,
            numberOfRecords)
    }

    /**
     * Get the data from a [DataProvider]
     *
     * @param dataProvider The data provider to retrieve
     * @param filter The predicate to apply
     * @param orders the order to apply
     * @param indexFirstRecord The index of the first record
     * @param numberOfRecords The number of records to retrieve
     * @return The results of the query
     */
    fun getData(
        dataProvider: DataProvider,
        filter: Predicate? = null,
        orders: List<Order>? = null,
        indexFirstRecord: Int? = null,
        numberOfRecords: Int? = null
    ): List<Map<String, Any>> {

        return when (dataProvider) {
            is DataProviderSQL    -> sqlDataReader.getData(dataProvider, filter, orders)
            is DataProviderGSheet -> throw BadParameterException(E.service.access.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Add a new entry in the database
     *
     * @param dataSource The data source to write to
     * @param data The data to add
     * @return If the insertion was successful or not
     */
    fun insertData(
        dataSource: DataSource,
        data: Map<String, Any>
    ): Int {

        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)

        validateLookupData(dataProvider, listOf(data))

        return when (dataProvider) {
            is DataProviderSQL    -> sqlDataWriter.insertData(dataProvider, data)
            is DataProviderGSheet -> throw BadParameterException(E.service.access.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Add multiple new entries in the database
     *
     * @param dataSource The data source to write to
     * @param data The data to add
     * @return The number of inserted object
     */
    fun massInsertData(
        dataSource: DataSource,
        data: List<Map<String, Any>>
    ): Int {

        val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)

        validateLookupData(dataProvider, data)

        return when (dataProvider) {
            is DataProviderSQL    -> sqlDataWriter.massInsertData(dataProvider, data)
            is DataProviderGSheet -> throw BadParameterException(E.service.access.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Update existing entries in the database
     *
     * @param dataSource The data source to write to
     * @param filter The predicate to apply
     * @param data The data to add
     * @return The number of entries modified
     */
    fun updateData(
        dataSource: DataSource,
        filter: Predicate,
        data: Map<String, Any?>
    ): Int {

        return updateData(
            dataProviderService.getDataProviderById(dataSource.dataProviderId),
            filter,
            data)
    }

    /**
     * Update existing entries in the database
     *
     * @param dataProvider The data provider to write to
     * @param filter The predicate to apply
     * @param data The data to add
     * @return The number of entries modified
     */
    fun updateData(
        dataProvider: DataProvider,
        filter: Predicate,
        data: Map<String, Any?>,
        longTransaction: LongTransaction? = null
    ): Int {

        validateLookupData(dataProvider, listOf(data))

        return when (dataProvider) {
            is DataProviderSQL    -> sqlDataWriter.updateData(dataProvider, filter, data, (longTransaction as? LongTransactionSQL)?.connection)
            is DataProviderGSheet -> throw BadParameterException(E.service.access.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Delete existing entries in the database
     *
     * @param dataSource The data source to write to
     * @param filter The predicate to apply
     * @return The number of entries deleted
     */
    fun deleteData(
        dataSource: DataSource,
        filter: Predicate
    ): Int {

        return when (val dataProvider = dataProviderService.getDataProviderById(dataSource.dataProviderId)) {
            is DataProviderSQL    -> sqlDataWriter.deleteData(dataProvider, filter)
            is DataProviderGSheet -> throw BadParameterException(E.service.access.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Get the columns from a DataSource
     *
     * @param dataSource The data source for which to retrieve the columns
     * @return a list of columns
     */
    fun getColumns(dataSource: DataSource): List<Column> {

        return dataProviderService.getDataProviderById(dataSource.dataProviderId).columns
    }

    /**
     * Create a new LongTransaction for the given [DataProvider]
     *
     * @param dataProvider The data provider
     */
    fun createLongTransaction(dataProvider: DataProvider): LongTransaction {

        return when (val schema = schemaService.getSchemaById(dataProvider.schemaId)) {
            is SchemaSQL    -> {
                val connection = sqlConnectionCache.getConnection(schema)
                connection.autoCommit = false
                LongTransactionSQL(connection)
            }
            is SchemaGSheet -> throw BadParameterException(E.service.access.schemaNotSupported, schema::class)
        }
    }


    /**
     * Ensure that the data are valid for given lookup values
     *
     * @param dataProvider The data provider where to insert / update data
     * @param data the data to insert
     *
     * @return a new set of data
     */
    private fun validateLookupData(dataProvider: DataProvider, data: List<Map<String, Any?>>) {

        val lookupColumnByName = getLookupColumnsByName(dataProvider)

        if (lookupColumnByName.isNotEmpty()) {
            val lookupValuesByName = retrieveAllLookups(dataProvider)
            data
                .forEach { row ->
                    row.entries
                        .forEach { entry ->
                            validateLookupDataEntry(lookupColumnByName, lookupValuesByName, entry.key, entry.value)
                        }
                }
        }
    }

    /**
     * Validate a single value
     *
     * @param lookupColumnByName The lookup columns by name
     * @param lookupValuesByName The lookup values by name
     * @param name the name of the value
     * @param value the value of the value (could be null or not even a lookup)
     *
     * @return an update Pair with name and value
     */
    private fun validateLookupDataEntry(
        lookupColumnByName: Map<String, ColumnLookup>,
        lookupValuesByName: Map<String, Map<String, String>>,
        name: String,
        value: Any?
    ) {

        // If something to test
        value?.let { valueToTest ->
            // If a lookup
            lookupColumnByName[name]?.let { column ->
                // If the value is not a list, not good
                (valueToTest as? List<*>)
                    ?.let { values ->
                        // If more lookup than the tolerated
                        if (values.size > column.maximumNumberOfLookups) {
                            throw BadParameterException(E.service.model.lookup.tooManyLookupsForColumn, column.name, column.maximumNumberOfLookups, values.size, values.joinToString(","))
                        }

                        // Ensure that the lookup code exist
                        lookupValuesByName[name]
                            ?.let { lookupValues ->
                                values.forEach { lookupKeyReceived ->
                                    lookupValues[lookupKeyReceived.toString()]
                                        ?: throw BadParameterException(E.service.model.lookup.badLookupForColumn, column.name, lookupKeyReceived)
                                }
                            }
                    }
                    ?: throw BadParameterException(E.service.model.lookup.badValueDataTypeForColumn, name, value::class)
            }
        }
    }

    /**
     * Return the lookups columns of a DataProvider by name
     *
     * @param  dataProvider The data provider
     * @return a map of the lookups column by name
     */
    private fun getLookupColumnsByName(dataProvider: DataProvider): Map<String, ColumnLookup> {
        return dataProvider.columns
            .mapNotNull {
                (it as? ColumnLookup)
                    ?.let { lookupColumn ->
                        lookupColumn.name to lookupColumn
                    }
            }
            .toMap()
    }

    /**
     * retrieve all lookups for a single data source
     *
     * @param dataProvider The data provider source for which to retrieve the lookups
     *
     * @return a map having as key the name of the column and as value a map for the lookup
     */
    fun retrieveAllLookups(dataProvider: DataProvider): Map<String, Map<String, String>> {

        // Find the columns having lookup
        val columns = dataProvider.columns.mapNotNull { it as? ColumnLookup }

        // Define the data to load (multiple column can refer to the same lookup)
        val lookupTripleIds = columns
            .map { Triple(it.dataSourceId, it.keyColumnName, it.valueColumnName) }
            .distinct()

        // Load the data by data source (as the same data source can hold multiple columns)
        val lookupTripleIdsToData = lookupTripleIds
            .groupBy { it.first }
            .flatMap { (lookupDataSourceId, lookupToLoad) ->

                // Get the lookup dataSource
                val lookupDataSource = dataSourceService.getDataSourceById(lookupDataSourceId)

                // Get the lookup data
                val data = getData(lookupDataSource)

                // Return the relations from lookupToLoad (the triple) and the its data data
                lookupToLoad
                    .map {
                        val keyColumnName = it.second
                        val valueColumnName = it.third

                        // Get the lookup key and values
                        val keyValue = data
                            .mapNotNull { lookupKeyValue ->
                                lookupKeyValue[keyColumnName]?.let { key ->
                                    lookupKeyValue[valueColumnName]?.let { value ->
                                        key.toString() to value.toString()
                                    }
                                }
                            }
                            .toMap()

                        it to keyValue
                    }
            }
            .toMap()

        // Map the column names to the data
        return columns
            .mapNotNull { column ->

                val columnLookupTripleId = Triple(column.dataSourceId, column.keyColumnName, column.valueColumnName)
                val data = lookupTripleIdsToData[columnLookupTripleId]

                data?.let {
                    column.name to data
                }
            }
            .toMap()
    }
}