package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
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
class SQLQueryImporterBasicCasesTest {

    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(SQLQueryImporterBasicCasesTest::class.java)
    private val jdbcHelper = JDBCHelper()
    private val sqlHelper = SQLHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlModelReader = SQLModelReader(jdbcHelper, sqlConnectionCache, schemaService)
    private val sqlQueryImporter = SQLQueryImporter(sqlHelper, sqlModelReader)

    private val GROUP_ID = 1L
    private val SERVER_ID = 100L
    private val SCHEMA_ID = 200L
    private val DATA_PROVIDER_ID = 300L

    private val serverSQL = ServerSQL(
        SERVER_ID,
        "testServer",
        GROUP_ID,
        true,
        "jdbc:h2:mem:",
        "sa",
        null)

    private val schemaSQL = SchemaSQL(
        SCHEMA_ID,
        "PUBLIC",
        SERVER_ID,
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
    fun `Can build data provider`() {

        val query = "select * from table_test"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name.toLowerCase() to it }.toMap()

        val col10 = columnByName["id"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val col11 = columnByName["string1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        val col11c = col11.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)

        val col12 = columnByName["int1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col12.dataType)
        val col12c = col12.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col12c.nullable)
        Assertions.assertFalse(col12c.primaryKey)
        Assertions.assertFalse(col12c.autoIncrement)

        val col13 = columnByName["float1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DOUBLE, col13.dataType)
        val col13c = col13.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col13c.nullable)
        Assertions.assertFalse(col13c.primaryKey)
        Assertions.assertFalse(col13c.autoIncrement)

        val col14 = columnByName["bool1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.BOOLEAN, col14.dataType)
        val col14c = col14.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col14c.nullable)
        Assertions.assertFalse(col14c.primaryKey)
        Assertions.assertFalse(col14c.autoIncrement)

        val col15 = columnByName["date1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DATE, col15.dataType)
        val col15c = col15.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col15c.nullable)
        Assertions.assertFalse(col15c.primaryKey)
        Assertions.assertFalse(col15c.autoIncrement)

        val col16 = columnByName["time1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.TIME, col16.dataType)
        val col16c = col16.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col16c.nullable)
        Assertions.assertFalse(col16c.primaryKey)
        Assertions.assertFalse(col16c.autoIncrement)

        val col17 = columnByName["datetime1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.DATE_TIME, col17.dataType)
        val col17c = col17.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertTrue(col17c.nullable)
        Assertions.assertFalse(col17c.primaryKey)
        Assertions.assertFalse(col17c.autoIncrement)
    }

    @Test
    fun `Can build data provider with table aliased`() {

        val query = "select t.id from table_test as t"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name.toLowerCase() to it }.toMap()

        val col10 = columnByName["t.id"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col10c.containerName.toLowerCase())
        // When aliasing is simple, it may be removed in the database results, so reading is not t.id, but only id
        Assertions.assertEquals("id", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)
    }

    @Test
    fun `Can build data provider on aliased query`() {

        val query = "select t.id as myId, t.string1 as MyString1 from table_test as t"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name.toLowerCase() to it }.toMap()

        val col10 = columnByName["myid"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col10c.containerName.toLowerCase())
        Assertions.assertEquals("myid", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val col11 = columnByName["mystring1"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        val col11c = col11.storageDetail  as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col11c.containerName.toLowerCase())
        Assertions.assertEquals("mystring1", col11c.readAttributeName.toLowerCase())
        Assertions.assertEquals("string1", col11c.writeAttributeName.toLowerCase())
        Assertions.assertTrue(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)
    }

    @Test
    fun `Can build data provider on query with an operation`() {

        val query = "select t.id as myId, t.int1 + 1 as MyValue from table_test as t"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name to it }.toMap()

        val col10 = columnByName["myId"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col10c.containerName.toLowerCase())
        Assertions.assertEquals("myid", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val col11 = columnByName["MyValue"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col11.dataType)
        val col11c = col11.storageDetail  as? ReadOnlyStorage ?: Assertions.fail()
        Assertions.assertEquals("myvalue", col11c.readAttributeName.toLowerCase())
        Assertions.assertTrue(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)
    }

    @Test
    fun `Can build data provider on query with an aliased function`() {

        val query = "select t.id as myId, UPPER(t.string1) as MyValue from table_test as t"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name to it }.toMap()

        val col10 = columnByName["myId"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col10c.containerName.toLowerCase())
        Assertions.assertEquals("myid", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val col11 = columnByName["MyValue"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        val col11c = col11.storageDetail  as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("myvalue", col11c.readAttributeName.toLowerCase())
        Assertions.assertEquals("string1", col11c.writeAttributeName.toLowerCase())
        Assertions.assertTrue(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)
    }

    @Test
    fun `Can build data provider on query with a non-aliased function`() {

        val query = "select t.id as myId, UPPER(t.string1) from table_test as t"

        val dataProvider = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProvider.name)
        Assertions.assertEquals(serverSQL.id, dataProvider.schemaId)
        Assertions.assertEquals(query, dataProvider.query)

        val columnByName = dataProvider.columns.map { it.name to it }.toMap()

        val col10 = columnByName["myId"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("table_test", col10c.containerName.toLowerCase())
        Assertions.assertEquals("myid", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val otherColumnName = columnByName.keys.firstOrNull { it != "myId" } ?: Assertions.fail()
        val col11 = columnByName[otherColumnName] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        val col11c = col11.storageDetail  as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("upper(t.string1)", col11c.readAttributeName.toLowerCase())
        Assertions.assertEquals("string1", col11c.writeAttributeName.toLowerCase())
        Assertions.assertTrue(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)
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
            DATA_PROVIDER_ID,
            SCHEMA_ID,
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