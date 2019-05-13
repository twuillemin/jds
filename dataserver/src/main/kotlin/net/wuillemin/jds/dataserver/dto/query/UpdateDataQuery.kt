package net.wuillemin.jds.dataserver.dto.query

import net.wuillemin.jds.dataserver.entity.query.Predicate

/**
 * The query for updating data
 *
 * @param filter The predicate to select data to be updated
 * @param data The data to be written. Note, that writing null is allowed.
 */
data class UpdateDataQuery(
    val filter: Predicate,
    val data: Map<String, Any?>
)