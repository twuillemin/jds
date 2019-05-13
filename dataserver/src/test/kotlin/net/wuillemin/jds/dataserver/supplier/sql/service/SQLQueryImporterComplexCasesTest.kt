package net.wuillemin.jds.dataserver.supplier.sql.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.entity.query.Order
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLQueryImporterComplexCasesTest {

    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(SQLQueryImporterComplexCasesTest::class.java)
    private val jdbcHelper = JDBCHelper()
    private val sqlHelper = SQLHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlOrderConverter = SQLOrderConverter()
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlDataReader = SQLDataReader(schemaService, jdbcHelper, sqlHelper, sqlConnectionCache, sqlPredicateConverter, sqlOrderConverter, objectMapper, logger)
    private val sqlModelReader = SQLModelReader(jdbcHelper, sqlConnectionCache, schemaService)
    private val sqlQueryImporter = SQLQueryImporter(sqlHelper, sqlModelReader)

    private val serverSQL = ServerSQL(
        "serverId",
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
    }

    @AfterEach
    fun afterEach() {
        noShutDownConnection?.close()
    }

    // ----------------------------------------------------------
    //                   INSERT
    // ----------------------------------------------------------

    @Test
    fun `Can process joined table`() {

        val query = "select p.product_id as myId, p.product_name as name, UPPER(p.description), p.category_id, c.category_name from products as p, categories as c where p.category_id = c.category_id"

        val dataProviderImported = sqlQueryImporter.buildDataProviderFromQuery(
            schemaSQL,
            "name",
            query)

        Assertions.assertEquals("name", dataProviderImported.name)
        Assertions.assertEquals(schemaSQL.id, dataProviderImported.schemaId)
        Assertions.assertEquals(query, dataProviderImported.query)

        // Change the name of a column (just for adding a bit more complexity)
        val dataProvider = dataProviderImported.copy(
            columns = dataProviderImported.columns.map { column ->
                if (column.name == "p.category_id") {
                    (column as? ColumnAttribute)?.copy(name = "catid") ?: Assertions.fail()
                }
                else {
                    column
                }
            }
        )

        val columnByName = dataProvider.columns.map { it.name.toLowerCase() to it }.toMap()

        val col10 = columnByName["myid"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col10.dataType)
        val col10c = col10.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("products", col10c.containerName.toLowerCase())
        Assertions.assertEquals("myid", col10c.readAttributeName.toLowerCase())
        Assertions.assertEquals("product_id", col10c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col10c.nullable)
        Assertions.assertTrue(col10c.primaryKey)
        Assertions.assertTrue(col10c.autoIncrement)

        val col11 = columnByName["name"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col11.dataType)
        val col11c = col11.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("products", col11c.containerName.toLowerCase())
        Assertions.assertEquals("name", col11c.readAttributeName.toLowerCase())
        Assertions.assertEquals("product_name", col11c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col11c.nullable)
        Assertions.assertFalse(col11c.primaryKey)
        Assertions.assertFalse(col11c.autoIncrement)

        val col12 = columnByName["upper(p.description)"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col12.dataType)
        val col12c = col12.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("products", col12c.containerName.toLowerCase())
        Assertions.assertEquals("upper(p.description)", col12c.readAttributeName.toLowerCase())
        Assertions.assertEquals("description", col12c.writeAttributeName.toLowerCase())
        Assertions.assertTrue(col12c.nullable)
        Assertions.assertFalse(col12c.primaryKey)
        Assertions.assertFalse(col12c.autoIncrement)

        val col113 = columnByName["catid"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.LONG, col113.dataType)
        val col13c = col113.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("products", col13c.containerName.toLowerCase())
        Assertions.assertEquals("category_id", col13c.readAttributeName.toLowerCase())
        Assertions.assertEquals("category_id", col13c.writeAttributeName.toLowerCase())
        Assertions.assertTrue(col13c.nullable)
        Assertions.assertFalse(col13c.primaryKey)
        Assertions.assertFalse(col13c.autoIncrement)

        val col14 = columnByName["c.category_name"] ?: Assertions.fail()
        Assertions.assertEquals(DataType.STRING, col14.dataType)
        val col14c = col14.storageDetail as? WritableStorage ?: Assertions.fail()
        Assertions.assertEquals("categories", col14c.containerName.toLowerCase())
        Assertions.assertEquals("category_name", col14c.readAttributeName.toLowerCase())
        Assertions.assertEquals("category_name", col14c.writeAttributeName.toLowerCase())
        Assertions.assertFalse(col14c.nullable)
        Assertions.assertFalse(col14c.primaryKey)
        Assertions.assertFalse(col14c.autoIncrement)

        val columnNameProductId = columnByName["myid"]!!.name
        val columnNameProductName = columnByName["name"]!!.name
        val columnNameCategoryId = columnByName["catid"]!!.name
        val columnNameCategoryName = columnByName["c.category_name"]!!.name
        val columnNameDescription = columnByName["upper(p.description)"]!!.name

        // Check the mass insert
        val data: List<Map<String, Any>> = listOf(
            // Insert a new category
            mapOf(columnNameCategoryName to "new category"),
            // Insert a new product
            mapOf(columnNameProductName to "new product name", columnNameDescription to "desc", columnNameCategoryId to 2))

        sqlDataWriter.massInsertData(dataProvider, data)

        val results = sqlDataReader.getData(dataProvider, null, listOf(Order(columnNameProductId)))
        Assertions.assertEquals(2, results.size)

        val newLine = results[1]
        Assertions.assertEquals(2L, newLine[columnNameProductId])
        Assertions.assertEquals("new product name", newLine[columnNameProductName])
        Assertions.assertEquals("DESC", newLine[columnNameDescription])
        Assertions.assertEquals(2L, newLine[columnNameCategoryId])
        Assertions.assertEquals("new category", newLine[columnNameCategoryName])
    }

    @Test
    fun `Can not process joined table if columns are duplicated`() {

        val query = "select * from products as p, categories as c where p.category_id = c.category_id"

        Assertions.assertThrows(BadParameterException::class.java) { sqlQueryImporter.buildDataProviderFromQuery(schemaSQL, "name", query) }
    }


    // ----------------------------------------------------------
    //                   UTILITY FUNCTION
    // ----------------------------------------------------------

    private fun createDatabase(connection: Connection) {

        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE IF EXISTS products")
        }

        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE IF EXISTS categories")
        }

        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE categories(category_id SERIAL, category_name VARCHAR NOT NULL)")
        }

        connection.createStatement().use { statement ->
            statement.execute("CREATE TABLE products(product_id SERIAL, product_name VARCHAR NOT NULL, description VARCHAR, category_id INTEGER)")
        }

        connection.createStatement().use { statement ->
            statement.execute("INSERT INTO categories(category_name) VALUES ('category1')")
        }

        connection.createStatement().use { statement ->
            statement.execute("INSERT INTO products(product_name, description, category_id) VALUES ('product1', 'description1', 1)")
        }
    }
}