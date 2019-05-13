package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.exception.E
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.select.Select
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Pattern

/**
 * Service class grouping various common function for managing SQL
 */
@Service
class SQLHelper {

    companion object {

        private val SINGLE_LINE_COMMENT = Pattern.compile("--.*")
        private val MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/")
        private val LINE_BREAK = Pattern.compile("(\\r\\n|\\r|\\n)")
        private const val SQL_ENDING = ';'
    }

    /**
     * Clean a SQL for comment and lasting ; so that it could run smoothly, even when embedded as a sub-clause
     * @param sqlQuery The query to clean
     * @return the cleaned query
     */
    fun cleanSQL(sqlQuery: String): String {

        return sqlQuery
            // Apply the various cleaning regexp
            .let {
                SINGLE_LINE_COMMENT.matcher(it).replaceAll(" ")
            }.let {
                MULTI_LINE_COMMENT.matcher(it).replaceAll(" ")
            }.let {
                LINE_BREAK.matcher(it).replaceAll(" ").trim()
            }.let {
                // Remove the last ; if any
                if (it[it.length - 1] == SQL_ENDING) {
                    it.substring(0, it.length - 1)
                }
                else {
                    it
                }
            }
    }

    /**
     * Return the name of the tables indexed by alias
     * If there is no alias given, then tableName is used as alias
     *
     * @return The tables by alias
     */
    fun getTableByAlias(sqlQuery: String): Map<String, String> {

        val visitor = SQLSelectStatementVisitor()
        visitor.visit(parseSelectStatement(sqlQuery))

        return HashMap(visitor.getTableByAlias())
    }

    /**
     * Return the name of the columns indexed by alias
     * If there is no alias given, then name of the column is used as alias
     *
     * @return The tables by alias
     */
    fun getColumnByAlias(sqlQuery: String): Map<String, SQLColumnDetail> {

        val visitor = SQLSelectStatementVisitor()
        visitor.visit(parseSelectStatement(sqlQuery))

        return HashMap(visitor.getColumnByAlias())
    }


    private fun parseSelectStatement(sqlQuery: String): Select {
        val statement: Statement
        try {
            statement = CCJSqlParserUtil.parse(sqlQuery)
        }
        catch (exception: JSQLParserException) {
            throw BadParameterException(E.supplier.sql.util.sqlParseError, exception.message, sqlQuery)
        }

        if (statement !is Select) {
            throw BadParameterException(E.supplier.sql.util.sqlParseSelectError)
        }

        return statement
    }


}