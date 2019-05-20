package net.wuillemin.jds.dataserver.supplier.sql.service

import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.StorageDetail
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import org.springframework.stereotype.Service

/**
 * A service for getting the model data from SQL: table, columns, etc.
 *
 * @param sqlConnectionCache The connections
 */
@Service
class SQLModelWriter(private val sqlConnectionCache: SQLConnectionCache) {

    /**
     * Create a table from a given set of columns
     *
     * @param schema The schema to which retrieve the tables
     * @param tableName The name of the table to create
     * @param columns The definition of the table columns
     */
    fun createTable(
        schema: SchemaSQL,
        tableName: String,
        columns: List<ColumnAttribute>
    ) {

        // Convert the columns
        val columnsStr = columns.map { (columnName, dataType, size, containerDetails) ->
            val base = when (dataType) {
                DataType.STRING    -> "$columnName varchar($size)"
                DataType.LONG      -> if (containerDetails.autoIncrement) {
                    "$columnName serial "
                }
                else {
                    "$columnName bigint "
                }
                DataType.DOUBLE    -> "$columnName double precision"
                DataType.BOOLEAN   -> "$columnName boolean"
                DataType.DATE      -> "$columnName date"
                DataType.TIME      -> "$columnName time without time zone"
                DataType.DATE_TIME -> "$columnName timestamp with time zone"
                else               -> "$columnName varchar($size)"
            }
            base + convertContainerDetail(containerDetails)
        }

        val query = "CREATE TABLE $tableName (${columnsStr.joinToString(",")})"

        // Get the connection
        sqlConnectionCache.getConnection(schema).use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.executeUpdate()
            }
        }
    }

    /**
     * Convert a container detail in its SQL counterpart for creating table
     * @param storageDetail the details of the container to convert
     * @return the SQL counterpart
     */
    private fun convertContainerDetail(storageDetail: StorageDetail?): String {

        return storageDetail
            ?.let { detail ->
                val pk = if (detail.primaryKey) {
                    " PRIMARY KEY "
                }
                else {
                    ""
                }
                val nullable = if (detail.nullable) {
                    ""
                }
                else {
                    " NOT NULL"
                }
                pk + nullable
            }
            ?: run {
                ""
            }
    }

}
