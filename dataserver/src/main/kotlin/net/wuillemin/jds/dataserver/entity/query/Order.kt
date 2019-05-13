package net.wuillemin.jds.dataserver.entity.query

/**
 * The order to sort data
 *
 * @param columnName The name of the column to order
 * @param direction The direction
 */
data class Order(
    val columnName: String,
    val direction: OrderDirection = OrderDirection.ASCENDING
)