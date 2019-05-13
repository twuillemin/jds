package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a filter
 *
 * @param name The name of the column
 */
data class ColumnName(
    val name: String
) : RequestElement