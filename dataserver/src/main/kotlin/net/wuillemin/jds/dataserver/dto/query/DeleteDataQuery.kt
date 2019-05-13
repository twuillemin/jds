package net.wuillemin.jds.dataserver.dto.query

import net.wuillemin.jds.dataserver.entity.query.Predicate

/**
 * The query for deleting data
 *
 * @param filter The predicate to select data to be deleted
 */
data class DeleteDataQuery(
    val filter: Predicate
)