package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.entity.query.OrderDirection
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLOrderConverter
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLPredicateConverter
import net.wuillemin.jds.dataserver.supplier.sql.util.map
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId


/**
 * Class for retrieving data from SQL
 *
 * @param schemaService The service for managing [Schema]
 * @param jdbcHelper The JDBC helper
 * @param sqlHelper The SQL helper
 * @param sqlConnectionCache The connections
 * @param sqlPredicateConverter The predicate converter
 * @param sqlOrderConverter The order converter
 * @param objectMapper The object mapper
 * @param logger The logger
 */
@Service
class SQLDataReader(
    private val schemaService: SchemaService,
    private val jdbcHelper: JDBCHelper,
    private val sqlHelper: SQLHelper,
    private val sqlConnectionCache: SQLConnectionCache,
    private val sqlPredicateConverter: SQLPredicateConverter,
    private val sqlOrderConverter: SQLOrderConverter,
    private val objectMapper: ObjectMapper,
    private val logger: Logger
) {

    companion object {

        /**
         * The type of data for a list of string
         */
        var typeListOfString: TypeReference<List<String>> = object : TypeReference<List<String>>() {

        }
    }

    /**
     * Get the the data of a table
     *
     * @param dataProvider The data provider to retrieve
     * @param filter The predicate to apply
     * @param orders the order to apply
     * @param indexFirstRecord The index of the first record
     * @param numberOfRecords The number of records to retrieve
     * @return the data as a list of map
     */
    fun getData(
        dataProvider: DataProviderSQL,
        filter: Predicate? = null,
        orders: List<Order>? = null,
        indexFirstRecord: Int? = null,
        numberOfRecords: Int? = null
    ): List<Map<String, Any>> {

        logger.debug("getData(${dataProvider.getLoggingId()})")

        val schema = schemaService.getSchemaById(dataProvider.schemaId).let {
            it as? SchemaSQL
                ?: throw BadParameterException(E.supplier.sql.service.schemaIsNotSql, it)
        }

        // Convert the predicate and the order if any
        val convertedPredicate = sqlPredicateConverter.generateWhereClause(dataProvider.columns, filter)

        // If no order given, sort by primary key (if any)
        val ordersToUse = orders
            ?: dataProvider.columns
                .filter { it.storageDetail.primaryKey }
                .map { Order(it.name, OrderDirection.ASCENDING) }

        // Generate the order clause
        val convertedOrder = with(sqlOrderConverter.generateOrderClause(dataProvider.columns, ordersToUse)) {
            if (this.isNotBlank()) {
                " ORDER BY $this"
            }
            else {
                ""
            }
        }

        val offset = indexFirstRecord
            ?.let { " OFFSET $it" }
            ?: run { "" }

        val limit = numberOfRecords
            ?.let { " LIMIT $it" }
            ?: run { "" }

        // Make the full query
        val query = "SELECT * FROM (${sqlHelper.cleanSQL(dataProvider.query)}) AS inner_table WHERE ${convertedPredicate.sql} $convertedOrder $offset $limit"

        val rawResults = sqlConnectionCache.getConnection(schema).use { connection ->
            connection.prepareStatement(query).use { statement ->
                convertedPredicate.values.forEachIndexed { index, any ->
                    statement.setObject(index + 1, any)
                }
                statement
                    .executeQuery()
                    .map { jdbcHelper.convertResultSet(it, ZoneId.systemDefault()) }
                    .toList()
            }
        }

        // The name of the column coming out of the query may not be the name of the column of the data provider. For example
        // doing a select * will return the column name in upper case for some db
        val dataProviderColumnByQueryColumn = dataProvider.columns
            .map { column ->
                column.storageDetail.readAttributeName to column.name
            }
            .toMap()

        // Find the columns that are lookup
        val lookupColumnNames = dataProvider.columns.mapNotNull { (it as? ColumnLookup)?.name }.toSet()

        return rawResults
            .map { row ->
                row.entries
                    .map { entry ->
                        val columnName = dataProviderColumnByQueryColumn[entry.key] ?: entry.key
                        val value = if (lookupColumnNames.contains(entry.key)) {
                            objectMapper.readValue<List<String>>(entry.value.toString(), typeListOfString)
                        }
                        else {
                            entry.value
                        }
                        columnName to value
                    }
                    .toMap()
            }
    }
}