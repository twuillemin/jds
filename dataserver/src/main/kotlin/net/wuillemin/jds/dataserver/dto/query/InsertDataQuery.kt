package net.wuillemin.jds.dataserver.dto.query

/**
 * The query for creating data
 *
 * @param data The data to be written. Note, that writing null is not allowed.
 */
data class InsertDataQuery(
    val data: Map<String, Any>
)