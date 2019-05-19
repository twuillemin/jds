package net.wuillemin.jds.dataserver.service.model

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.dto.Preview
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderGSheet
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaGSheet
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelWriter
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLQueryImporter
import org.springframework.stereotype.Service

/**
 * Service for retrieving and modifying the underlying model
 *
 * @param sqlModelReader The service for retrieving model from SQL
 * @param sqlModelWriter The service for writing SQL model
 * @param sqlQueryImporter The service for importing query from SQL
 * @param dataProviderService The service for managing [DataProvider]
 */
@Service
class ModelService(
    private val sqlModelReader: SQLModelReader,
    private val sqlModelWriter: SQLModelWriter,
    private val sqlQueryImporter: SQLQueryImporter,
    private val dataProviderService: DataProviderService
) {

    /**
     * Get the table in a schema
     *
     * @param schema The schema
     * @return the list of table in the schema
     */
    fun getTables(schema: Schema): List<String> {

        return when (schema) {
            is SchemaSQL    -> sqlModelReader.getTables(schema)
            is SchemaGSheet -> throw BadParameterException(E.service.model.preview.schemaNotSupported, schema::class)
        }
    }

    /**
     * Get the preview for a single table in a schema
     *
     * @param schema The schema
     * @param tableName The name of the table
     * @return the preview of the data
     */
    fun getPreview(schema: Schema, tableName: String): Preview {

        return when (schema) {
            is SchemaSQL    -> sqlModelReader.getPreview(schema, tableName)
            is SchemaGSheet -> throw BadParameterException(E.service.model.preview.schemaNotSupported, schema::class)
        }
    }

    /**
     * Get the preview for a dataProvider
     *
     * @param dataProvider The DataProviderSQL
     * @return the preview of the data
     */
    fun getPreview(dataProvider: DataProvider): Preview {

        return when (dataProvider) {
            is DataProviderSQL    -> sqlModelReader.getPreview(dataProvider)
            is DataProviderGSheet -> throw BadParameterException(E.service.model.preview.dataProviderNotSupported, dataProvider::class)
        }
    }

    /**
     * Create a data container (a table, a sheet, etc.) in the current schema
     *
     * @param schema The schema to which retrieve the tables
     * @param tableName The name of the table to create
     * @param columns The definition of the table columns
     */
    fun createTable(
        schema: Schema,
        tableName: String,
        columns: List<ColumnAttribute>
    ) {
        return when (schema) {
            is SchemaSQL    -> sqlModelWriter.createTable(schema, tableName, columns)
            is SchemaGSheet -> throw BadParameterException(E.service.model.preview.schemaNotSupported, schema::class)
        }
    }

    /**
     * Create a new DataProvider From SQL
     *
     * @param schema The schema
     * @param query The query
     * @return a new DataProvider
     */
    fun createDataProviderFromSQL(
        schema: Schema,
        name: String,
        query: String
    ): DataProvider {

        val schemaSQL = schema as? SchemaSQL
            ?: throw BadParameterException(E.service.import.dataProviderNotSupported, schema::class)

        // Get the unsaved DataProvider
        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            name,
            query        )

        // Create a new DataProvider
        return dataProviderService.addDataProvider(dataProvider)
    }
}