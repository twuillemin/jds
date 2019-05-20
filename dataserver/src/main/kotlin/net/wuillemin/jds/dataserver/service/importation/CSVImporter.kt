package net.wuillemin.jds.dataserver.service.importation

import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.service.access.DataAccessService
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.ModelService
import org.apache.commons.csv.CSVFormat
import org.springframework.stereotype.Service

/**
 * Service for importing CSV files
 *
 * @param csvModelReader The service for reading CSV models
 * @param modelService The service for creating a table in a schema
 * @param dataProviderService The service for managing [DataProvider]
 * @param dataSourceService The service for managing [DataSource]
 * @param dataAccessService The service for importing data
 */
@Service
class CSVImporter(
    private val csvModelReader: CSVModelReader,
    private val modelService: ModelService,
    private val dataProviderService: DataProviderService,
    private val dataSourceService: DataSourceService,
    private val dataAccessService: DataAccessService
) {

    /**
     * Try to import auto-magically a CSV file
     *
     * @param schema The schema into which create the table
     * @param tableName The name of the table, that will be the name of the DataProvider and the name of the DataSource
     * @param csvContent The content of the CSV
     */
    fun autoImportCSV(schema: Schema, tableName: String, csvContent: String): DataSource {

        // Read the csv
        val columnByName = csvModelReader.guessCSVModel(csvContent.reader())
            .map { (name, dataType, nullable, longestString) ->
                name to ColumnAttribute(
                    name,
                    dataType,
                    longestString,
                    WritableStorage(
                        tableName,
                        name,
                        name,
                        nullable,
                        false,
                        false))
            }
            .toMap()
            .toMutableMap()

        // Ensure there is unique id column or add it
        columnByName["id"] = columnByName["id"]
            ?.let { column ->
                column.copy(
                    storageDetail =
                    when (column.storageDetail) {
                        is WritableStorage -> column.storageDetail.copy(primaryKey = true)
                        is ReadOnlyStorage -> column.storageDetail.copy(primaryKey = true)
                    })
            }
            ?: run {
                ColumnAttribute(
                    "id",
                    DataType.LONG,
                    8,
                    WritableStorage(
                        tableName,
                        "id",
                        "id",
                        false,
                        true,
                        true))
            }

        // Create the table
        modelService.createTable(schema, tableName, columnByName.values.toList())
        // Prepare a simple data provider
        val dataProviderDefinition = modelService.createDataProviderFromSQL(schema, tableName, "SELECT ${columnByName.keys.joinToString(",")} FROM $tableName")
        // Persist the data provider
        val dataProvider = dataProviderService.addDataProvider(dataProviderDefinition)
        // Persist a DataSource
        val dataSource = dataSourceService.addDataSource(
            DataSource(
                null,
                tableName,
                dataProvider.id!!,
                emptySet(),
                emptySet(),
                emptySet()))

        // Import the data
        importCSV(dataSource, csvContent)

        return dataSource
    }

    /**
     * Import CSV data into a data source
     *
     * @param dataSource The data source into which import the data
     * @param csvContent The csv data
     * @return the number of records saved
     */
    private fun importCSV(dataSource: DataSource, csvContent: String): Int {

        // Parse the CSV
        val records = CSVFormat.EXCEL
            .withFirstRecordAsHeader()
            .parse(csvContent.reader())

        // Get the name of the column
        val columnNames = records.headerMap.entries.map { it.key }

        // Read the data
        val data = records.map { record ->
            columnNames
                .mapNotNull { columnName ->
                    record[columnName]?.let { value ->
                        if (value.trim().isNotBlank()) {
                            columnName to value
                        }
                        else {
                            null
                        }
                    }
                }
                .toMap()
        }

        // Insert the data
        return dataAccessService.massInsertData(dataSource, data)
    }
}