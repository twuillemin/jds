package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.Column
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
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.entity.query.RequestElement
import net.wuillemin.jds.dataserver.entity.query.StartsWith
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.query.Context
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

/**
 * Service for converting a predicate to a SQL clause
 *
 * @param contextBuilder The builder of predicate context (for parsing the predicate)
 */
@Service
class SQLPredicateConverter(private val contextBuilder: PredicateContextBuilder) {

    /**
     * Convert the given predicate to a SQL clause. If no predicate is given a neutral clause si generated The result
     * does not include the WHERE keyword, so that it could used with other generated clauses if needed
     *
     * @param columns The columns of the DataProvider
     * @param predicateToConvert The predicate to convert
     * @return The result of the conversion having the SQL and the objects to map
     */
    fun generateWhereClause(columns: List<Column>, predicateToConvert: Predicate?): ConvertedRequestElement {

        return predicateToConvert
            ?.let { predicate ->
                convertPredicate(
                    contextBuilder.buildContext(columns, predicate),
                    predicate)
            }
            ?: ConvertedRequestElement("1=1", emptyList())

    }

    private fun convertRequestElement(context: Context, element: RequestElement): ConvertedRequestElement {
        return when (element) {
            is Predicate  -> convertPredicate(context, element)
            is Value      -> convertValue(context, element)
            is ColumnName -> convertColumnName(element)
            else          -> {
                throw BadParameterException(E.supplier.sql.util.converterBadRequestElement, element::class)
            }
        }
    }

    private fun convertPredicate(context: Context, predicate: Predicate): ConvertedRequestElement {

        return when (predicate) {
            is And                -> {
                val intermediates = predicate.predicates.map { it -> convertRequestElement(context, it) }.toList()
                ConvertedRequestElement(
                    intermediates.joinToString(prefix = "( (", separator = ") AND (", postfix = ") )") { it.sql },
                    intermediates.flatMap { it -> it.values }.toList())
            }
            is Contains           -> {
                val column = convertRequestElement(context, predicate.column)
                val value = convertRequestElement(context, predicate.value)
                val updatedValue = updateValueString(value) { "%$it%" }
                if (predicate.caseSensitive) {
                    ConvertedRequestElement("${column.sql} LIKE ${value.sql}", column.values + updatedValue)
                }
                else {
                    ConvertedRequestElement("LOWER(${column.sql}) LIKE ${value.sql}", column.values + updatedValue.toLowerCase())
                }
            }
            is EndsWith           -> {
                val column = convertRequestElement(context, predicate.column)
                val value = convertRequestElement(context, predicate.value)
                val updatedValue = updateValueString(value) { "%$it" }
                if (predicate.caseSensitive) {
                    ConvertedRequestElement("${column.sql} LIKE ${value.sql}", column.values + updatedValue)
                }
                else {
                    ConvertedRequestElement("LOWER(${column.sql}) LIKE ${value.sql}", column.values + updatedValue.toLowerCase())
                }
            }
            is Equal              -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} = ${right.sql}", left.values + right.values)
            }
            is GreaterThan        -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} > ${right.sql}", left.values + right.values)
            }
            is GreaterThanOrEqual -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} >= ${right.sql}", left.values + right.values)
            }
            is In                 -> {
                val column = convertRequestElement(context, predicate.column)
                val elements = predicate.values.map { convertRequestElement(context, it) }
                val sqls = elements.joinToString(separator = ",") { it.sql }
                val values = column.values + elements.flatMap { it.values }
                ConvertedRequestElement("${column.sql} IN ($sqls)", values)
            }
            is LowerThan          -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} < ${right.sql}", left.values + right.values)
            }
            is LowerThanOrEqual   -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} <= ${right.sql}", left.values + right.values)
            }
            is NotEqual           -> {
                val left = convertRequestElement(context, predicate.left)
                val right = convertRequestElement(context, predicate.right)
                ConvertedRequestElement("${left.sql} <> ${right.sql}", left.values + right.values)
            }
            is NotIn              -> {
                val column = convertRequestElement(context, predicate.column)
                val elements = predicate.values.map { convertRequestElement(context, it) }
                val sqls = elements.joinToString(separator = ",") { it.sql }
                val values = column.values + elements.flatMap { it.values }
                ConvertedRequestElement("${column.sql} NOT IN ($sqls)", values)
            }
            is Or                 -> {
                val intermediates = predicate.predicates.map { it -> convertRequestElement(context, it) }.toList()
                ConvertedRequestElement(
                    intermediates.joinToString(prefix = "( (", separator = ") OR (", postfix = ") )") { it.sql },
                    intermediates.flatMap { it -> it.values }.toList())
            }
            is StartsWith         -> {
                val column = convertRequestElement(context, predicate.column)
                val value = convertRequestElement(context, predicate.value)
                val updatedValue = updateValueString(value) { "$it%" }
                if (predicate.caseSensitive) {
                    ConvertedRequestElement("${column.sql} LIKE ${value.sql}", column.values + updatedValue)
                }
                else {
                    ConvertedRequestElement("LOWER(${column.sql}) LIKE ${value.sql}", column.values + updatedValue.toLowerCase())
                }
            }
            else                  -> {
                throw BadParameterException(E.supplier.sql.util.converterBadPredicate, predicate::class)
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun convertValue(context: Context, value: Value): ConvertedRequestElement {

        val rawValue = value.value.toString()

        val convertedValue: Any = context[value]
            ?.let { dataType ->
                when (dataType) {
                    DataType.STRING          -> rawValue
                    DataType.LONG            -> rawValue.toLong().toString()
                    DataType.DOUBLE          -> rawValue.toDouble().toString()
                    DataType.BOOLEAN         -> rawValue.toBoolean().toString()
                    DataType.DATE            -> LocalDate.parse(rawValue)
                    DataType.TIME            -> LocalTime.parse(rawValue)
                    DataType.DATE_TIME       -> OffsetDateTime.parse(rawValue)
                    DataType.LIST_OF_STRINGS -> "[\"$rawValue\"]"
                }
            }
            ?: let {
                rawValue
            }

        return ConvertedRequestElement("?", listOf(convertedValue))
    }

    private fun convertColumnName(columnName: ColumnName): ConvertedRequestElement {

        return ConvertedRequestElement(columnName.name, emptyList())
    }

    /**
     * A Simple utility method that retrieve the first value from a ConvertedRequestElement values, expected
     * as a string, and apply a function to it. This function mainly to be used for Like like predicates, where
     * the value should always be a a single string
     *
     * @param value The converted element to read the string from
     * @param conversion The conversion to apply
     * @return the result of the conversion function
     */
    private fun updateValueString(value: ConvertedRequestElement, conversion: (String) -> String): String {
        val base = value.values.firstOrNull()
        return when (base) {
            is String -> conversion(base)
            else      -> throw BadParameterException(E.supplier.sql.util.converterLikeNotOnString)
        }
    }

    /**
     * The result of a converted request
     */
    data class ConvertedRequestElement(
        /**
         * The SQL code generated for this element
         */
        val sql: String,
        /**
         * The list of values to be set in the prepare statement
         */
        val values: List<Any>)
}