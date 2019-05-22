package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.entity.query.ColumnName
import net.wuillemin.jds.dataserver.entity.query.Equal
import net.wuillemin.jds.dataserver.entity.query.Order
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
class SQLDataWriterTest {

    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(SQLDataWriterTest::class.java)
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
    }

    @AfterEach
    fun afterEach() {
        noShutDownConnection?.close()
    }

    // ----------------------------------------------------------
    //                   INSERT
    // ----------------------------------------------------------

    @Test
    fun `Can insert valid data`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.parse("2025-12-25"), "time1" to LocalTime.parse("13:00:25"), "datetime1" to OffsetDateTime.parse("1976-09-07T16:33:22Z"))
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id")))
        Assertions.assertEquals(1, results.size)

        val result = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result["id"])
        Assertions.assertEquals("string1", result["string1"])
        Assertions.assertEquals(23L, result["int1"])
        Assertions.assertEquals(1.5, result["float1"])
        Assertions.assertEquals(true, result["bool1"])
        Assertions.assertEquals(LocalDate.parse("2025-12-25"), result["date1"])
        Assertions.assertEquals(LocalTime.parse("13:00:25"), result["time1"])
        Assertions.assertEquals(OffsetDateTime.parse("1976-09-07T16:33:22Z"), result["datetime1"])
    }

    @Test
    fun `Can not insert in non editable`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        Assertions.assertThrows(BadParameterException::class.java) { sqlDataWriter.insertData(getNonEditableDataProvider(), data) }
    }

    @Test
    fun `Can not insert with invalid attribute`() {

        val data = mapOf<String, Any>("not_existing" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.parse("2025-12-25"), "time1" to LocalTime.parse("13:00:25"), "datetime1" to OffsetDateTime.parse("1976-09-07T16:33:22Z"))
        Assertions.assertThrows(ConstraintException::class.java) { sqlDataWriter.insertData(getBasicDataProvider(), data) }
    }

    @Test
    fun `Insert nothing if overwrite autoincrement`() {

        val data = mapOf<String, Any>("id" to 33, "string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        val numberInserted = sqlDataWriter.insertData(getBasicDataProvider(), data)
        Assertions.assertEquals(0, numberInserted)
    }

    @Test
    fun `Insert nothing if mandatory data not defined`() {

        val data = mapOf<String, Any>("string1" to "string1")
        val numberInserted = sqlDataWriter.insertData(getAllMandatoryDataProvider(), data)
        Assertions.assertEquals(0, numberInserted)
    }

    @Test
    fun `Insert nothing if missing pk`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        val numberInserted = sqlDataWriter.insertData(getPrimaryKeyToBeProvidedDataProvider(), data)
        Assertions.assertEquals(0, numberInserted)
    }

    // ----------------------------------------------------------
    //                   MASS INSERT
    // ----------------------------------------------------------

    @Test
    fun `Can mass insert valid data`() {

        val data = listOf(
            mapOf<String, Any>("string1" to "string1"),
            mapOf<String, Any>("int1" to 23))

        sqlDataWriter.massInsertData(getBasicDataProvider(), data)

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id")))
        Assertions.assertEquals(2, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result1["id"])
        Assertions.assertEquals("string1", result1["string1"])
        Assertions.assertNull(result1["int1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result2["id"])
        Assertions.assertNull(result2["string1"])
        Assertions.assertEquals(23L, result2["int1"])
    }

    @Test
    fun `Can not mass insert in non editable`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        Assertions.assertThrows(BadParameterException::class.java) { sqlDataWriter.massInsertData(getNonEditableDataProvider(), listOf(data)) }
    }

    @Test
    fun `Can not mass insert with invalid attribute`() {

        val data = listOf(
            mapOf<String, Any>("not_existing_column" to "string1"),
            mapOf<String, Any>("int1" to 23))

        Assertions.assertThrows(ConstraintException::class.java) { sqlDataWriter.massInsertData(getBasicDataProvider(), data) }
    }

    @Test
    fun `Mass insert nothing if overwrite autoincrement`() {

        val data = listOf(
            mapOf<String, Any>("id" to 1),
            mapOf<String, Any>("int1" to 23))

        val numberInserted = sqlDataWriter.massInsertData(getBasicDataProvider(), data)
        Assertions.assertEquals(1, numberInserted)
    }

    @Test
    fun `Mass insert nothing if missing pk`() {

        val data = listOf(
            mapOf<String, Any>("string1" to "string1"),
            mapOf<String, Any>("int1" to 23))

        val numberInserted = sqlDataWriter.massInsertData(getPrimaryKeyToBeProvidedDataProvider(), data)
        Assertions.assertEquals(0, numberInserted)

    }

    // ----------------------------------------------------------
    //                   UPDATE
    // ----------------------------------------------------------

    @Test
    fun `Can update valid data`() {

        val data1 = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data1)
        val data2 = mapOf<String, Any>("string1" to "string2", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data2)

        // Update
        sqlDataWriter.updateData(getBasicDataProvider(), Equal(ColumnName("string1"), Value("string1")), mapOf<String, Any?>("int1" to 24))

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id")))
        Assertions.assertEquals(2, results.size)

        val result1 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result1["id"])
        Assertions.assertEquals("string1", result1["string1"])
        Assertions.assertEquals(24L, result1["int1"])

        val result2 = results[1].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result2["id"])
        Assertions.assertEquals("string2", result2["string1"])
        Assertions.assertEquals(23L, result2["int1"])
    }

    @Test
    fun `Can not update in non editable`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        Assertions.assertThrows(BadParameterException::class.java) {
            sqlDataWriter.updateData(getNonEditableDataProvider(), Equal(ColumnName("string1"), Value("string1")), mapOf<String, Any?>("int1" to null))
        }
    }

    @Test
    fun `Can not update with invalid attribute`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        Assertions.assertThrows(ConstraintException::class.java) {
            sqlDataWriter.updateData(getBasicDataProvider(), Equal(ColumnName("string1"), Value("string1")), mapOf<String, Any?>("not_existing" to null))
        }
    }

    @Test
    fun `Update nothing if overwrite autoincrement field`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        val numberUpdated = sqlDataWriter.updateData(getBasicDataProvider(), Equal(ColumnName("string1"), Value("string1")), mapOf<String, Any?>("id" to 24))
        Assertions.assertEquals(0, numberUpdated)
    }

    @Test
    fun `Update nothing if null non nullable fields`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        // Update
        val numberUpdated = sqlDataWriter.updateData(getAllMandatoryDataProvider(), Equal(ColumnName("string1"), Value("string1")), mapOf<String, Any?>("int1" to null))
        Assertions.assertEquals(0, numberUpdated)
    }

    // ----------------------------------------------------------
    //                   DELETE
    // ----------------------------------------------------------

    @Test
    fun `Can delete valid data`() {

        val data1 = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data1)
        val data2 = mapOf<String, Any>("string1" to "string2", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data2)

        // Delete
        sqlDataWriter.deleteData(getBasicDataProvider(), Equal(ColumnName("string1"), Value("string1")))

        val results = sqlDataReader.getData(getBasicDataProvider(), null, listOf(Order("id")))
        Assertions.assertEquals(1, results.size)

        val result2 = results[0].entries.map { entry -> entry.key.toLowerCase() to entry.value }.toMap()
        Assertions.assertNotNull(result2["id"])
        Assertions.assertEquals("string2", result2["string1"])
    }

    @Test
    fun `Can not delete in non editable`() {

        val data = mapOf<String, Any>("string1" to "string1", "int1" to 23, "float1" to 1.5, "bool1" to true, "date1" to LocalDate.now(), "time1" to LocalTime.now(), "datetime1" to OffsetDateTime.now())
        sqlDataWriter.insertData(getBasicDataProvider(), data)

        Assertions.assertThrows(BadParameterException::class.java) {
            sqlDataWriter.deleteData(getNonEditableDataProvider(), Equal(ColumnName("string1"), Value("string1")))
        }
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

    private fun getAllMandatoryDataProvider(): DataProviderSQL {
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
                    WritableStorage("table_test", "string1", "string1", false, false, false)),
                ColumnAttribute(
                    "int1",
                    DataType.LONG,
                    4,
                    WritableStorage("table_test", "int1", "int1", false, false, false)),
                ColumnAttribute(
                    "float1",
                    DataType.DOUBLE,
                    4,
                    WritableStorage("table_test", "float1", "float1", false, false, false)),
                ColumnAttribute(
                    "bool1",
                    DataType.BOOLEAN,
                    4,
                    WritableStorage("table_test", "bool1", "bool1", false, false, false)),
                ColumnAttribute(
                    "date1",
                    DataType.DATE,
                    4,
                    WritableStorage("table_test", "date1", "date1", false, false, false)),
                ColumnAttribute(
                    "time1",
                    DataType.TIME,
                    4,
                    WritableStorage("table_test", "time1", "time1", false, false, false)),
                ColumnAttribute(
                    "datetime1",
                    DataType.DATE_TIME,
                    4,
                    WritableStorage("table_test", "datetime1", "datetime1", false, false, false))),
            true,
            query = "SELECT * FROM table_test")
    }

    private fun getPrimaryKeyToBeProvidedDataProvider(): DataProviderSQL {
        return DataProviderSQL(
            DATA_PROVIDER_ID,
            "data provider name",
            SCHEMA_ID,
            listOf(
                ColumnAttribute(
                    "id",
                    DataType.LONG,
                    4,
                    WritableStorage("table_test", "id", "id", false, true, false)),
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

    private fun getNonEditableDataProvider(): DataProviderSQL {
        return DataProviderSQL(
            DATA_PROVIDER_ID,
            "data provider name",
            SCHEMA_ID,
            listOf(
                ColumnAttribute(
                    "id",
                    DataType.LONG,
                    4,
                    ReadOnlyStorage("id", false, true, true)),
                ColumnAttribute(
                    "string1",
                    DataType.STRING,
                    4,
                    ReadOnlyStorage("string1", true, false, false)),
                ColumnAttribute(
                    "int1",
                    DataType.LONG,
                    4,
                    ReadOnlyStorage("int1", true, false, false)),
                ColumnAttribute(
                    "float1",
                    DataType.DOUBLE,
                    4,
                    ReadOnlyStorage("float1", true, false, false)),
                ColumnAttribute(
                    "bool1",
                    DataType.BOOLEAN,
                    4,
                    ReadOnlyStorage("bool1", true, false, false)),
                ColumnAttribute(
                    "date1",
                    DataType.DATE,
                    4,
                    ReadOnlyStorage("date1", true, false, false)),
                ColumnAttribute(
                    "time1",
                    DataType.TIME,
                    4,
                    ReadOnlyStorage("time1", true, false, false)),
                ColumnAttribute(
                    "datetime1",
                    DataType.DATE_TIME,
                    4,
                    ReadOnlyStorage("datetime1", true, false, false))),
            false,
            query = "SELECT * FROM table_test")
    }
}