package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a Not In predicate
 *
 * @param column The name of the column
 * @param values The possible values
 */
data class NotIn(
    val column: ColumnName,
    val values: List<Value>
) : Predicate