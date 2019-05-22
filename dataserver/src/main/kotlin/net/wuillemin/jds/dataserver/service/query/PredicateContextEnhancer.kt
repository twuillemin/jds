package net.wuillemin.jds.dataserver.service.query

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.DataType
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
import net.wuillemin.jds.dataserver.entity.query.RequestElement
import net.wuillemin.jds.dataserver.entity.query.StartsWith
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.exception.E

/**
 * Visitor that will try to extend the knowledge of a given context by propagating the rules
 *
 * @param previousContext The existing context with the previously discovered information
 */
class PredicateContextEnhancer(previousContext: Context) : QueryVisitor {

    // The internal context which is updated by running the visitor
    private val internalContext = HashMap<RequestElement, DataType>(previousContext)

    /**
     * The number of changes, only read from external
     */
    var numberOfChanges: Int = 0
        private set

    /**
     * Get the generated context once the visitor has run against the query
     */
    val context: Context
        get() = internalContext.toMap()

    override fun visitAnd(and: And) {
        // Ensure that all sons are defined as Boolean
        if (and.predicates.asSequence().mapNotNull { predicate -> internalContext[predicate] }.any { dataType -> dataType != DataType.BOOLEAN }) {
            throw BadParameterException(E.service.query.andChildrenNotBoolean)
        }
    }

    override fun visitColumnName(columnName: ColumnName) {
        // Nothing to do
    }

    override fun visitContains(contains: Contains) {
        processColumnValuePredicate(contains.column, contains.value)
    }

    override fun visitEndsWith(endsWith: EndsWith) {
        processColumnValuePredicate(endsWith.column, endsWith.value)
    }

    override fun visitEqual(equal: Equal) {
        processLeftRightPredicate(equal.left, equal.right)
    }

    override fun visitGreaterThan(greaterThan: GreaterThan) {
        processLeftRightPredicate(greaterThan.left, greaterThan.right)
    }

    override fun visitGreaterThanOrEqual(greaterThanOrEqual: GreaterThanOrEqual) {
        processLeftRightPredicate(greaterThanOrEqual.left, greaterThanOrEqual.right)
    }

    override fun visitIn(inValues: In) {
        inValues.values.forEach { processLeftRightPredicate(inValues.column, it) }
    }

    override fun visitLowerThan(lowerThan: LowerThan) {
        processLeftRightPredicate(lowerThan.left, lowerThan.right)
    }

    override fun visitLowerThanOrEqual(lowerThanOrEqual: LowerThanOrEqual) {
        processLeftRightPredicate(lowerThanOrEqual.left, lowerThanOrEqual.right)
    }

    override fun visitNotEqual(notEqual: NotEqual) {
        processLeftRightPredicate(notEqual.left, notEqual.right)
    }

    override fun visitNotIn(notIn: NotIn) {
        notIn.values.forEach { processLeftRightPredicate(notIn.column, it) }
    }

    override fun visitOr(or: Or) {
        // Ensure that all sons are defined as Boolean
        if (or.predicates.asSequence().mapNotNull { predicate -> internalContext[predicate] }.any { dataType -> dataType != DataType.BOOLEAN }) {
            throw BadParameterException(E.service.query.orChildrenNotBoolean)
        }
    }

    override fun visitStartsWith(startsWith: StartsWith) {
        processColumnValuePredicate(startsWith.column, startsWith.value)
    }

    override fun visitValue(value: Value) {
        // Nothing to do
    }

    /**
     * Check that both values in a left-right predicate are comparable
     * @param left the left element
     * @param right the right element
     */
    private fun processLeftRightPredicate(
        right: RequestElement,
        left: RequestElement
    ) {

        internalContext[left]
            // If there is something on the left of the equation
            ?.let { leftContext ->
                // If there is something on the right
                internalContext[right]
                    ?.let { rightContext ->
                        // Ensure that left and right are compatible
                        if (leftContext != rightContext) {
                            if (!((leftContext == DataType.LONG && rightContext == DataType.DOUBLE) || (leftContext == DataType.DOUBLE && rightContext == DataType.LONG))) {
                                throw BadParameterException(E.service.query.differentLeftAndRightType, leftContext, rightContext)
                            }
                        }
                    }
                    ?: run {
                        // Otherwise, copy the context from right to left
                        internalContext[right] = leftContext
                        numberOfChanges++
                    }
            }
            ?: run {
                // If there is nothing on the left of the equation and if there is something on the right
                internalContext[right]?.let { rightContext ->
                    // Copy the context from right to left
                    internalContext[left] = rightContext
                    numberOfChanges++
                }
            }
    }

    /**
     * Check that the predicate is valid for column value
     * @param column The column
     * @param value The value
     */
    private fun processColumnValuePredicate(
        column: ColumnName,
        value: Value
    ) {

        if (internalContext[column] == null || internalContext[column] != DataType.STRING) {
            throw BadParameterException(E.service.query.likeColumnNotString)
        }

        internalContext[value]
            ?.let { existingDataType ->
                if (existingDataType != DataType.STRING) {
                    throw BadParameterException(E.service.query.likeValueNotString)
                }
            }
            ?: run {
                internalContext[value] = DataType.STRING
                numberOfChanges++
            }

    }
}