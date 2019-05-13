package net.wuillemin.jds.dataserver.dto.query

/**
 * The query for importing CSV
 *
 * @param schemaId The id of the schema
 * @param tableName The name of the table
 * @param dataBase64 The CSV data as Base64 Encoded String
 */
data class ImportCSVQuery(
    val schemaId: String,
    val tableName: String,
    val dataBase64: String
)