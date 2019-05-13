package net.wuillemin.jds.dataserver.service.query

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
import net.wuillemin.jds.dataserver.entity.query.StartsWith
import net.wuillemin.jds.dataserver.entity.query.Value

/**
 * Define a Visitor that can be used to parse QueryPredicate
 */
interface QueryVisitor {

    /**
     * Visit a predicate And
     * @param and The predicate
     */
    fun visitAnd(and: And)

    /**
     * Visit a predicate ColumnName
     * @param columnName The name of the column
     */
    fun visitColumnName(columnName: ColumnName)

    /**
     * Visit a predicate Contains
     * @param contains The predicate
     */
    fun visitContains(contains: Contains)

    /**
     * Visit a predicate EndsWith
     * @param endsWith The predicate
     */
    fun visitEndsWith(endsWith: EndsWith)

    /**
     * Visit a predicate Equal
     * @param equal The predicate
     */
    fun visitEqual(equal: Equal)

    /**
     * Visit a predicate GreaterThan
     * @param greaterThan The predicate
     */
    fun visitGreaterThan(greaterThan: GreaterThan)

    /**
     * Visit a predicate GreaterThanOrEqual
     * @param greaterThanOrEqual The predicate
     */
    fun visitGreaterThanOrEqual(greaterThanOrEqual: GreaterThanOrEqual)

    /**
     * Visit a predicate In
     * @param inValues The predicate (named inValues because in is a keyword)
     */
    fun visitIn(inValues: In)

    /**
     * Visit a predicate LowerThan
     * @param lowerThan The predicate
     */
    fun visitLowerThan(lowerThan: LowerThan)

    /**
     * Visit a predicate LowerThanOrEqual
     * @param lowerThanOrEqual The predicate
     */
    fun visitLowerThanOrEqual(lowerThanOrEqual: LowerThanOrEqual)

    /**
     * Visit a predicate NotEqual
     * @param notEqual The predicate
     */
    fun visitNotEqual(notEqual: NotEqual)

    /**
     * Visit a predicate NotIn
     * @param notIn The predicate
     */
    fun visitNotIn(notIn: NotIn)

    /**
     * Visit a predicate Or
     * @param or The predicate
     */
    fun visitOr(or: Or)

    /**
     * Visit a predicate StartsWith
     * @param startsWith The predicate
     */
    fun visitStartsWith(startsWith: StartsWith)

    /**
     * Visit a Value
     * @param value The value
     */
    fun visitValue(value: Value)
}