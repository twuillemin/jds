package net.wuillemin.jds.dataserver.dto

import net.wuillemin.jds.dataserver.entity.model.DataType

/**
 * Represents a column in a table
 *
 * @param name String
 * @param dataType The type of data
 * @param size The size of the column
 * @param nullable If the column is Nullable
 * @param primaryKey If the column is a Primary Key
 * @param autoIncrement If the column is autoIncremented
 */
data class TableColumnMeta(
    val name: String,
    val dataType: DataType,
    val size: Int,
    val nullable: Boolean,
    val primaryKey: Boolean,
    val autoIncrement: Boolean
)