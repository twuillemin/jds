package net.wuillemin.jds.dataserver.dto.query

/**
 * The query for mass creating data
 * @param data The data to be written. Note, that writing null is not allowed.
 */
data class MassInsertDataQuery(
    val data: List<Map<String, Any>>
)