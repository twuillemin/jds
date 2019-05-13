package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents an in predicate
 *
 * @param column The name of the column
 * @param values The possible values
 */
data class In(
    val column: ColumnName,
    val values: List<Value>
) : Predicate