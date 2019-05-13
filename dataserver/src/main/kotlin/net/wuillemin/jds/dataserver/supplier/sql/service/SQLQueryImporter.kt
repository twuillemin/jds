package net.wuillemin.jds.dataserver.supplier.sql.service

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.dto.TableColumnMeta
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.StorageDetail
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLColumnDetail
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLReadOnlyColumn
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLWritableColumn
import org.springframework.stereotype.Service

/**
 * Service class for importing SQL
 *
 * @param sqlHelper The SQL helper
 * @param sqlModelReader The service for reading the model from the database
 */
@Service
class SQLQueryImporter(
    private val sqlHelper: SQLHelper,
    private val sqlModelReader: SQLModelReader) {

    /**
     * Build a [DataProvider] from query but does not persist it
     *
     * @param schema The server to which retrieve the tables
     * @param name The name of the DataProvider
     * @param query The query to be executed
     * @return a DataProviderSQL object ready to be persisted
     */
    fun buildDataProviderFromQuery(
        schema: SchemaSQL,
        name: String,
        query: String): DataProviderSQL {

        return schema.id

            ?.let { schemaId ->

                // Get the tables and their alias
                val sqlTableByAlias = sqlHelper.getTableByAlias(query)

                // Get the database definition of the table involved in the query
                val allDatabaseTableNames = sqlModelReader.getTables(schema)
                val dbTables = sqlTableByAlias.values
                    .mapNotNull { sqlTableName -> allDatabaseTableNames.find { it.toLowerCase() == sqlTableName.toLowerCase() } }
                    .map { dataBaseTableName -> Pair(dataBaseTableName, sqlModelReader.getColumnsFromTable(schema, dataBaseTableName)) }
                    .toMap()

                // Get the columns from the query
                val columns = getColumnsForQuery(schema, query, sqlTableByAlias, dbTables)

                // Check if table allow editing
                val editable = getEditable(columns, dbTables)

                return DataProviderSQL(
                    null,
                    schemaId,
                    name,
                    columns,
                    editable,
                    query)
            }

            ?: throw BadParameterException(E.supplier.sql.service.importerServerNotPersisted)
    }

    /**
     * Retrieve the columns for a query
     *
     * @param schema The schema
     * @param query The query
     * @param sqlTableByAlias The list of tables from the query
     * @param dbTables The list of tables from the query, but retrieved from the schema
     * @return the list of columns returned by the query
     */
    private fun getColumnsForQuery(
        schema: SchemaSQL,
        query: String,
        sqlTableByAlias: Map<String, String>,
        dbTables: Map<String, List<TableColumnMeta>>): List<ColumnAttribute> {

        // Get the columns of the query from a 1 line preview
        val queryColumns = sqlModelReader.getPreviewFromQuery(schema, query, 1).columns

        // Ensure that there are no ambiguous name, as ambiguous name (even if supported for generating the DataProvider)
        // are not readable
        val duplicatedColumns = queryColumns
            .groupBy { it.name }
            .filter { entry -> entry.value.size > 1 }
            .map { entry -> entry.key }

        if (duplicatedColumns.isNotEmpty()) {
            throw BadParameterException(E.supplier.sql.service.importerDuplicatedColumns, duplicatedColumns)
        }

        // Get the columns and their alias
        val sqlColumnByAlias = sqlHelper.getColumnByAlias(query)

        // Get the definition of all columns
        return queryColumns.map {

            ColumnAttribute(
                findGivenSQLColumnNameFromQueryResult(sqlColumnByAlias, it)
                    ?.first
                    ?: run {
                        it.name
                    },
                it.dataType,
                it.size,
                getDataBaseContainerInformation(
                    sqlColumnByAlias,
                    sqlTableByAlias,
                    dbTables,
                    it))
        }
    }

    /**
     * Try to find the column resulting from the execution of the query (queryColumnToSearch) in the columns coming
     * from the parsing of the query
     *
     * @param sqlColumnByAlias The columns coming from the parsing of the query
     * @param queryColumnToSearch The column coming from the query
     * @return if found a pair having the name of the column and the details of the column
     */
    private fun findGivenSQLColumnNameFromQueryResult(
        sqlColumnByAlias: Map<String, SQLColumnDetail>,
        queryColumnToSearch: TableColumnMeta): Pair<String, SQLColumnDetail>? {

        // Normally, the query should return a column having the same name as the sql. For example, if the sql is
        // SELECT UPPER(t.id) FROM myTable t, the query column would probably be UPPER(t.id) (case insensitive). Same
        // thing is there is an alias SELECT t.id as ABC FROM myTable t, the query will have a column named ABC. Or even
        // simpled SELECT id from myTable will return a column id. In this case, the column name is matching the alias
        // computed by reading the SQL
        return sqlColumnByAlias.entries
            .firstOrNull { it.key.equals(queryColumnToSearch.name, ignoreCase = true) }
            ?.let {
                Pair(it.key, it.value)
            }
        // However, if the query is trivial and non ambiguous and non aliased, the column name of the query may be
        // simplified. For example, if the sql is SELECT t.id FROM table t, the query column may just named id
        // (case insensitive). As all the simple cases are covered above, and as the query is non ambiguous (otherwise
        // it would have fail) we can only search for "."+name of query column
            ?: run {
                sqlColumnByAlias.entries
                    .firstOrNull { it.key.endsWith("." + queryColumnToSearch.name, ignoreCase = true) }
                    ?.let {
                        Pair(it.key, it.value)
                    }
            }
    }

    /**
     * Match a column coming from the query with a column coming from the database
     */
    private fun getDataBaseContainerInformation(
        sqlColumnByAlias: Map<String, SQLColumnDetail>,
        sqlTableByAlias: Map<String, String>,
        dbTables: Map<String, List<TableColumnMeta>>,
        queryColumnToSearch: TableColumnMeta): StorageDetail {

        val writableContainer = findGivenSQLColumnNameFromQueryResult(sqlColumnByAlias, queryColumnToSearch)
            ?.let { sqlColumn ->

                val aliasName = sqlColumn.first
                val sqlDetail = sqlColumn.second

                when (sqlDetail) {

                    //
                    // The column is an writable column
                    //

                    is SQLWritableColumn -> {

                        // Extract the relevant parts
                        val splitted = sqlDetail.writeExpression.split('.')
                        val tableNameOrAlias = splitted[0]
                        val realColumnName = splitted[1]

                        // If the column start with . (no table alias) it is supposed to be non ambiguous
                        val tableAndColumnInDb = if (tableNameOrAlias.isBlank()) {
                            // Look in the columns of db tables of the query for the first one having a column with this name
                            // There should be only one as the name is non ambiguous
                            dbTables.entries
                                .mapNotNull { entry ->
                                    entry.value
                                        .find { (name) -> name.toLowerCase() == realColumnName.toLowerCase() }
                                        ?.let { Pair(entry.key, it) }
                                }
                                .firstOrNull()
                        }
                        else {
                            // sqlTableByAlias is giving (alias or real name) -> real name
                            sqlTableByAlias[tableNameOrAlias]?.let { realTableName ->
                                // Find the table if possible
                                dbTables
                                    .filter { it.key.toLowerCase() == realTableName.toLowerCase() }
                                    .toList()
                                    .firstOrNull()
                                    // With the table found, look for the column
                                    ?.let { (tableName, columns) ->
                                        columns
                                            .firstOrNull { (name) -> name.toLowerCase() == realColumnName.toLowerCase() }
                                            ?.let { Pair(tableName, it) }
                                    }
                            }
                        }

                        // If a valid table and column is found in the db the container is writable
                        tableAndColumnInDb
                            ?.let {
                                val tableName = it.first
                                val dbColumnMeta = it.second
                                WritableStorage(
                                    tableName,
                                    queryColumnToSearch.name,
                                    dbColumnMeta.name,
                                    dbColumnMeta.nullable,
                                    dbColumnMeta.primaryKey,
                                    dbColumnMeta.autoIncrement)
                            }
                    }

                    //
                    // The column is non writable column, even if properly aliased, don't use it
                    //
                    is SQLReadOnlyColumn -> null
                }
            }

        //
        // The column returned by the query is not found in the column from the SQL, this may happen
        // for example when doing a select *
        //
            ?: run {

                // Look in the columns of db tables of the query for all having a column. There may be zero
                // or one. By SQL norm, a query cannot have two columns with the same name. Some database support it
                // (such as H2) but this is not not standard
                val tablesAndColumnsInDb = dbTables.entries
                    .mapNotNull { entry ->
                        entry.value
                            .find { possibleColumn ->
                                // Make the comparison without using primary key, as query columns are always given with primaryKey = false
                                (possibleColumn.name.toLowerCase() == queryColumnToSearch.name.toLowerCase()) &&
                                    (possibleColumn.nullable == queryColumnToSearch.nullable) &&
                                    (possibleColumn.autoIncrement == queryColumnToSearch.autoIncrement)
                            }
                            ?.let { Pair(entry.key, it) }
                    }
                    .firstOrNull()

                // If a table and column couple was found, use it for insertion
                tablesAndColumnsInDb
                    ?.let {
                        val tableName = it.first
                        val dbColumnMeta = it.second
                        WritableStorage(
                            tableName,
                            dbColumnMeta.name,
                            dbColumnMeta.name,
                            dbColumnMeta.nullable,
                            dbColumnMeta.primaryKey,
                            dbColumnMeta.autoIncrement)
                    }
            }

        return writableContainer
            ?: run {
                ReadOnlyStorage(
                    queryColumnToSearch.name,
                    queryColumnToSearch.nullable,
                    queryColumnToSearch.primaryKey,
                    queryColumnToSearch.autoIncrement)
            }
    }

    /**
     * Find if the columns of a query are enough to be able to insert / update data
     * @param columns the columns returned by the query
     * @param dbTables The list of tables from the query, but retrieved from the schema
     * @return if one or more table can be edited
     *
     */
    private fun getEditable(
        columns: List<ColumnAttribute>,
        dbTables: Map<String, List<TableColumnMeta>>): Boolean {

        // Group the insertion target, so that each target is defined by its db table name and the list of the columns
        // coming from the query
        val possibleInsertionTables = columns
            .mapNotNull { column -> column.storageDetail as? WritableStorage }
            .groupBy { it.containerName }

        // Only keep the insertion target that match all the primary keys of the table
        val validInsertionTables = possibleInsertionTables
            .filter { possibleInsertionTable ->

                // Find all the columns names generated by the query for the container
                val columnsGivenByQueryInContainer = possibleInsertionTable.value.map { column -> column.writeAttributeName }

                // Find the mandatory columns for inserting a record (should never go to empty list as the table
                // was found before in the construction of getDataBaseContainerInformation
                val columnsMandatoryInDbTable = dbTables[possibleInsertionTable.key]
                    ?.filter { column ->
                        // Mandatory columns are (primary key or not nullable) and not autoincrement
                        (column.primaryKey || !column.nullable) && !column.autoIncrement
                    }
                    ?: emptyList()

                // Ensure that the query is returning all the needed information
                val nonCoveredMandatoryColumns = columnsMandatoryInDbTable.filter {
                    !columnsGivenByQueryInContainer.contains(it.name)
                }

                // If there are some column left in the mandatory columns, then filter out this entry
                nonCoveredMandatoryColumns.isEmpty()
            }

        // Independently of the data given, if no valid insertion table is found, the DataProvider is not editable
        return validInsertionTables.isNotEmpty()
    }
}