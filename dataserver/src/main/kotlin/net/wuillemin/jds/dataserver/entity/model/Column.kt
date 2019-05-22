package net.wuillemin.jds.dataserver.entity.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Represents a column in a table
 *
 * @param name The name of the column
 * @param dataType The type of data
 * @param size The size of the column
 * @param storageDetail The possible insertion information for the column
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ColumnAttribute::class, name = "attribute"),
    JsonSubTypes.Type(value = ColumnLookup::class, name = "lookup")
)
sealed class Column(
    open val name: String,
    open val dataType: DataType,
    open val size: Int,
    open val storageDetail: StorageDetail
)

/**
 * Represents a column holding a simple attribute in the database.
 */
data class ColumnAttribute(
    // Inherited properties
    override val name: String,
    override val dataType: DataType,
    override val size: Int,
    override val storageDetail: StorageDetail
) : Column(name, dataType, size, storageDetail)


/**
 * Represents a column which is to be treated as lookup
 *
 * @param maximumNumberOfLookups The maximum number of elements in the collection of lookup
 * @param dataSourceId The id of the DataSource having the lookup values
 * @param keyColumnName The name of the column holding the key
 * @param valueColumnName The name of the column holding the value
 */
data class ColumnLookup(
    // Inherited properties
    override val name: String,
    override val dataType: DataType,
    override val size: Int,
    override val storageDetail: StorageDetail,
    // Specific properties
    val maximumNumberOfLookups: Int,
    val dataSourceId: Long,
    val keyColumnName: String,
    val valueColumnName: String
) : Column(name, dataType, size, storageDetail)

