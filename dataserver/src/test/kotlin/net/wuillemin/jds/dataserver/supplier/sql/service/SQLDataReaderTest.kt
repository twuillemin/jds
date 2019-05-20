package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.entity.query.ColumnName
import net.wuillemin.jds.dataserver.entity.query.Equal
import net.wuillemin.jds.dataserver.entity.query.GreaterThanOrEqual
import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.entity.query.OrderDirection
import net.wuillemin.jds.dataserver.entity.query.Value
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLOrderConverter
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

// Definition of constants
private const val GROUP_ID = 1L
private const val SERVER_ID = 100L
private const val SCHEMA_ID = 200L
private const val DATA_PROVIDER_ID = 300L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLDataReaderTest {

    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(SQLDataReaderTest::class.java)
    private val jdbcHelper = JDBCHelper()
    private val sqlHelper = SQLHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlOrderConverter = SQLOrderConverter()
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlDataReader = SQLDataReader(schemaService, jdbcHelper, sqlHelper, sqlConnectionCache, sqlPredicateConverter, sqlOrderConverter, objectMapper, logger)

    private val serverSQL = ServerSQL(
        SERVER_ID,
        "testServer",
        GROUP_ID,
        true,
        "jdbc:h2:mem:",
        "sa",
        null,
        "org.h2.Driver")

    private val schemaSQL = SchemaSQL(
        SCHEMA_ID,
        "PUBLIC",
        GROUP_ID,
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
    fun `Can read data without option`() {

        val results = sqlDataReader.getData(getBasicDataProvider(), null, null)
        // As no order, just check 3 results
        Assertions.assertEquals(3, results.size)
    }

    @Test
    fun `Can read data with order default`() {

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id")))
        // As no order, just check 3 results
        Assertions.assertEquals(3, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result1["id"])
        Assertions.assertEquals("string1", result1["string1"])
        Assertions.assertEquals(1L, result1["int1"])
        Assertions.assertEquals(1.5, result1["float1"])
        Assertions.assertEquals(true, result1["bool1"])
        Assertions.assertEquals(LocalDate.parse("2025-12-25"), result1["date1"])
        Assertions.assertEquals(LocalTime.parse("13:00:25"), result1["time1"])
        Assertions.assertEquals(OffsetDateTime.parse("1976-09-07T16:33:22Z"), result1["datetime1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string2", result2["string1"])

        val result3 = results[2].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string3", result3["string1"])
    }

    @Test
    fun `Can read data with order asc`() {

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id", OrderDirection.ASCENDING)))
        // As no order, just check 3 results
        Assertions.assertEquals(3, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string1", result1["string1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string2", result2["string1"])

        val result3 = results[2].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string3", result3["string1"])
    }

    @Test
    fun `Can read data with order desc`() {

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id", OrderDirection.DESCENDING)))
        // As no order, just check 3 results
        Assertions.assertEquals(3, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string3", result1["string1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string2", result2["string1"])

        val result3 = results[2].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string1", result3["string1"])
    }

    @Test
    fun `Can filter data`() {

        val results = sqlDataReader.getData(getBasicDataProvider(), Equal(ColumnName("string1"), Value("string2")), null)
        // As no order, just check 3 results
        Assertions.assertEquals(1, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string2", result1["string1"])
    }

    @Test
    fun `Can filter data and order`() {

        val results = sqlDataReader.getData(
            getBasicDataProvider(),
            GreaterThanOrEqual(ColumnName("int1"), Value(2)),
            listOf(Order("id", OrderDirection.DESCENDING)))

        // As no order, just check 3 results
        Assertions.assertEquals(2, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string3", result1["string1"])
        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertEquals("string2", result2["string1"])
    }

    // ----------------------------------------------------------
    //                   UTILITY FUNCTION
    // ----------------------------------------------------------

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
            DATA_PROVIDER_ID,
            "data provider name",
            SCHEMA_ID,
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