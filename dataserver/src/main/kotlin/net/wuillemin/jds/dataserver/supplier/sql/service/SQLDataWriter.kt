package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLPredicateConverter
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.sql.Connection

/**
 * A service for writing data into a SQL database
 *
 * @param schemaService The service for managing [Schema]
 * @param sqlConnectionCache The connections
 * @param sqlPredicateConverter The predicate converter
 * @param objectMapper The object mapper
 * @param logger The logger
 */
@Service
class SQLDataWriter(
    /**
     * The service for accessing schema
     */
    private val schemaService: SchemaService,
    /**
     * The connections
     */
    private val sqlPredicateConverter: SQLPredicateConverter,
    /**
     * The connections
     */
    private val sqlConnectionCache: SQLConnectionCache,
    /**
     * The object mapper
     */
    private val objectMapper: ObjectMapper,
    /**
     * The logger
     */
    private val logger: Logger
) {

    /**
     * Insert new data in table
     *
     * @param dataProvider The data provider from which to retrieve the data
     * @param dataToInsert The data to insert
     * @return the number of entries written
     */
    fun insertData(
        dataProvider: DataProviderSQL,
        dataToInsert: Map<String, Any>
    ): Int {

        logger.debug("insertData: try to insert data in SQL dataProvider ${dataProvider.id}-${dataProvider.name}")

        if (!dataProvider.editable) {
            throw BadParameterException(E.supplier.sql.service.dataProviderNotEditable, dataProvider)
        }

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)

        }

        val columnsByName = dataProvider.columns.map { it.name to it }.toMap()

        // Avoid bad attributes
        dataToInsert.keys.forEach { key ->
            columnsByName[key]
                ?: throw ConstraintException(E.supplier.sql.service.attributeDoesNotExist, key)
        }

        // Prepare the data
        val data = prepareMandatoryData(dataProvider, listOf(dataToInsert))[0]

        // Find the insertion targets for this data provider
        val possibleInsertionTargets = getWritableColumnsByTable(dataProvider)

        val queries = possibleInsertionTargets
            .mapNotNull { possibleInsertionTarget ->
                val tableName = possibleInsertionTarget.key
                val columns = possibleInsertionTarget.value

                // All the PK (non auto increment) and non nullable values must be covered
                val mandatoryAttributes = columns.filter { (it.second.primaryKey || !it.second.nullable) && !it.second.autoIncrement }.map { it.first }
                val missingMandatoryValues = mandatoryAttributes.filter { !data.containsKey(it) }

                if (missingMandatoryValues.isEmpty()) {

                    // Don't insert the auto increment columns
                    val autoIncrementColumnNames = columns.asSequence().filter { it.second.autoIncrement }.map { it.first }.toList()
                    val overwritingAutoIncrementColumnNames = data.keys.filter { autoIncrementColumnNames.contains(it) }

                    if (overwritingAutoIncrementColumnNames.isEmpty()) {

                        // For all columns to be filled that are not auto increment, if there is a value make a Pair
                        val insertItems = columns
                            .mapNotNull { (first, second) ->
                                data[first]?.let { Pair(second.writeAttributeName, it) }
                            }

                        // Prepare the SQL
                        val columnsClause = insertItems.joinToString(separator = ", ") { it.first }
                        val valueClause = insertItems.joinToString(separator = ", ") { "(?)" }
                        val query = "INSERT INTO $tableName($columnsClause) VALUES ($valueClause)"

                        // Return a pair with the query and the objects to be mapped
                        Pair(query, insertItems.map { it.second })
                    }
                    else {
                        // If the data are going to overwrite auto increment columns, then no insertion is possible in this target. Simply skip
                        logger.debug("insertData: Unable to insert new value in table $tableName due to given values for auto increment columns: $overwritingAutoIncrementColumnNames")
                        null
                    }
                }
                else {
                    // If the data are missing mandatory values, then no insertion is possible in this target. Simply skip
                    logger.warn("insertData: Unable to insert new value in table $tableName due to missing attributes: $missingMandatoryValues")
                    null
                }
            }

        // With all the queries prepared, execute them
        return sqlConnectionCache.getConnection(schema).use { connection ->

            // Don't auto commit as we need everything to succeed
            connection.autoCommit = false

            val individualCreation = queries.map { (sql, values) ->
                connection.prepareStatement(sql).use { statement ->
                    values.forEachIndexed { index, value ->
                        statement.setObject(index + 1, value)
                    }
                    statement.executeUpdate()
                }
            }

            // Commit the whole insertion
            connection.commit()

            individualCreation.sum()
        }
    }

    /**
     * Mass insert new dataToInsert in table
     *
     * @param dataProvider The dataToInsert provider from which to retrieve the dataToInsert
     * @param dataToInsert The list of dataToInsert to insert
     * @return the number of updated records
     */
    fun massInsertData(
        dataProvider: DataProviderSQL,
        dataToInsert: List<Map<String, Any>>
    ): Int {

        logger.debug("massInsertData: try to mass insert in SQL dataProvider ${dataProvider.getLoggingId()}")

        if (!dataProvider.editable) {
            throw BadParameterException(E.supplier.sql.service.dataProviderNotEditable, dataProvider)
        }

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)

        }

        // Prepare the values (lookups to strings etc.)
        val data = prepareMandatoryData(dataProvider, dataToInsert)

        // Group by queries having the same keys, each one can generate its own queries
        val dataByKeys: Map<Set<String>, List<Map<String, Any>>> = data.groupBy { it.keys }

        // Avoid bad attributes
        val columnsByName = dataProvider.columns.map { it.name to it }.toMap()
        dataByKeys.keys.forEach { keys ->
            keys.forEach { key ->
                columnsByName[key]
                    ?: throw ConstraintException(E.supplier.sql.service.attributeDoesNotExist, key)

            }
        }

        // Find the insertion targets for this data provider
        val possibleInsertionTargets = getWritableColumnsByTable(dataProvider)

        val allQueryAndAllValues = dataByKeys.flatMap { dataByKey ->

            val keys = dataByKey.key
            val dataForKeys = dataByKey.value

            possibleInsertionTargets
                .mapNotNull { possibleInsertionTarget ->

                    val tableName = possibleInsertionTarget.key
                    val columns = possibleInsertionTarget.value

                    // All the PK (non auto increment) and non nullable values must be covered
                    val mandatoryAttributes = columns.filter { (it.second.primaryKey || !it.second.nullable) && !it.second.autoIncrement }.map { it.first }
                    val missingMandatoryValues = mandatoryAttributes.filter { !keys.contains(it) }

                    if (missingMandatoryValues.isEmpty()) {

                        // Don't insert the auto increment columns
                        val autoIncrementColumnNames = columns.asSequence().filter { it.second.autoIncrement }.map { it.first }.toList()
                        val overwritingAutoIncrementColumnNames = keys.filter { autoIncrementColumnNames.contains(it) }

                        if (overwritingAutoIncrementColumnNames.isEmpty()) {

                            val containerInformationByColumnName = columns.toMap()

                            // There may be way more keys than what can be inserted, so just keep the ones valid for the insertion target
                            val usableKeysAndContainer = keys
                                .mapNotNull { key ->
                                    containerInformationByColumnName[key]?.let { Pair(key, it) }
                                }
                                .filter { !it.second.autoIncrement }

                            // Prepare the SQL
                            val columnsClause = usableKeysAndContainer.joinToString(separator = ", ") { it.second.writeAttributeName }
                            val valueClause = usableKeysAndContainer.joinToString(separator = ", ") { "(?)" }
                            val query = "INSERT INTO $tableName($columnsClause) VALUES ($valueClause)"

                            // Filter the dataToInsert
                            val filteredData = dataForKeys.map { singleObject ->
                                usableKeysAndContainer.map { (usableKey, _) ->
                                    // The value should never be null as the values given to the function (that can not be null) were used
                                    // to build the singleObject map.
                                    singleObject.getValue(usableKey)
                                }.toList()
                            }

                            Pair(query, filteredData)
                        }
                        else {
                            // If the dataToInsert are going to overwrite auto increment columns, then no insertion is possible in this target. Simply skip
                            logger.debug("massInsertData: Unable to insert new value in table $tableName due to given values for auto increment columns: $overwritingAutoIncrementColumnNames")
                            null
                        }
                    }
                    else {
                        // If the dataToInsert are missing mandatory values, then no insertion is possible in this target. Simply skip
                        logger.debug("massInsertData: Unable to insert new values in table $tableName due to missing attributes: $missingMandatoryValues")
                        null
                    }
                }
        }

        // Get a connection to process the mass insert
        return sqlConnectionCache.getConnection(schema).use { connection ->

            // Don't auto commit as we need everything to succeed
            connection.autoCommit = false

            // For each query
            val queryResults = allQueryAndAllValues.map { (query, allValues) ->

                connection.prepareStatement(query).use { statement ->
                    // Add multiple objects
                    allValues.map { values ->
                        // Add a single objects
                        values.forEachIndexed { index, value ->
                            statement.setObject(index + 1, value)
                        }
                        statement.addBatch()
                    }
                    // Execute the batch
                    val individualResults = statement.executeBatch()
                    // Return the number of inserted objects
                    individualResults.sum()
                }
            }

            // Commit the whole insertion
            connection.commit()

            // Return the total number of object inserted
            queryResults.sum()
        }
    }

    /**
     * Update data in table
     *
     * @param dataProvider The data provider from which to retrieve the data
     * @param filter The predicate to select the values to be updated
     * @param dataToUpdate The data to update
     * @param connection The connection to the database to use if any, otherwise a new collection is created
     * @return the number of updated records
     */
    fun updateData(
        dataProvider: DataProviderSQL,
        filter: Predicate,
        dataToUpdate: Map<String, Any?>,
        connection: Connection? = null
    ): Int {

        logger.debug("updateData: try to update data in SQL dataProvider ${dataProvider.id}-${dataProvider.name}")

        if (!dataProvider.editable) {
            throw BadParameterException(E.supplier.sql.service.dataProviderNotEditable, dataProvider)
        }

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)

        }

        // Avoid bad attributes
        val columnsByName = dataProvider.columns.map { it.name to it }.toMap()
        dataToUpdate.keys.forEach { key ->
            columnsByName[key]
                ?: throw ConstraintException(E.supplier.sql.service.attributeDoesNotExist, key)

        }

        val data = prepareOptionalData(dataProvider, listOf(dataToUpdate))[0]

        // Find the insertion targets for this data provider
        val possibleUpdateTargets = getWritableColumnsByTable(dataProvider)

        val queries = possibleUpdateTargets
            .mapNotNull { possibleUpdateTarget ->

                val tableName = possibleUpdateTarget.key
                val columns = possibleUpdateTarget.value

                // No null value should be given for NonNullable columns
                val nonNullableColumnNames = columns.filter { !it.second.nullable }.map { it.first }
                val forbiddenToNullColumns = nonNullableColumnNames.filter { data.containsKey(it) && data[it] == null }

                if (forbiddenToNullColumns.isEmpty()) {

                    // Don't insert the auto increment columns
                    val autoIncrementColumnNames = columns.asSequence().filter { it.second.autoIncrement }.map { it.first }.toList()
                    val overwritingAutoIncrementColumnNames = data.keys.filter { autoIncrementColumnNames.contains(it) }

                    if (overwritingAutoIncrementColumnNames.isEmpty()) {
                        // We must be able to generate a predicate
                        try {
                            val columnNames = columns.map { it.first }.toSet()
                            val predicate = sqlPredicateConverter.generateWhereClause(dataProvider.columns.filter { columnNames.contains(it.name) }, filter)

                            val containerInformationByColumnName = columns.toMap()

                            // There may be way more keys than what can be inserted, so just keep the ones valid for the insertion target
                            val updateItems = data.keys
                                .mapNotNull { key ->
                                    containerInformationByColumnName[key]?.let { Pair(key, it) }
                                }
                                .filter { !it.second.autoIncrement }
                                .map {
                                    Pair(it.second.writeAttributeName, data[it.first])
                                }

                            // Prepare the SQL
                            val setClause = updateItems.joinToString(separator = ",") { "${it.first}=(?)" }
                            val query = "UPDATE $tableName SET $setClause WHERE ${predicate.sql}"

                            // The data to be mapped are the data to be set + the data coming from the predicate
                            val mappedValues = updateItems.map { it.second } + predicate.values

                            // Return a pair with the query and the objects to be mapped
                            Pair(query, mappedValues)

                        }
                        catch (e: Exception) {
                            // If the query does not apply to the target, then the target can not be updated. Skip it
                            logger.debug("updateData: Unable to update values in table $tableName due to not able to apply the given filter fot this table")
                            null
                        }
                    }
                    else {
                        // If the given values would overwrite auto-increment columns, then the target can not be updated. Skip it
                        logger.debug("updateData: Unable to update values in table $tableName due to given values for auto increment columns: $overwritingAutoIncrementColumnNames")
                        null
                    }
                }
                else {
                    // If the given values would overwrite set to null non-nullable columns, then the target can not be updated. Skip it
                    logger.debug("updateData: Unable to update values in table $tableName due to null given for non nullable columns: $forbiddenToNullColumns")
                    null
                }
            }

        // Build a function that will execute the update
        val updateExecute = fun(connection: Connection): Int {

            val individualCreation = queries.map { (sql, values) ->
                connection.prepareStatement(sql).use { statement ->
                    values.forEachIndexed { index, value ->
                        statement.setObject(index + 1, value)
                    }
                    statement.executeUpdate()
                }
            }

            return individualCreation.sum()
        }

        // Use the given connection or allocate one
        return connection
            ?.let {
                updateExecute(it)
            }
            ?: run {

                sqlConnectionCache.getConnection(schema).use { connection ->

                    // Don't auto commit as we need everything to succeed
                    connection.autoCommit = false

                    val result = updateExecute(connection)

                    // Commit the whole insertion
                    connection.commit()

                    result
                }
            }
    }

    /**
     * Delete data in table
     *
     * @param dataProvider The data provider from which to retrieve the data
     * @param filter The predicate to select the values to be updated
     * @return the number of updated records
     */
    fun deleteData(
        dataProvider: DataProviderSQL,
        filter: Predicate
    ): Int {

        logger.debug("deleteData: try to delete data in SQL dataProvider ${dataProvider.id}-${dataProvider.name}")

        if (!dataProvider.editable) {
            throw BadParameterException(E.supplier.sql.service.dataProviderNotEditable, dataProvider)
        }

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)
        }

        // Find the delete targets
        val possibleDeleteTargets = getWritableColumnsByTable(dataProvider)

        val queries = possibleDeleteTargets
            .mapNotNull { possibleDeleteTarget ->

                val tableName = possibleDeleteTarget.key
                val columns = possibleDeleteTarget.value

                try {
                    val columnNames = columns.map { it.first }.toSet()
                    val predicate = sqlPredicateConverter.generateWhereClause(dataProvider.columns.filter { columnNames.contains(it.name) }, filter)

                    Pair("DELETE FROM $tableName WHERE ${predicate.sql}", predicate.values)
                }
                catch (e: Exception) {
                    // If the query does not apply to the target, then the target can not be updated. Skip it
                    logger.debug("deleteData: Unable to update values in table $tableName due to not able to apply the given filter fot this table")
                    null
                }
            }

        // With all the queries prepared, execute them
        return sqlConnectionCache.getConnection(schema).use { connection ->

            // Don't auto commit as we need everything to succeed
            connection.autoCommit = false

            val individualDelete = queries.map { (sql, values) ->
                connection.prepareStatement(sql).use { statement ->
                    values.forEachIndexed { index, value ->
                        statement.setObject(index + 1, value)
                    }
                    statement.executeUpdate()
                }
            }

            // Commit the whole insertion
            connection.commit()

            individualDelete.sum()
        }
    }

    /**
     * Find the possible insertion/update target, so that each target is defined by its db table name and the list of the columns
     * coming from the query
     *
     * @param dataProvider The Data Provider
     * @return a list of insertion/update target as a Map with as key the name of the container (the table) and as value
     * the list of Pair of column name and container detail
     */
    private fun getWritableColumnsByTable(dataProvider: DataProviderSQL): Map<String, List<Pair<String, WritableStorage>>> {
        return dataProvider.columns
            .mapNotNull { column ->
                (column.storageDetail as? WritableStorage)?.let { writableContainer ->
                    Pair(column.name, writableContainer)
                }
            }
            .groupBy { pair -> pair.second.containerName }
    }

    /**
     * Prepare the data to be inserted (data is mandatory for insert).
     *
     * @param dataProvider The data provider where to insert / update data
     * @param data the data to insert
     *
     * @return a new set of data
     */
    private fun prepareMandatoryData(dataProvider: DataProviderSQL, data: List<Map<String, Any>>): List<Map<String, Any>> {

        val specialColumns = dataProvider.columns
            .filter { it.dataType == DataType.LIST_OF_STRINGS || it.dataType == DataType.BOOLEAN }
            .map { it.name to it }
            .toMap()

        return if (specialColumns.isEmpty()) {
            data
        }
        else {
            data
                .map { row ->
                    row.entries
                        .map { entry ->
                            prepareValue(specialColumns, entry.key, entry.value)
                        }
                        .toMap()
                }
        }
    }

    /**
     * Prepare the data to be updated (data is optional for insert).
     *
     * @param dataProvider The data provider where to insert / update data
     * @param data the data to insert
     *
     * @return a new set of data
     */
    private fun prepareOptionalData(dataProvider: DataProviderSQL, data: List<Map<String, Any?>>): List<Map<String, Any?>> {

        val specialColumns = dataProvider.columns
            .filter { it.dataType == DataType.LIST_OF_STRINGS || it.dataType == DataType.BOOLEAN }
            .map { it.name to it }
            .toMap()

        return if (specialColumns.isEmpty()) {
            data
        }
        else {
            data
                .map { row ->
                    row.entries
                        .map { entry ->
                            entry.value
                                ?.let {
                                    prepareValue(specialColumns, entry.key, it)
                                }
                                ?: run {
                                    entry.key to null
                                }
                        }
                        .toMap()
                }
        }
    }

    /**
     * Prepare a single value
     *
     * @param specialColumns The specialColumns column by name
     * @param name the name of the value
     * @param value the value of the value
     *
     * @return an update Pair with name and value
     */
    private fun prepareValue(specialColumns: Map<String, Column>, name: String, value: Any): Pair<String, Any> {

        return name to (
            specialColumns[name]
                ?.let { column ->
                    when (column.dataType) {

                        // The various cases supported for boolean
                        DataType.BOOLEAN -> {
                            when (value) {
                                is Int    -> value > 0
                                is Long   -> value > 0
                                is String -> value.trim().toBoolean()
                                else      -> value
                            }
                        }
                        // The list of strings is written as a string
                        DataType.LIST_OF_STRINGS
                                         -> {
                            (value as? List<*>)
                                ?.let { values ->
                                    val strings = values.map { it.toString() }
                                    objectMapper.writeValueAsString(strings)
                                }
                            // Should not happen as it was validated before
                                ?: throw BadParameterException(E.supplier.sql.service.lookupMustBeProvidedAsList, name, value::class)
                        }

                        // Other values are not affected
                        else             -> value
                    }
                }
                ?: run {
                    value
                })
    }
}