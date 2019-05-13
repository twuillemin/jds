package net.wuillemin.jds.dataserver.entity.query

/**
 * Represents an or predicate
 *
 * @param predicates The list of sub predicates
 */
data class Or(
    val predicates: List<RequestElement>
) : Predicate