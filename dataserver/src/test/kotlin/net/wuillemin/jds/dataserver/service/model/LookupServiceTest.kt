package net.wuillemin.jds.dataserver.service.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.service.LocalisationService
import net.wuillemin.jds.dataserver.dto.query.PromoteColumnToLookupQuery
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.service.access.DataAccessService
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLConnectionCache
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataWriter
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLOrderConverter
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLPredicateConverter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
class LookupServiceTest {

    // -------------------------------------------------------------
    // Definition of the mocks
    // -------------------------------------------------------------
    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)
    private var dataSourceService: DataSourceService = Mockito.mock(DataSourceService::class.java)
    private var dataProviderService: DataProviderService = Mockito.mock(DataProviderService::class.java)
    private var localisationService: LocalisationService = Mockito.mock(LocalisationService::class.java)

    // -------------------------------------------------------------
    // Definition of the services
    // -------------------------------------------------------------
    private val jdbcHelper = JDBCHelper()
    private val sqlHelper = SQLHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlOrderConverter = SQLOrderConverter()
    private val logger = LoggerFactory.getLogger(LookupServiceTest::class.java)
    private var objectMapper = ObjectMapper()
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlDataReader = SQLDataReader(schemaService, jdbcHelper, sqlHelper, sqlConnectionCache, sqlPredicateConverter, sqlOrderConverter, objectMapper, logger)
    private val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)
    private val lookupService: LookupService = LookupService(dataSourceService, dataProviderService, dataAccessService, localisationService, objectMapper)

    // -------------------------------------------------------------
    // Definition of objects (for mocked services)
    // -------------------------------------------------------------
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

    private val dataProviderLookup = DataProviderSQL(
        "lookupDPId",
        "schemaId",
        "lookupDPId",
        listOf(
            ColumnAttribute("ID", DataType.STRING, 200, ReadOnlyStorage("ID", false, false, false)),
            ColumnAttribute("KEY", DataType.STRING, 200, ReadOnlyStorage("KEY", false, false, false)),
            ColumnAttribute("VALUE", DataType.STRING, 200, ReadOnlyStorage("VALUE", false, false, false))),
        false,
        "SELECT * FROM TABLE_LOOKUP")

    private val dataProviderTest = DataProviderSQL(
        "testDPId",
        "schemaId",
        "lookupDPId",
        listOf(
            ColumnAttribute(
                "ID",
                DataType.STRING,
                200, WritableStorage(
                "table_test",
                "id",
                "id",
                false,
                true,
                true)),
            ColumnAttribute(
                "DATA",
                DataType.STRING,
                200,
                WritableStorage(
                    "table_test",
                    "data",
                    "data",
                    true,
                    false,
                    false))),
        true,
        "SELECT * FROM table_test")

    private val dataSourceLookup = DataSource(
        "lookupDSId",
        "lookupDPId",
        "lookup data source",
        emptySet(),
        emptySet(),
        emptySet())

    // -------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------

    @BeforeEach
    fun beforeEach() {
        whenever(serverService.getServerById(any())).thenReturn(serverSQL)
        whenever(schemaService.getSchemaById(any())).thenReturn(schemaSQL)
    }

    @AfterEach
    fun afterEach() {
    }

    @Test
    fun `Can promote a column to lookupnad use it`() {

        sqlConnectionCache.getConnection(schemaSQL).use { connection ->

            connection.createStatement().use { it.execute("DROP TABLE IF EXISTS TABLE_LOOKUP") }
            connection.createStatement().use { it.execute("CREATE TABLE TABLE_LOOKUP(id SERIAL, key VARCHAR, value VARCHAR)") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_LOOKUP(key, value) VALUES ('C1', 'Red')") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_LOOKUP(key, value) VALUES ('C2', 'Green')") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_LOOKUP(key, value) VALUES ('C3', 'Blue')") }
            connection.createStatement().use { it.execute("DROP TABLE IF EXISTS TABLE_TEST") }
            connection.createStatement().use { it.execute("CREATE TABLE TABLE_TEST(id SERIAL, data VARCHAR)") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_TEST(data) VALUES ('Red, Green')") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_TEST(data) VALUES ('Green')") }
            connection.createStatement().use { it.execute("INSERT INTO TABLE_TEST(data) VALUES ('')") }

            connection.commit()
        }

        whenever(dataProviderService.getDataProviderById("lookupDPId")).thenReturn(dataProviderLookup)

        whenever(dataProviderService.getDataProviderById("testDPId")).thenReturn(dataProviderTest)

        whenever(dataSourceService.getDataSourceById("lookupDSId")).thenReturn(dataSourceLookup)

        whenever(dataProviderService.updateDataProvider(any())).doAnswer { params -> params.getArgument(0) as DataProvider }

        val updatedDataProvider = lookupService.promoteColumnToLookup(
            dataProviderService.getDataProviderById("testDPId"),
            PromoteColumnToLookupQuery(
                "DATA",
                5,
                "lookupDSId",
                "KEY",
                "VALUE"),
            Locale.getDefault())

        val data = dataAccessService.getData(updatedDataProvider)
        Assertions.assertEquals(3, data.size)
    }
}