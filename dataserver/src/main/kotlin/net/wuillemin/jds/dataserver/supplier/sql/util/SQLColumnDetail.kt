package net.wuillemin.jds.dataserver.supplier.sql.util

/**
 * The column coming from a SELECT statement in the query
 */
sealed class SQLColumnDetail

/**
 * The column represents a writable attribute (for example a direct column name)
 * @param readExpression The expression for reading the column
 * @param writeExpression The expression for writing the column
 */
data class SQLWritableColumn(
    val readExpression: String,
    val writeExpression: String
) : SQLColumnDetail()

/**
 * The column is an expression and so not writable (for example an expression such as an addition of two columns)
 * @param expression The expression
 */
data class SQLReadOnlyColumn(
    val expression: String
) : SQLColumnDetail()