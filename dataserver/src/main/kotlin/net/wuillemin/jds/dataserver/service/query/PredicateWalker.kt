package net.wuillemin.jds.dataserver.service.query

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.query.And
import net.wuillemin.jds.dataserver.entity.query.ColumnName
import net.wuillemin.jds.dataserver.entity.query.Contains
import net.wuillemin.jds.dataserver.entity.query.EndsWith
import net.wuillemin.jds.dataserver.entity.query.Equal
import net.wuillemin.jds.dataserver.entity.query.GreaterThan
import net.wuillemin.jds.dataserver.entity.query.GreaterThanOrEqual
import net.wuillemin.jds.dataserver.entity.query.In
import net.wuillemin.jds.dataserver.entity.query.LowerThan
import net.wuillemin.jds.dataserver.entity.query.LowerThanOrEqual
import net.wuillemin.jds.dataserver.entity.query.NotEqual
import net.wuillemin.jds.dataserver.entity.query.NotIn
import net.wuillemin.jds.dataserver.entity.query.Or
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.entity.query.RequestElement
import net.wuillemin.jds.dataserver.entity.query.StartsWith
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.exception.E

/**
 * Walk all the nodes of a predicate, calling the visitor for each before going to sub nodes
 *
 * @param predicate The predicate to process
 */
class PredicateWalker(private val predicate: Predicate) {

    /**
     * Start the walking process
     * @param visitor The visitor
     * @return the visitor
     */
    fun <T : QueryVisitor> walk(visitor: T): T {
        return walkRequestElement(predicate, visitor)
    }

    private fun <T : QueryVisitor> walkRequestElement(requestElement: RequestElement, visitor: T): T {

        when (requestElement) {
            is And                -> {
                visitor.visitAnd(requestElement)
                requestElement.predicates.forEach { walkRequestElement(it, visitor) }
            }
            is ColumnName         -> visitor.visitColumnName(requestElement)
            is Contains           -> {
                visitor.visitContains(requestElement)
                walkRequestElement(requestElement.column, visitor)
                walkRequestElement(requestElement.value, visitor)
            }
            is EndsWith           -> {
                visitor.visitEndsWith(requestElement)
                walkRequestElement(requestElement.column, visitor)
                walkRequestElement(requestElement.value, visitor)
            }
            is Equal              -> {
                visitor.visitEqual(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is GreaterThan        -> {
                visitor.visitGreaterThan(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is GreaterThanOrEqual -> {
                visitor.visitGreaterThanOrEqual(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is In                 -> {
                visitor.visitIn(requestElement)
                requestElement.values.forEach { walkRequestElement(it, visitor) }
            }
            is LowerThan          -> {
                visitor.visitLowerThan(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is LowerThanOrEqual   -> {
                visitor.visitLowerThanOrEqual(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is NotEqual           -> {
                visitor.visitNotEqual(requestElement)
                walkRequestElement(requestElement.left, visitor)
                walkRequestElement(requestElement.right, visitor)
            }
            is NotIn              -> {
                visitor.visitNotIn(requestElement)
                requestElement.values.forEach { walkRequestElement(it, visitor) }
            }
            is Or                 -> {
                visitor.visitOr(requestElement)
                requestElement.predicates.forEach { walkRequestElement(it, visitor) }
            }
            is StartsWith         -> {
                visitor.visitStartsWith(requestElement)
                walkRequestElement(requestElement.column, visitor)
                walkRequestElement(requestElement.value, visitor)
            }
            is Value              -> visitor.visitValue(requestElement)
            else                  -> {
                throw BadParameterException(E.service.query.unableToWalkElement, requestElement::class)
            }
        }

        return visitor
    }
}