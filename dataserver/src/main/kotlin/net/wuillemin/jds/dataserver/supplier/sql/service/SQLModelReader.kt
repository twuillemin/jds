package net.wuillemin.jds.dataserver.supplier.sql.service

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.dataserver.dto.Preview
import net.wuillemin.jds.dataserver.dto.TableColumnMeta
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.iterator
import net.wuillemin.jds.dataserver.supplier.sql.util.mapNotNull
import org.springframework.stereotype.Service
import java.sql.ResultSetMetaData
import java.sql.ResultSetMetaData.columnNullable
import java.sql.ResultSetMetaData.columnNullableUnknown
import java.time.ZoneId

/**
 * A service for getting the model data from SQL: table, columns, etc.
 *
 * @param jdbcHelper The JDBC Helper
 * @param schemaService The service for managing [Schema]
 * @param sqlConnectionCache The connections
 */
@Service
class SQLModelReader(
    /**
     * The JDBC helper
     */
    private val jdbcHelper: JDBCHelper,
    /**
     * The connections
     */
    private val sqlConnectionCache: SQLConnectionCache,
    /**
     * The service for managing the [Schema]
     */
    private val schemaService: SchemaService) {

    /**
     * Get the tables from a schema
     *
     * @param schema The schema to which retrieve the tables
     * @return the list of all tables in the schema
     */
    fun getTables(schema: SchemaSQL): List<String> {

        // Get the connection
        return sqlConnectionCache.getConnection(schema).use { connection ->
            // Get the meta data
            connection.metaData
                .getTables(null, schema.name, "%", arrayOf("TABLE", "VIEW"))
                .mapNotNull { resultSet ->
                    resultSet.getString("TABLE_NAME")
                }
        }
    }

    /**
     * Get the columns from a table.
     *
     * @param schema The [Schema] to which retrieve the tables
     * @param tableName The name of the table. The name of the table may or may not be case sensitive depending
     * on the server
     * @return a list of columns for the table
     */
    fun getColumnsFromTable(schema: SchemaSQL, tableName: String): List<TableColumnMeta> {

        // Get the connection
        return sqlConnectionCache.getConnection(schema).use { connection ->

            val primaryKeyNames = connection.metaData
                .getPrimaryKeys(null, null, tableName)
                .mapNotNull { it.getString("COLUMN_NAME") }
                .toSet()

            connection.metaData
                .getColumns(null, null, tableName, null)
                .mapNotNull { resultSet ->
                    resultSet.getString("COLUMN_NAME")?.let { columnName ->
                        TableColumnMeta(
                            columnName,
                            jdbcHelper.getDataTypeFromJdbcType(resultSet.getInt("DATA_TYPE")),
                            resultSet.getInt("COLUMN_SIZE"),
                            resultSet.getString("IS_NULLABLE")?.let { it == "YES" } ?: false,
                            primaryKeyNames.contains(columnName),
                            resultSet.getString("IS_AUTOINCREMENT")?.let { it == "YES" } ?: false)
                    }
                }
        }
    }

    /**
     * Get the first lines from a table
     *
     * @param schema The server to which retrieve the tables
     * @param tableName The name of the table
     * @return a Preview object
     */
    fun getPreview(schema: SchemaSQL, tableName: String): Preview {
        return getPreviewFromQuery(schema, "SELECT * FROM $tableName", 50)
    }

    /**
     * Get the first lines from a DataProviderSQL
     *
     * @param dataProvider The dataProvider to retrieve
     * @return a Preview object
     * @throws NotFoundException if the server referenced by the data provider does not exist
     * @throws BadParameterException if the server referenced by the data provider is not a Server SQL
     */
    fun getPreview(dataProvider: DataProviderSQL): Preview {

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)
        }

        return getPreviewFromQuery(schema, dataProvider.query, 50)
    }

    /**
     * Get the first lines from a table. Note that this function always returns the columns with
     * the attribute primaryKey set to false
     *
     * @param schema The [Schema] to which retrieve the tables
     * @param query The query to execute
     * @return a Preview object
     */
    fun getPreviewFromQuery(schema: SchemaSQL, query: String, previewSize: Int): Preview {

        // Get the connection
        return sqlConnectionCache
            .getConnection(schema)
            .use { connection ->
                connection
                    .createStatement()
                    .use { statement ->
                        statement.fetchSize = previewSize
                        statement
                            .executeQuery(query)
                            .let { resultSet ->
                                Preview(
                                    getColumnsForResultSet(resultSet.metaData),
                                    resultSet
                                        .iterator()
                                        .asSequence()
                                        .take(previewSize)
                                        .map { jdbcHelper.convertResultSet(it, ZoneId.systemDefault()) }
                                        .toList()
                                )
                            }
                    }
            }
    }

    /**
     * Get the columns from a query
     * @param metaData The metadata of the result set
     * @return a list of columns for the given result set
     */
    private fun getColumnsForResultSet(metaData: ResultSetMetaData): List<TableColumnMeta> {

        // Get the list of tables

        // JDBC columns start at 1 and range are inclusive
        return IntRange(1, metaData.columnCount)
            .mapNotNull { index ->
                metaData.getColumnLabel(index)?.let { Pair(index, it) }
            }
            .map { (index, columnName) ->
                TableColumnMeta(
                    columnName,
                    jdbcHelper.getDataTypeFromJdbcType(metaData.getColumnType(index)),
                    metaData.getColumnDisplaySize(index),
                    metaData.isNullable(index).let { it == columnNullable || it == columnNullableUnknown },
                    false,
                    metaData.isAutoIncrement(index))
            }
            .toList()
    }
}
