package net.wuillemin.jds.dataserver.dto.query

/**
 * Object with the parameters for importing a DataProviderFrom a SQL query
 *
 * @param schemaId The id of the server
 * @param name The query to execute
 * @param query The query to execute
 */
data class ImportSQLQuery(
    val schemaId: Long,
    val name: String,
    val query: String
)