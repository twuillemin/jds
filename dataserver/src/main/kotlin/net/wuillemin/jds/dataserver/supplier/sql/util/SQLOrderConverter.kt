package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.entity.query.OrderDirection
import net.wuillemin.jds.dataserver.exception.E
import org.springframework.stereotype.Service

/**
 * Service for converting a predicate to a SQL clause
 */
@Service
class SQLOrderConverter {

    /**
     * Convert the given list of order in a single string clause. The generated string does not include the ORDER BY.
     * If no order is given (null), than a null string is returned
     *
     * @param columns The columns of the DataProvider
     * @param orders The orders to convert
     * @return The result of the conversion having the SQL and the objects to map
     */
    fun generateOrderClause(columns: List<Column>, orders: List<Order>): String {

        val columnNames = columns.map { it.name }.toSet()

        return orders.joinToString(separator = ", ") { (columnName, direction) ->
            val directionSQL = if (direction == OrderDirection.ASCENDING) {
                "ASC"
            }
            else {
                "DESC"
            }
            if (columnNames.contains(columnName)) {
                "$columnName $directionSQL"
            }
            else {
                throw BadParameterException(E.supplier.sql.util.orderColumn, columnName)
            }
        }
    }
}
