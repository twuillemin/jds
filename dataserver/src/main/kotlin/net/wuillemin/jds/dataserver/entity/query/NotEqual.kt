package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a predicate not equal
 *
 * @param left The left element of the equal
 * @param right The right element of the equal
 */
data class NotEqual(
    val left: RequestElement,
    val right: RequestElement
) : Predicate