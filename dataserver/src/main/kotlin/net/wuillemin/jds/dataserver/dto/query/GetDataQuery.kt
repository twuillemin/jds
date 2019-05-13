package net.wuillemin.jds.dataserver.dto.query

import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.entity.query.Predicate

/**
 * The query for retrieving data
 *
 * @param filter The predicate to filter the query
 * @param orders The orders to sort the results
 * @param indexFirstRecord The index of the first record
 * @param numberOfRecords The number of records to retrieve
 */
data class GetDataQuery(
    val filter: Predicate? = null,
    val orders: List<Order>? = null,
    val indexFirstRecord: Int? = null,
    val numberOfRecords: Int? = null
)