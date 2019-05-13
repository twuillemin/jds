package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a filter
 *
 * @param value The value
 */
data class Value(
    val value: Any
) : RequestElement
