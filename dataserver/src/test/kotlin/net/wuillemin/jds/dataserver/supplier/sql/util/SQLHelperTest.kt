package net.wuillemin.jds.dataserver.supplier.sql.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class SQLHelperTest {

    private val sqlHelper = SQLHelper()

    // -----------------------------------------------
    //                 CLEAN SQL
    // -----------------------------------------------

    @Test
    fun `Dirty SQL is cleaned`() {

        val dirty = " SELECT /* columnA, */ columnB FROM \n -- Comment\n TABLE;"

        val cleaned = sqlHelper.cleanSQL(dirty)

        // Remove the double space as there are no issue here
        val multiSpace = Pattern.compile("\\s+")
        val cleanedWithoutRepeatedSpaces = multiSpace.matcher(cleaned).replaceAll(" ").trim()

        Assertions.assertEquals("SELECT columnB FROM TABLE", cleanedWithoutRepeatedSpaces)
    }

    // -----------------------------------------------
    //                 GET TABLES
    // -----------------------------------------------

    @Test
    fun `Retrieve table from simple query`() {

        val query = "SELECT * FROM MyTable"

        val tables = sqlHelper.getTableByAlias(query)

        Assertions.assertEquals(1, tables.size)
        Assertions.assertEquals("mytable", tables["mytable"])
    }

    @Test
    fun `Retrieve table and alias from simple query`() {

        val query = "SELECT * FROM MyTable As MyAlias"

        val tables = sqlHelper.getTableByAlias(query)

        Assertions.assertEquals(1, tables.size)
        Assertions.assertEquals("mytable", tables["myalias"])
    }

    @Test
    fun `Retrieve table and alias from complex query`() {

        val query = "SELECT * FROM MyTable As MyAlias WHERE myId IN (SELECT id FROM subTable As subAlias)"

        val tables = sqlHelper.getTableByAlias(query)

        // Subquery should not be used
        Assertions.assertEquals(1, tables.size)
        Assertions.assertEquals("mytable", tables["myalias"])
    }

    @Test
    fun `Retrieve tables and alias from joined tables`() {

        val query = "SELECT t1.colA AS ca, t2.colA AS cb FROM tab1 AS t1 JOIN tab2 as t2 ON t1.id=t2.id"

        val tables = sqlHelper.getTableByAlias(query)

        Assertions.assertEquals(2, tables.size)
        Assertions.assertEquals("tab1", tables["t1"])
        Assertions.assertEquals("tab2", tables["t2"])
    }

    // -----------------------------------------------
    //                 GET COLUMNS
    // -----------------------------------------------


    @Test
    fun `Retrieve column from simple query`() {

        val query = "SELECT MyColumn FROM MyTable"

        val columns = sqlHelper.getColumnByAlias(query)

        Assertions.assertEquals(1, columns.size)
        Assertions.assertEquals(SQLWritableColumn(".MyColumn", ".MyColumn"), columns["MyColumn"])
    }

    @Test
    fun `Retrieve column from simple query with table reference`() {

        val query = "SELECT MyTableAlias.MyColumn FROM MyTable As MyTableAlias"

        val columns = sqlHelper.getColumnByAlias(query)

        // Without alias, the name of the reading column is used
        Assertions.assertEquals(1, columns.size)
        Assertions.assertEquals(SQLWritableColumn("MyTableAlias.MyColumn", "MyTableAlias.MyColumn"), columns["MyTableAlias.MyColumn"])
    }

    @Test
    fun `Retrieve column and alias from simple query`() {

        val query = "SELECT MyColumn As MyAlias FROM MyTable"

        val columns = sqlHelper.getColumnByAlias(query)

        Assertions.assertEquals(1, columns.size)
        Assertions.assertEquals(SQLWritableColumn(".MyColumn", ".MyColumn"), columns["MyAlias"])
    }

    @Test
    fun `Retrieve column and alias from simple query with table alias`() {

        val query = "SELECT MyTableAlias.MyColumn As MyAlias FROM MyTable As MyTableAlias"

        val columns = sqlHelper.getColumnByAlias(query)

        Assertions.assertEquals(1, columns.size)
        Assertions.assertEquals(SQLWritableColumn("MyTableAlias.MyColumn", "MyTableAlias.MyColumn"), columns["MyAlias"])
    }

    @Test
    fun `Retrieve column and alias from query with table alias and subquery`() {

        val query = "SELECT MyTableAlias.MyColumn As MyAlias FROM MyTable As MyTableAlias WHERE MyTableAlias.id IN (SELECT MyOtherID FROM MyJoinTable)"

        val columns = sqlHelper.getColumnByAlias(query)

        // Subquery should not be used
        Assertions.assertEquals(1, columns.size)
        Assertions.assertEquals(SQLWritableColumn("MyTableAlias.MyColumn", "MyTableAlias.MyColumn"), columns["MyAlias"])
    }

    @Test
    fun `Retrieve column and alias from joined tables`() {

        val query = "SELECT t1.colA AS ca, t2.colA AS cb FROM tab1 AS t1 JOIN tab2 as t2 ON t1.id=t2.id"

        val columns = sqlHelper.getColumnByAlias(query)

        Assertions.assertEquals(2, columns.size)
        Assertions.assertEquals(SQLWritableColumn("t1.colA", "t1.colA"), columns["ca"])
        Assertions.assertEquals(SQLWritableColumn("t2.colA", "t2.colA"), columns["cb"])
    }

    @Test
    fun `Retrieve column with operation`() {

        val query = "SELECT UPPER(t1.colA) AS ca, LOWER(t2.colA) AS cb FROM tab1 AS t1 JOIN tab2 as t2 ON t1.id=t2.id"

        val columns = sqlHelper.getColumnByAlias(query)

        Assertions.assertEquals(2, columns.size)
        Assertions.assertEquals(SQLWritableColumn("UPPER(t1.colA)", "t1.colA"), columns["ca"])
        Assertions.assertEquals(SQLWritableColumn("LOWER(t2.colA)", "t2.colA"), columns["cb"])
    }


}