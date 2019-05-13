package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a contains predicate
 *
 * @param column The name of the column
 * @param value The value of the column
 * @param caseSensitive If the predicate is case sensitive
 */
data class Contains(
    val column: ColumnName,
    val value: Value,
    val caseSensitive: Boolean = false
) : Predicate