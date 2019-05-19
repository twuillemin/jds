package net.wuillemin.jds.dataserver.dto.query

/**
 * The query for promoting a column as a lookup column
 *
 * @param columnName The name of the column to promote
 * @param maximumNumberOfLookups The maximum number of elements in the collection of lookup
 * @param dataSourceId The id of the DataSource having the lookup values
 * @param keyColumnName The name of the column holding the key
 * @param valueColumnName The name of the column holding the value
 */
data class PromoteColumnToLookupQuery(
    val columnName: String,
    val maximumNumberOfLookups: Int,
    val dataSourceId: Long,
    val keyColumnName: String,
    val valueColumnName: String
)