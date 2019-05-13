package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a predicate equal
 *
 * @param left The left element of the equal
 * @param right The right element of the equal
 */
data class Equal(
    val left: RequestElement,
    val right: RequestElement
) : Predicate