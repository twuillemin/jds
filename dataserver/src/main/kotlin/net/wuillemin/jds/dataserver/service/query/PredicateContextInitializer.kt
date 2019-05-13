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
 * Visitor that creates an initial internalContext for a QueryPredicate. The initial internalContext only defines
 * the DataType that are mandatory.
 *
 * @param dataTypeByColumnName A dictionary giving the data type for the various columns present in the DataSpec / DataSource
 */
class PredicateContextInitializer(private val dataTypeByColumnName: Map<String, DataType>) : QueryVisitor {

    // The internal context which is created by the visitor
    private val internalContext = HashMap<RequestElement, DataType>()

    /**
     * Get the generated context once the visitor has run against the query
     */
    val context: Context
        get() = internalContext.toMap()


    override fun visitAnd(and: And) {
        internalContext[and] = DataType.BOOLEAN
    }

    override fun visitColumnName(columnName: ColumnName) {
        val dataType = dataTypeByColumnName[columnName.name]
            ?: throw BadParameterException(E.service.query.columnNotInDataProvider, columnName.name)

        internalContext[columnName] = dataType
    }

    override fun visitContains(contains: Contains) {
        internalContext[contains] = DataType.BOOLEAN
    }

    override fun visitEndsWith(endsWith: EndsWith) {
        internalContext[endsWith] = DataType.BOOLEAN
    }

    override fun visitEqual(equal: Equal) {
        internalContext[equal] = DataType.BOOLEAN
    }

    override fun visitGreaterThan(greaterThan: GreaterThan) {
        internalContext[greaterThan] = DataType.BOOLEAN
    }

    override fun visitGreaterThanOrEqual(greaterThanOrEqual: GreaterThanOrEqual) {
        internalContext[greaterThanOrEqual] = DataType.BOOLEAN
    }

    override fun visitIn(inValues: In) {
        internalContext[inValues] = DataType.BOOLEAN
    }

    override fun visitLowerThan(lowerThan: LowerThan) {
        internalContext[lowerThan] = DataType.BOOLEAN
    }

    override fun visitLowerThanOrEqual(lowerThanOrEqual: LowerThanOrEqual) {
        internalContext[lowerThanOrEqual] = DataType.BOOLEAN
    }

    override fun visitNotEqual(notEqual: NotEqual) {
        internalContext[notEqual] = DataType.BOOLEAN
    }

    override fun visitNotIn(notIn: NotIn) {
        internalContext[notIn] = DataType.BOOLEAN
    }

    override fun visitOr(or: Or) {
        internalContext[or] = DataType.BOOLEAN
    }

    override fun visitStartsWith(startsWith: StartsWith) {
        internalContext[startsWith] = DataType.BOOLEAN
    }

    override fun visitValue(value: Value) {
        when (value.value) {
            is Double  -> internalContext[value] = DataType.DOUBLE
            is Float   -> internalContext[value] = DataType.DOUBLE
            is Boolean -> internalContext[value] = DataType.BOOLEAN
        }
    }
}