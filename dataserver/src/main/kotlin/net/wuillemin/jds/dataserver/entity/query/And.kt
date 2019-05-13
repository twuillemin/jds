package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents a and predicate
 *
 * @param predicates The list of sub predicates
 */
data class And(
    val predicates: List<RequestElement>
) : Predicate