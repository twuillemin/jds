package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a predicate greater than or equal
 *
 * @param left The left element of the equal
 * @param right The right element of the equal
 */
data class GreaterThanOrEqual(
    val left: RequestElement,
    val right: RequestElement
) : Predicate