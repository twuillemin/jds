package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.dataserver.dto.TableColumnMeta
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLPredicateConverter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLModelReaderTest {

    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(SQLModelReaderTest::class.java)
    private val jdbcHelper = JDBCHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlModelReader = SQLModelReader(jdbcHelper, sqlConnectionCache, schemaService)

    private val serverSQL = ServerSQL(
        "schemaId",
        "testServer",
        "groupId",
        true,
        "jdbc:h2:mem:",
        "sa",
        null)

    private val schemaSQL = SchemaSQL(
        "schemaId",
        "PUBLIC",
        "groupId",
        null)

    @BeforeAll
    fun beforeAll() {
        whenever(serverService.getServerById(any())).thenReturn(serverSQL)
        whenever(schemaService.getSchemaById(any())).thenReturn(schemaSQL)
    }

    private var noShutDownConnection: Connection? = null

    @BeforeEach
    fun beforeEach() {
        noShutDownConnection = sqlConnectionCache.getConnection(schemaSQL)
        sqlConnectionCache.getConnection(schemaSQL).use { createDatabase(it) }
        insertData()
    }

    @AfterEach
    fun afterEach() {
        noShutDownConnection?.close()
    }

    @Test
    fun `Can read tables`() {

        val tables = sqlModelReader.getTables(schemaSQL)
        Assertions.assertEquals(2, tables.size)
        val tablesLowerCase = tables.map { it.toLowerCase() }

        Assertions.assertTrue(tablesLowerCase.contains("table_test"))
        Assertions.assertTrue(tablesLowerCase.contains("table_other"))
    }

    @Test
    fun `Can read columns with basic types`() {

        val columns = getColumns("table_test")

        val columnByName = columns.map { it.name.toLowerCase() to it }.toMap()

        val col10 = columnByName["id"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        Assertions.assertFalse(col10.nullable)
        Assertions.assertTrue(col10.primaryKey)
        Assertions.assertTrue(col10.autoIncrement)

        val col11 = columnByName["string1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        Assertions.assertTrue(col11.nullable)
        Assertions.assertFalse(col11.primaryKey)
        Assertions.assertFalse(col11.autoIncrement)

        val col12 = columnByName["int1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col12.dataType)
        Assertions.assertTrue(col12.nullable)
        Assertions.assertFalse(col12.primaryKey)
        Assertions.assertFalse(col12.autoIncrement)

        val col13 = columnByName["float1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DOUBLE, col13.dataType)
        Assertions.assertTrue(col13.nullable)
        Assertions.assertFalse(col13.primaryKey)
        Assertions.assertFalse(col13.autoIncrement)

        val col14 = columnByName["bool1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.BOOLEAN, col14.dataType)
        Assertions.assertTrue(col14.nullable)
        Assertions.assertFalse(col14.primaryKey)
        Assertions.assertFalse(col14.autoIncrement)

        val col15 = columnByName["date1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DATE, col15.dataType)
        Assertions.assertTrue(col15.nullable)
        Assertions.assertFalse(col15.primaryKey)
        Assertions.assertFalse(col15.autoIncrement)

        val col16 = columnByName["time1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.TIME, col16.dataType)
        Assertions.assertTrue(col16.nullable)
        Assertions.assertFalse(col16.primaryKey)
        Assertions.assertFalse(col16.autoIncrement)

        val col17 = columnByName["datetime1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DATE_TIME, col17.dataType)
        Assertions.assertTrue(col17.nullable)
        Assertions.assertFalse(col17.primaryKey)
        Assertions.assertFalse(col17.autoIncrement)
    }

    @Test
    fun `Can read columns with not so common types`() {

        val columns = getColumns("table_other")

        val columnByName = columns.map { it.name.toLowerCase() to it }.toMap()

        val col20 = columnByName["id"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col20.dataType)
        Assertions.assertFalse(col20.nullable)
        Assertions.assertTrue(col20.primaryKey)
        Assertions.assertFalse(col20.autoIncrement)

        val col21 = columnByName["string1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col21.dataType)
        Assertions.assertTrue(col21.nullable)
        Assertions.assertFalse(col21.primaryKey)
        Assertions.assertFalse(col21.autoIncrement)

        val col22 = columnByName["int1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col22.dataType)
        Assertions.assertFalse(col22.nullable)
        Assertions.assertFalse(col22.primaryKey)
        Assertions.assertFalse(col22.autoIncrement)

        val col23 = columnByName["int2"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col23.dataType)
        Assertions.assertTrue(col23.nullable)
        Assertions.assertFalse(col23.primaryKey)
        Assertions.assertFalse(col23.autoIncrement)

        val col24 = columnByName["int3"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col24.dataType)
        Assertions.assertTrue(col24.nullable)
        Assertions.assertFalse(col24.primaryKey)
        Assertions.assertFalse(col24.autoIncrement)

        val col25 = columnByName["float1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DOUBLE, col25.dataType)
        Assertions.assertTrue(col25.nullable)
        Assertions.assertFalse(col25.primaryKey)
        Assertions.assertFalse(col25.autoIncrement)

        val col26 = columnByName["float2"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DOUBLE, col26.dataType)
        Assertions.assertTrue(col26.nullable)
        Assertions.assertFalse(col26.primaryKey)
        Assertions.assertFalse(col26.autoIncrement)

        val col27 = columnByName["datetime1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DATE_TIME, col27.dataType)
        Assertions.assertTrue(col27.nullable)
        Assertions.assertFalse(col27.primaryKey)
        Assertions.assertFalse(col27.autoIncrement)
    }

    // ----------------------------------------------------------
    //                   UTILITY FUNCTION
    // ----------------------------------------------------------

    private fun getColumns(tableName: String): List<TableColumnMeta> {

        val tables = sqlModelReader.getTables(schemaSQL)
        Assertions.assertEquals(2, tables.size)
        val table = tables.firstOrNull { it.toLowerCase() == tableName.toLowerCase() } ?: Assertions.fail()
        return sqlModelReader.getColumnsFromTable(schemaSQL, table)
    }

    private fun createDatabase(connection: Connection) {

        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE IF EXISTS table_test")
        }

        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE table_test( " +
                    "id SERIAL, " +
                    "string1 VARCHAR, " +
                    "int1 INTEGER, " +
                    "float1 FLOAT, " +
                    "bool1 BOOLEAN, " +
                    "date1 DATE, " +
                    "time1 TIME, " +
                    "datetime1 TIMESTAMP WITH TIME ZONE " +
                    ")")
        }

        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE IF EXISTS table_other")
        }

        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE table_other( " +
                    "id INT PRIMARY KEY, " +
                    "string1 CLOB, " +
                    "int1 TINYINT NOT NULL, " +
                    "int2 SMALLINT, " +
                    "int3 BIGINT, " +
                    "float1 DECIMAL, " +
                    "float2 REAL, " +
                    "datetime1 TIMESTAMP " +
                    ")")
        }
    }

    private fun insertData() {
        sqlDataWriter.insertData(
            getBasicDataProvider(),
            mapOf<String, Any>(
                "string1" to "string1",
                "int1" to 1,
                "float1" to 1.5,
                "bool1" to true,
                "date1" to LocalDate.parse("2025-12-25"),
                "time1" to LocalTime.parse("13:00:25"),
                "datetime1" to OffsetDateTime.parse("1976-09-07T16:33:22Z")))

        sqlDataWriter.insertData(
            getBasicDataProvider(),
            mapOf<String, Any>(
                "string1" to "string2",
                "int1" to 2,
                "float1" to 2.5,
                "bool1" to true,
                "date1" to LocalDate.parse("2026-12-25"),
                "time1" to LocalTime.parse("14:00:25"),
                "datetime1" to OffsetDateTime.parse("1977-09-07T16:33:22Z")))

        sqlDataWriter.insertData(
            getBasicDataProvider(),
            mapOf<String, Any>(
                "string1" to "string3",
                "int1" to 3,
                "float1" to 3.5,
                "bool1" to false,
                "date1" to LocalDate.parse("2027-12-25"),
                "time1" to LocalTime.parse("13:00:25"),
                "datetime1" to OffsetDateTime.parse("1976-09-07T16:33:22Z")))

    }

    private fun getBasicDataProvider(): DataProviderSQL {
        return DataProviderSQL(
            "dataProviderId",
            "schemaId",
            "data provider name",
            listOf(
                ColumnAttribute(
                    "id",
                    DataType.LONG,
                    4,
                    WritableStorage("table_test", "id", "id", false, true, true)),
                ColumnAttribute(
                    "string1",
                    DataType.STRING,
                    4,
                    WritableStorage("table_test", "string1", "string1", true, false, false)),
                ColumnAttribute(
                    "int1",
                    DataType.LONG,
                    4,
                    WritableStorage("table_test", "int1", "int1", true, false, false)),
                ColumnAttribute(
                    "float1",
                    DataType.DOUBLE,
                    4,
                    WritableStorage("table_test", "float1", "float1", true, false, false)),
                ColumnAttribute(
                    "bool1",
                    DataType.BOOLEAN,
                    4,
                    WritableStorage("table_test", "bool1", "bool1", true, false, false)),
                ColumnAttribute(
                    "date1",
                    DataType.DATE,
                    4,
                    WritableStorage("table_test", "date1", "date1", true, false, false)),
                ColumnAttribute(
                    "time1",
                    DataType.TIME,
                    4,
                    WritableStorage("table_test", "time1", "time1", true, false, false)),
                ColumnAttribute(
                    "datetime1",
                    DataType.DATE_TIME,
                    4,
                    WritableStorage("table_test", "datetime1", "datetime1", true, false, false))),
            true,
            query = "SELECT * FROM table_test")
    }
}