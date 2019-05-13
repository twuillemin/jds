package net.wuillemin.jds.dataserver.supplier.sql.util

import net.sf.jsqlparser.expression.CastExpression
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.FromItemVisitor
import net.sf.jsqlparser.statement.select.LateralSubSelect
import net.sf.jsqlparser.statement.select.ParenthesisFromItem
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SelectExpressionItem
import net.sf.jsqlparser.statement.select.SelectItemVisitor
import net.sf.jsqlparser.statement.select.SelectVisitor
import net.sf.jsqlparser.statement.select.SetOperationList
import net.sf.jsqlparser.statement.select.SubJoin
import net.sf.jsqlparser.statement.select.SubSelect
import net.sf.jsqlparser.statement.select.TableFunction
import net.sf.jsqlparser.statement.select.ValuesList
import net.sf.jsqlparser.statement.select.WithItem

/**
 * A visitor for walking a SQL Select statement and extract from it the information such
 * as table, est.
 */
class SQLSelectStatementVisitor : SelectVisitor, FromItemVisitor, SelectItemVisitor {

    private val otherItemNames: MutableList<String> = ArrayList()
    private val tableByAlias: MutableMap<String, String> = HashMap()
    private val columnByAlias: MutableMap<String, SQLColumnDetail> = HashMap()

    /**
     * Visit the given statement which will (hopefully) fill the information needed by the getter methods
     * such as getTableByAlias
     *
     * @param select The statement to process
     */
    fun visit(select: Select) {
        this.otherItemNames.clear()
        this.tableByAlias.clear()
        this.columnByAlias.clear()

        if (select.withItemsList != null) {
            for (withItem in select.withItemsList) {
                withItem.accept(this)
            }
        }
        if (select.selectBody != null) {
            select.selectBody.accept(this)
        }
    }

    /**
     * Return the name of the tables indexed by alias
     * If there is no alias given, then tableName is used as alias
     *
     * @return The tables by alias
     */
    fun getTableByAlias(): Map<String, String> {

        return this.tableByAlias
    }

    /**
     * Return the name of the columns indexed by alias
     * If there is no alias given, then name of the column is used as alias
     *
     * @return The tables by alias
     */
    fun getColumnByAlias(): Map<String, SQLColumnDetail> {

        return this.columnByAlias
    }

    // ----------------------------------------------------------------
    // FOR VISITING FROM SELECT
    // ----------------------------------------------------------------

    /**
     * Visit a PlainSelect node
     *
     * @param selectItem The select clause
     */
    override fun visit(selectItem: PlainSelect?) {

        selectItem?.fromItem?.accept(this)
        selectItem?.selectItems?.forEach { select ->
            select.accept(this)
        }
        selectItem?.joins?.forEach { join ->
            join.rightItem?.accept(this)
        }

        // We should parse the Where clause in case there is something hidden there with something like
        // plainSelect.getWhere().accept(this); But in fact this is a lot of code and that should not happen often
    }

    /**
     * Visit a list of operations
     *
     * @param operations The set of operations, a list of plainSelects connected by set operations (UNION,INTERSECT,MINUS,EXCEPT)
     */
    override fun visit(operations: SetOperationList?) {

        operations?.selects?.forEach { select ->
            select.accept(this)
        }
    }

    /**
     * Visit a With Item (inner select)
     *
     * @param withItem The With clause
     */
    override fun visit(withItem: WithItem?) {

        withItem?.name?.let { otherItemNames.add(it) }
        withItem?.selectBody?.accept(this)
    }

    // ----------------------------------------------------------------
    // FOR VISITING FROM ITEMS
    // ----------------------------------------------------------------

    /**
     * Visit a table
     *
     * @param tableItem The table
     */
    override fun visit(tableItem: Table?) {

        tableItem?.let { table ->
            table.name?.toLowerCase()?.let { tableName ->
                table.fullyQualifiedName?.toLowerCase()?.let { tableFullyQualifiedName ->

                    // If there is an alias use it, otherwise use the table as its own alias
                    val tableAlias = table.alias
                        ?.let { alias ->
                            if (alias.isUseAs && alias.name != null) {
                                alias.name.toLowerCase()
                            }
                            else {
                                tableName
                            }
                        }
                        ?: tableName

                    // If nothing was already found
                    if (!this.otherItemNames.contains(tableFullyQualifiedName) && !this.tableByAlias.containsKey(tableAlias)) {
                        this.tableByAlias[tableAlias] = tableName
                    }
                }
            }
        }
    }

    /**
     * Visit a sub select
     */
    override fun visit(subSelectItem: SubSelect?) {

        subSelectItem?.selectBody?.accept(this)
    }

    /**
     * Visit a sub join
     */
    override fun visit(subJoinItem: SubJoin?) {

        subJoinItem?.left?.accept(this)
        subJoinItem?.joinList?.forEach { join ->
            join.rightItem.accept(this)
        }
    }

    /**
     * Visit a lateral Sub Select
     */
    override fun visit(lateralSubSelect: LateralSubSelect?) {

        lateralSubSelect?.subSelect?.selectBody?.accept(this)
    }

    /**
     * Visit values
     */
    override fun visit(values: ValuesList) {
        // Nothing to do
    }

    /**
     * Visit table function
     */
    override fun visit(tableFunction: TableFunction?) {
        // Nothing to do
    }

    /**
     * Visit parenthesis from
     */
    override fun visit(parenthesisFromItem: ParenthesisFromItem?) {
        // Nothing to do
    }

    // ----------------------------------------------------------------
    // FOR VISITING SELECT ITEMS
    // ----------------------------------------------------------------

    /**
     * Visit the all columns symbol (*)
     */
    override fun visit(allColumns: AllColumns) {
        // Do Nothing
    }

    /**
     * Visit the all columns of the table symbol (.*)
     */
    override fun visit(allTableColumns: AllTableColumns) {
        // Do Nothing
    }

    /**
     * Visit the columns of a select
     */
    override fun visit(selectExpressionItem: SelectExpressionItem?) {

        selectExpressionItem?.let { selectExpression ->

            val writableColumn = when (selectExpression.expression) {

                //
                // Process the basic columns
                //
                is Column         -> {
                    val column: Column = selectExpression.expression as Column

                    column.columnName?.let { columnName ->

                        val tableName = column.table?.name ?: ""

                        SQLWritableColumn("$tableName.$columnName", "$tableName.$columnName")
                    }
                }

                //
                // Process the simple functions
                //
                is Function       -> {
                    val function = selectExpression.expression as Function

                    // Try to get the name of the function
                    function.name?.toLowerCase()?.let { functionName ->

                        // Just work with simple function
                        if (functionName == "upper" || functionName == "lower") {

                            // Get the first expression
                            val firstExpression = function.parameters
                                ?.expressions
                                ?.firstOrNull()
                                ?.toString()
                                ?.trim()

                            // If there is something remove the parenthesis and get the content
                            firstExpression
                                ?.let { expression ->
                                    ensureDottedColumnName(expression)
                                        ?.let { SQLWritableColumn(selectExpression.expression.toString(), it) }
                                }

                        }
                        else {
                            null
                        }
                    }
                }


                //
                // Process the CAST (that won't probably work in any case)
                //
                is CastExpression -> {
                    val castExpression = selectExpression.expression as CastExpression

                    castExpression.leftExpression
                        ?.let { leftExpression ->
                            val columnName = ensureDottedColumnName(leftExpression.toString())
                            columnName?.let { SQLWritableColumn(selectExpression.expression.toString(), it) }
                        }
                }

                // All other expression are not generating a writable column
                else              -> null
            }

            // Find the alias to use
            val alias = selectExpression.alias
                ?.let { alias ->
                    if (alias.isUseAs && alias.name != null) {
                        alias.name
                    }
                    else {
                        selectExpression.expression.toString()
                    }
                }
                ?: selectExpression.expression.toString()

            // If nothing was already found
            this.columnByAlias.putIfAbsent(
                alias,
                writableColumn ?: SQLReadOnlyColumn(selectExpression.expression.toString()))
        }
    }


    private fun ensureDottedColumnName(baseName: String): String? {

        return when (baseName.count { it == '.' }) {

            // If no dot found, than add an artificial one at the beginning
            0    -> ".$baseName"

            // If one dot found, than already well formed
            1    -> baseName

            // If multiple dot found, don't process
            else -> null
        }
    }
}
