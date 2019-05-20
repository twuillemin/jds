package net.wuillemin.jds.dataserver.entity.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.wuillemin.jds.common.entity.Loggable

/**
 * The base DataProvider definition
 *
 * @param id The id of the DataProvider
 * @param schemaId The id of the Schema
 * @param name The name of the DataProvider
 * @param columns The columns of the DataProvider
 * @param editable If the DataProvider is editable (create/update)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DataProviderSQL::class, name = "SQL"),
    JsonSubTypes.Type(value = DataProviderGSheet::class, name = "GSheet"))
sealed class DataProvider(
    open val id: Long?,
    open val name: String,
    open val schemaId: Long,
    open val columns: List<Column>,
    open val editable: Boolean
) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.name} [id: ${this.id}]'"
    }
}

/**
 * Represents a SQL data provider
 *
 * @param query The SQL query executed by the data provider
 */
data class DataProviderSQL(
    // Inherited properties
    override val id: Long?,
    override val name: String,
    override val schemaId: Long,
    override val columns: List<Column>,
    override val editable: Boolean,
    // Specific properties
    val query: String
) : DataProvider(
    id,
    name,
    schemaId,
    columns,
    editable)

/**
 * Represents a GSheet data provider
 *
 * @param sheetName The name of the Sheet in the workbook
 */
data class DataProviderGSheet(
    // Inherited properties
    override val id: Long?,
    override val name: String,
    override val schemaId: Long,
    override val columns: List<Column>,
    override val editable: Boolean,
    // Specific properties
    val sheetName: String
) : DataProvider(
    id,
    name,
    schemaId,
    columns,
    editable)

