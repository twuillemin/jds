package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.dataserver.entity.model.DataType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JDBCHelperTest {

    private val jdbcHelper = JDBCHelper()
    private val connection = createDatabase()

    @Test
    fun `Get DataType from JDBC`() {

        val typeByColumnName = connection.metaData
            .getColumns(null, null, "TABLE_TEST", null)
            .map { resultSet -> resultSet.getString("COLUMN_NAME") to jdbcHelper.getDataTypeFromJdbcType(resultSet.getInt("DATA_TYPE")) }
            .toMap()

        val result1 = typeByColumnName.entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals(DataType.LONG, result1["id"])
        Assertions.assertEquals(DataType.STRING, result1["string1"])
        Assertions.assertEquals(DataType.LONG, result1["int1"])
        Assertions.assertEquals(DataType.DOUBLE, result1["float1"])
        Assertions.assertEquals(DataType.BOOLEAN, result1["bool1"])
        Assertions.assertEquals(DataType.DATE, result1["date1"])
        Assertions.assertEquals(DataType.TIME, result1["time1"])
        Assertions.assertEquals(DataType.DATE_TIME, result1["datetime1"])
    }

    @Test
    fun `Convert ResultSet to Map`() {

        // Make the full query
        val resultSet = connection.createStatement().executeQuery("SELECT * FROM table_test")

        val results = resultSet.map { jdbcHelper.convertResultSet(it, ZoneOffset.UTC.normalized()) }.toList()

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals(8, results[0].size)
        Assertions.assertEquals(1L, result1["id"])
        Assertions.assertEquals("bbbb", result1["string1"])
        Assertions.assertEquals(2L, result1["int1"])
        Assertions.assertEquals(1.5, result1["float1"])
        Assertions.assertEquals(true, result1["bool1"])
        Assertions.assertEquals(LocalDate.parse("2018-12-25"), result1["date1"])
        Assertions.assertEquals(LocalTime.parse("13:00:00"), result1["time1"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-12-25T13:00:00Z"), result1["datetime1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals(1, results[1].size)
        Assertions.assertEquals(2L, result2["id"])
    }

    private fun createDatabase(): Connection {

        val connection = DriverManager.getConnection("jdbc:h2:mem:")

        val stmtDrop = connection.createStatement()
        stmtDrop.execute("DROP TABLE IF EXISTS table_test")
        stmtDrop.close()

        val stmtCreate = connection.createStatement()
        stmtCreate.execute(
            "CREATE TABLE table_test( " +
                "id INTEGER, " +
                "string1 VARCHAR, " +
                "int1 INTEGER, " +
                "float1 FLOAT, " +
                "bool1 BOOLEAN, " +
                "date1 DATE, " +
                "time1 TIME, " +
                "datetime1 TIMESTAMP WITH TIME ZONE " +
                ")")
        stmtCreate.close()

        val insertQuery = "INSERT INTO table_test(id, string1, int1, float1, bool1, date1, time1, datetime1) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"

        var idx = 1

        val stmtInsert1 = connection.prepareStatement(insertQuery)
        stmtInsert1.setObject(idx++, 1)
        stmtInsert1.setObject(idx++, "bbbb")
        stmtInsert1.setObject(idx++, 2)
        stmtInsert1.setObject(idx++, 1.5)
        stmtInsert1.setObject(idx++, true)
        stmtInsert1.setObject(idx++, LocalDate.parse("2018-12-25"))
        stmtInsert1.setObject(idx++, LocalTime.parse("13:00:00"))
        stmtInsert1.setObject(idx, OffsetDateTime.parse("2018-12-25T13:00:00Z"))
        stmtInsert1.execute()
        stmtInsert1.close()

        idx = 1
        val stmtInsert2 = connection.prepareStatement(insertQuery)
        stmtInsert2.setObject(idx++, 2)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx++, null)
        stmtInsert2.setObject(idx, null)
        stmtInsert2.execute()
        stmtInsert2.close()

        return connection
    }
}