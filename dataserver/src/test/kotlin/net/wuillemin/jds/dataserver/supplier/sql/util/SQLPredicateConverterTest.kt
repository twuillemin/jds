package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
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
import net.wuillemin.jds.dataserver.entity.query.StartsWith
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLPredicateConverterTest {

    private val connection = createDatabase()
    private val columns = createColumns()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)

    @AfterAll()
    fun afterAll() {
        connection.close()
    }

    // -----------------------------------------------
    //                 AND
    // -----------------------------------------------
    @Test
    fun `And generated for two columns`() {
        validateResults(And(listOf(ColumnName("bool1"), ColumnName("bool2"))), setOf(1))
    }

    @Test
    fun `And generated for a column and a value`() {
        validateResults(And(listOf(ColumnName("bool1"), Value("true"))), setOf(1, 3))
        validateResults(And(listOf(ColumnName("bool1"), Value(true))), setOf(1, 3))
    }

    @Test
    fun `And generated for a two predicates`() {
        validateResults(
            And(
                listOf(
                    Equal(ColumnName("string1"), ColumnName("string2")),
                    Equal(ColumnName("string1"), ColumnName("string2")))),
            setOf(1))
    }

    // -----------------------------------------------
    //                 CONTAINS
    // -----------------------------------------------
    @Test
    fun `Contains generated for a column and a value`() {
        validateResults(Contains(ColumnName("string1"), Value("b")), setOf(1))
        validateResults(Contains(ColumnName("string1"), Value("B"), true), emptySet())
        validateResults(Contains(ColumnName("string1"), Value("B"), false), setOf(1))
        validateResults(Contains(ColumnName("string1"), Value("bbbb")), setOf(1))
    }

    // -----------------------------------------------
    //                 ENDSWITH
    // -----------------------------------------------
    @Test
    fun `EndsWith generated for a column and a value`() {
        validateResults(EndsWith(ColumnName("string1"), Value("b")), setOf(1))
        validateResults(Contains(ColumnName("string1"), Value("B"), true), emptySet())
        validateResults(Contains(ColumnName("string1"), Value("B"), false), setOf(1))
        validateResults(EndsWith(ColumnName("string1"), Value("bbbb")), setOf(1))
    }

    // -----------------------------------------------
    //                 EQUAL
    // -----------------------------------------------
    @Test
    fun `Equal generated for two columns`() {
        validateResults(Equal(ColumnName("string1"), ColumnName("string2")), setOf(1))
        validateResults(Equal(ColumnName("int1"), ColumnName("int2")), setOf(1))
        validateResults(Equal(ColumnName("float1"), ColumnName("float2")), setOf(1))
        validateResults(Equal(ColumnName("bool1"), ColumnName("bool2")), setOf(1))
        validateResults(Equal(ColumnName("date1"), ColumnName("date2")), setOf(1))
        validateResults(Equal(ColumnName("time1"), ColumnName("time2")), setOf(1))
        validateResults(Equal(ColumnName("datetime1"), ColumnName("datetime2")), setOf(1))
    }

    @Test
    fun `Equal generated for a column and a value`() {
        validateResults(Equal(ColumnName("string1"), Value("bbbb")), setOf(1))
        validateResults(Equal(ColumnName("int1"), Value(2)), setOf(1))
        validateResults(Equal(ColumnName("float1"), Value(1.5)), setOf(1))
        validateResults(Equal(ColumnName("bool1"), Value(true)), setOf(1, 3))
        validateResults(Equal(ColumnName("date1"), Value("2018-12-25")), setOf(1))
        validateResults(Equal(ColumnName("time1"), Value("13:00:00")), setOf(1))
        validateResults(Equal(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(1))
    }

    // -----------------------------------------------
    //                 GREATER THAN
    // -----------------------------------------------
    @Test
    fun `GreaterThan generated for two columns`() {
        validateResults(GreaterThan(ColumnName("string1"), ColumnName("string2")), setOf(3))
        validateResults(GreaterThan(ColumnName("int1"), ColumnName("int2")), setOf(3))
        validateResults(GreaterThan(ColumnName("float1"), ColumnName("float2")), setOf(3))
        validateResults(GreaterThan(ColumnName("date1"), ColumnName("date2")), setOf(3))
        validateResults(GreaterThan(ColumnName("time1"), ColumnName("time2")), setOf(3))
        validateResults(GreaterThan(ColumnName("datetime1"), ColumnName("datetime2")), setOf(3))
    }

    @Test
    fun `GreaterThan generated for a column and a value`() {
        validateResults(GreaterThan(ColumnName("string1"), Value("bbbb")), setOf(3))
        validateResults(GreaterThan(ColumnName("int1"), Value(2)), setOf(3))
        validateResults(GreaterThan(ColumnName("float1"), Value(1.5)), setOf(3))
        validateResults(GreaterThan(ColumnName("date1"), Value("2018-12-25")), setOf(3))
        validateResults(GreaterThan(ColumnName("time1"), Value("13:00:00")), setOf(3))
        validateResults(GreaterThan(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(3))
    }

    // -----------------------------------------------
    //                 GREATER THAN OR EQUAL
    // -----------------------------------------------
    @Test
    fun `GreaterThanOrEqual generated for two columns`() {
        validateResults(GreaterThanOrEqual(ColumnName("string1"), ColumnName("string2")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("int1"), ColumnName("int2")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("float1"), ColumnName("float2")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("date1"), ColumnName("date2")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("time1"), ColumnName("time2")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("datetime1"), ColumnName("datetime2")), setOf(1, 3))
    }

    @Test
    fun `GreaterThanOrEqual generated for a column and a value`() {
        validateResults(GreaterThanOrEqual(ColumnName("string1"), Value("bbbb")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("int1"), Value(2)), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("float1"), Value(1.5)), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("date1"), Value("2018-12-25")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("time1"), Value("13:00:00")), setOf(1, 3))
        validateResults(GreaterThanOrEqual(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(1, 3))
    }

    // -----------------------------------------------
    //                 IN
    // -----------------------------------------------
    @Test
    fun `In generated for a column and a value`() {
        validateResults(In(ColumnName("string1"), listOf(Value("bbbb"), Value("aaaa"))), setOf(1, 2))
        validateResults(In(ColumnName("int1"), listOf(Value(1), Value(2))), setOf(1, 2))
        validateResults(In(ColumnName("float1"), listOf(Value(0.5), Value(1.5))), setOf(1, 2))
        validateResults(In(ColumnName("date1"), listOf(Value("2018-12-24"), Value("2018-12-25"))), setOf(1, 2))
        validateResults(In(ColumnName("time1"), listOf(Value("12:00:00"), Value("13:00:00"))), setOf(1, 2))
        validateResults(In(ColumnName("datetime1"), listOf(Value("2018-12-25T12:00:00"), Value("2018-12-25T13:00:00"))), setOf(1, 2))
    }

    // -----------------------------------------------
    //                 LOWER THAN
    // -----------------------------------------------
    @Test
    fun `LowerThan generated for two columns`() {
        validateResults(LowerThan(ColumnName("string1"), ColumnName("string2")), setOf(2))
        validateResults(LowerThan(ColumnName("int1"), ColumnName("int2")), setOf(2))
        validateResults(LowerThan(ColumnName("float1"), ColumnName("float2")), setOf(2))
        validateResults(LowerThan(ColumnName("date1"), ColumnName("date2")), setOf(2))
        validateResults(LowerThan(ColumnName("time1"), ColumnName("time2")), setOf(2))
        validateResults(LowerThan(ColumnName("datetime1"), ColumnName("datetime2")), setOf(2))
    }

    @Test
    fun `LowerThan generated for a column and a value`() {
        validateResults(LowerThan(ColumnName("string1"), Value("bbbb")), setOf(2))
        validateResults(LowerThan(ColumnName("int1"), Value(2)), setOf(2))
        validateResults(LowerThan(ColumnName("float1"), Value(1.5)), setOf(2))
        validateResults(LowerThan(ColumnName("date1"), Value("2018-12-25")), setOf(2))
        validateResults(LowerThan(ColumnName("time1"), Value("13:00:00")), setOf(2))
        validateResults(LowerThan(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(2))
    }

    // -----------------------------------------------
    //                 LOWER THAN OR EQUAL
    // -----------------------------------------------
    @Test
    fun `LowerThanOrEqual generated for two columns`() {
        validateResults(LowerThanOrEqual(ColumnName("string1"), ColumnName("string2")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("int1"), ColumnName("int2")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("float1"), ColumnName("float2")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("date1"), ColumnName("date2")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("time1"), ColumnName("time2")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("datetime1"), ColumnName("datetime2")), setOf(1, 2))
    }

    @Test
    fun `LowerThanOrEqual generated for a column and a value`() {
        validateResults(LowerThanOrEqual(ColumnName("string1"), Value("bbbb")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("int1"), Value(2)), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("float1"), Value(1.5)), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("date1"), Value("2018-12-25")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("time1"), Value("13:00:00")), setOf(1, 2))
        validateResults(LowerThanOrEqual(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(1, 2))
    }

    // -----------------------------------------------
    //                 NOT EQUAL
    // -----------------------------------------------
    @Test
    fun `NotEqual generated for two columns`() {
        validateResults(NotEqual(ColumnName("string1"), ColumnName("string2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("int1"), ColumnName("int2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("float1"), ColumnName("float2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("bool1"), ColumnName("bool2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("date1"), ColumnName("date2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("time1"), ColumnName("time2")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("datetime1"), ColumnName("datetime2")), setOf(2, 3))
    }

    @Test
    fun `NotEqual generated for a column and a value`() {
        validateResults(NotEqual(ColumnName("string1"), Value("bbbb")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("int1"), Value(2)), setOf(2, 3))
        validateResults(NotEqual(ColumnName("float1"), Value(1.5)), setOf(2, 3))
        validateResults(NotEqual(ColumnName("bool1"), Value(true)), setOf(2))
        validateResults(NotEqual(ColumnName("date1"), Value("2018-12-25")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("time1"), Value("13:00:00")), setOf(2, 3))
        validateResults(NotEqual(ColumnName("datetime1"), Value("2018-12-25T13:00:00.000Z")), setOf(2, 3))
    }

    // -----------------------------------------------
    //                 NOT IN
    // -----------------------------------------------
    @Test
    fun `Not In generated for a column and a value`() {
        validateResults(NotIn(ColumnName("string1"), listOf(Value("bbbb"), Value("aaaa"))), setOf(3))
        validateResults(NotIn(ColumnName("int1"), listOf(Value(1), Value(2))), setOf(3))
        validateResults(NotIn(ColumnName("float1"), listOf(Value(0.5), Value(1.5))), setOf(3))
        validateResults(NotIn(ColumnName("date1"), listOf(Value("2018-12-24"), Value("2018-12-25"))), setOf(3))
        validateResults(NotIn(ColumnName("time1"), listOf(Value("12:00:00"), Value("13:00:00"))), setOf(3))
        validateResults(NotIn(ColumnName("datetime1"), listOf(Value("2018-12-25T12:00:00"), Value("2018-12-25T13:00:00"))), setOf(3))
    }

    // -----------------------------------------------
    //                 OR
    // -----------------------------------------------
    @Test
    fun `Or generated for two columns`() {
        validateResults(Or(listOf(ColumnName("bool1"), ColumnName("bool2"))), setOf(1, 2, 3))
    }

    @Test
    fun `Or generated for a column and a value`() {
        validateResults(Or(listOf(ColumnName("bool1"), Value("true"))), setOf(1, 2, 3))
        validateResults(Or(listOf(ColumnName("bool1"), Value(true))), setOf(1, 2, 3))
        validateResults(Or(listOf(ColumnName("bool1"), Value("false"))), setOf(1, 3))
        validateResults(Or(listOf(ColumnName("bool1"), Value(false))), setOf(1, 3))

    }

    @Test
    fun `Or generated for a two predicates`() {
        validateResults(
            Or(
                listOf(
                    Equal(ColumnName("string1"), ColumnName("string2")),
                    Equal(ColumnName("int1"), ColumnName("int2")))),
            setOf(1))
    }

    // -----------------------------------------------
    //                 STARTSWITH
    // -----------------------------------------------
    @Test
    fun `StartsWith generated for a column and a value`() {
        validateResults(StartsWith(ColumnName("string1"), Value("b")), setOf(1))
        validateResults(Contains(ColumnName("string1"), Value("B"), true), emptySet())
        validateResults(Contains(ColumnName("string1"), Value("B"), false), setOf(1))
        validateResults(StartsWith(ColumnName("string1"), Value("bbbb")), setOf(1))
    }

    // -----------------------------------------------
    //                 UTILITY FUNCTIONS
    // -----------------------------------------------
    private fun createDatabase(): Connection {

        val connection = DriverManager.getConnection("jdbc:h2:mem:")

        val stmtDrop = connection.createStatement()
        stmtDrop.execute("DROP TABLE IF EXISTS test")
        stmtDrop.close()

        val stmtCreate = connection.createStatement()
        stmtCreate.execute(
            "CREATE TABLE test( " +
                "id INTEGER, " +
                "string1 VARCHAR, " +
                "string2 VARCHAR, " +
                "int1 INTEGER, " +
                "int2 INTEGER," +
                "float1 FLOAT, " +
                "float2 FLOAT," +
                "bool1 BOOLEAN, " +
                "bool2 BOOLEAN, " +
                "date1 DATE, " +
                "date2 DATE, " +
                "time1 TIME, " +
                "time2 TIME, " +
                "datetime1 TIMESTAMP WITH TIME ZONE, " +
                "datetime2 TIMESTAMP WITH TIME ZONE " +
                ")")
        stmtCreate.close()

        val insertQuery = "INSERT INTO test(id, string1, string2, int1, int2, float1, float2, bool1, bool2, date1, date2, time1, time2, datetime1, datetime2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        var idx = 1

        val stmtInsert1 = connection.prepareStatement(insertQuery)
        stmtInsert1.setObject(idx++, 1)
        stmtInsert1.setObject(idx++, "bbbb")
        stmtInsert1.setObject(idx++, "bbbb")
        stmtInsert1.setObject(idx++, 2)
        stmtInsert1.setObject(idx++, 2)
        stmtInsert1.setObject(idx++, 1.5)
        stmtInsert1.setObject(idx++, 1.5)
        stmtInsert1.setObject(idx++, true)
        stmtInsert1.setObject(idx++, true)
        stmtInsert1.setObject(idx++, LocalDate.parse("2018-12-25"))
        stmtInsert1.setObject(idx++, LocalDate.parse("2018-12-25"))
        stmtInsert1.setObject(idx++, LocalTime.parse("13:00:00"))
        stmtInsert1.setObject(idx++, LocalTime.parse("13:00:00"))
        stmtInsert1.setObject(idx++, OffsetDateTime.parse("2018-12-25T13:00:00Z"))
        stmtInsert1.setObject(idx, OffsetDateTime.parse("2018-12-25T13:00:00Z"))
        stmtInsert1.execute()
        stmtInsert1.close()

        idx = 1
        val stmtInsert2 = connection.prepareStatement(insertQuery)
        stmtInsert2.setObject(idx++, 2)
        stmtInsert2.setObject(idx++, "aaaa")
        stmtInsert2.setObject(idx++, "bbbb")
        stmtInsert2.setObject(idx++, 1)
        stmtInsert2.setObject(idx++, 2)
        stmtInsert2.setObject(idx++, 0.5)
        stmtInsert2.setObject(idx++, 1.5)
        stmtInsert2.setObject(idx++, false)
        stmtInsert2.setObject(idx++, true)
        stmtInsert2.setObject(idx++, LocalDate.parse("2018-12-24"))
        stmtInsert2.setObject(idx++, LocalDate.parse("2018-12-25"))
        stmtInsert2.setObject(idx++, LocalTime.parse("12:00:00"))
        stmtInsert2.setObject(idx++, LocalTime.parse("13:00:00"))
        stmtInsert2.setObject(idx++, OffsetDateTime.parse("2018-12-25T12:00:00Z"))
        stmtInsert2.setObject(idx, OffsetDateTime.parse("2018-12-25T13:00:00Z"))
        stmtInsert2.execute()
        stmtInsert2.close()

        idx = 1
        val stmtInsert3 = connection.prepareStatement(insertQuery)
        stmtInsert3.setObject(idx++, 3)
        stmtInsert3.setObject(idx++, "cccc")
        stmtInsert3.setObject(idx++, "bbbb")
        stmtInsert3.setObject(idx++, 3)
        stmtInsert3.setObject(idx++, 2)
        stmtInsert3.setObject(idx++, 2.5)
        stmtInsert3.setObject(idx++, 1.5)
        stmtInsert3.setObject(idx++, true)
        stmtInsert3.setObject(idx++, false)
        stmtInsert3.setObject(idx++, LocalDate.parse("2018-12-26"))
        stmtInsert3.setObject(idx++, LocalDate.parse("2018-12-25"))
        stmtInsert3.setObject(idx++, LocalTime.parse("14:00:00"))
        stmtInsert3.setObject(idx++, LocalTime.parse("13:00:00"))
        stmtInsert3.setObject(idx++, OffsetDateTime.parse("2018-12-25T14:00:00Z"))
        stmtInsert3.setObject(idx, OffsetDateTime.parse("2018-12-25T13:00:00Z"))
        stmtInsert3.execute()
        stmtInsert3.close()

        return connection
    }

    private fun createColumns(): List<Column> {
        return listOf(
            ColumnAttribute(
                "id",
                DataType.LONG,
                4,
                WritableStorage("table_test", "id", "id", false, true, true)),
            ColumnAttribute(
                "string1",
                DataType.STRING,
                250,
                WritableStorage("table_test", "string1", "string1", true, false, false)),
            ColumnAttribute(
                "string2",
                DataType.STRING,
                250,
                WritableStorage("table_test", "string2", "string1", true, false, false)),
            ColumnAttribute(
                "int1",
                DataType.LONG,
                4,
                WritableStorage("table_test", "int1", "int1", true, false, false)),
            ColumnAttribute(
                "int2",
                DataType.LONG,
                4,
                WritableStorage("table_test", "int2", "int1", true, false, false)),
            ColumnAttribute(
                "float1",
                DataType.DOUBLE,
                4,
                WritableStorage("table_test", "float1", "float1", true, false, false)),
            ColumnAttribute(
                "float2",
                DataType.DOUBLE,
                4,
                WritableStorage("table_test", "float2", "float1", true, false, false)),
            ColumnAttribute(
                "bool1",
                DataType.BOOLEAN,
                4,
                WritableStorage("table_test", "bool1", "bool1", true, false, false)),
            ColumnAttribute(
                "bool2",
                DataType.BOOLEAN,
                4,
                WritableStorage("table_test", "bool2", "bool1", true, false, false)),
            ColumnAttribute(
                "date1",
                DataType.DATE,
                4,
                WritableStorage("table_test", "date1", "date1", true, false, false)),
            ColumnAttribute(
                "date2",
                DataType.DATE,
                4,
                WritableStorage("table_test", "date2", "date1", true, false, false)),
            ColumnAttribute(
                "time1",
                DataType.TIME,
                4,
                WritableStorage("table_test", "time1", "time1", true, false, false)),
            ColumnAttribute(
                "time2",
                DataType.TIME,
                4,
                WritableStorage("table_test", "time2", "time1", true, false, false)),
            ColumnAttribute(
                "datetime1",
                DataType.DATE_TIME,
                4,
                WritableStorage("table_test", "datetime1", "datetime1", true, false, false)),
            ColumnAttribute(
                "datetime2",
                DataType.DATE_TIME,
                4,
                WritableStorage("table_test", "datetime2", "datetime1", true, false, false)))
    }

    private fun validateResults(predicate: Predicate, expectedResults: Set<Int>) {

        val convertedPredicate = sqlPredicateConverter.generateWhereClause(columns, predicate)

        val ids = connection.prepareStatement("SELECT id FROM test WHERE ${convertedPredicate.sql}").use { statement ->

            convertedPredicate.values.forEachIndexed { index, any ->
                statement.setObject(index + 1, any)
            }

            statement.executeQuery().map { it.getInt(1) }.toSet()
        }

        Assertions.assertEquals(expectedResults.size, ids.size)
        Assertions.assertEquals(expectedResults, ids)
    }
}