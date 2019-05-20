package net.wuillemin.jds.dataserver.service.importation

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.query.Order
import net.wuillemin.jds.dataserver.service.access.DataAccessService
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.ModelService
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.service.model.ServerService
import net.wuillemin.jds.dataserver.service.query.PredicateContextBuilder
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLConnectionCache
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataWriter
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelWriter
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLQueryImporter
import net.wuillemin.jds.dataserver.supplier.sql.util.JDBCHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLHelper
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLOrderConverter
import net.wuillemin.jds.dataserver.supplier.sql.util.SQLPredicateConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

// Definition of constants
private const val GROUP_ID = 1L
private const val SERVER_ID = 100L
private const val SCHEMA_ID = 200L
private const val DATA_PROVIDER_ID = 300L
private const val DATA_SOURCE_ID = 400L

class CSVImporterTest {

    // -------------------------------------------------------------
    // Definition of the mocks
    // -------------------------------------------------------------
    private val serverService = Mockito.mock(ServerService::class.java)
    private val schemaService = Mockito.mock(SchemaService::class.java)
    private var dataSourceService: DataSourceService = Mockito.mock(DataSourceService::class.java)
    private var dataProviderService: DataProviderService = Mockito.mock(DataProviderService::class.java)

    // -------------------------------------------------------------
    // Definition of the services
    // -------------------------------------------------------------
    private val jdbcHelper = JDBCHelper()
    private val sqlHelper = SQLHelper()
    private val predicateContextBuilder = PredicateContextBuilder()
    private val sqlPredicateConverter = SQLPredicateConverter(predicateContextBuilder)
    private val sqlOrderConverter = SQLOrderConverter()
    private val logger = LoggerFactory.getLogger(CSVImporterTest::class.java)
    private var objectMapper = ObjectMapper()
    private val sqlConnectionCache = SQLConnectionCache(serverService, logger)
    private val sqlModelReader = SQLModelReader(jdbcHelper, sqlConnectionCache, schemaService)
    private val sqlModelWriter = SQLModelWriter(sqlConnectionCache)
    private val sqlQueryImporter = SQLQueryImporter(sqlHelper, sqlModelReader)
    private val sqlDataWriter = SQLDataWriter(schemaService, sqlPredicateConverter, sqlConnectionCache, objectMapper, logger)
    private val sqlDataReader = SQLDataReader(schemaService, jdbcHelper, sqlHelper, sqlConnectionCache, sqlPredicateConverter, sqlOrderConverter, objectMapper, logger)
    private val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)
    private val modelService = ModelService(sqlModelReader, sqlModelWriter, sqlQueryImporter, dataProviderService)
    private val csvModelReader = CSVModelReader()
    private val csvImporter = CSVImporter(csvModelReader, modelService, dataProviderService, dataSourceService, dataAccessService)

    // -------------------------------------------------------------
    // Definition of objects (for mocked services)
    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------

    @BeforeEach
    fun beforeEach() {
        whenever(serverService.getServerById(any())).thenReturn(serverSQL)
        whenever(schemaService.getSchemaById(any())).thenReturn(schemaSQL)
    }

    @Test
    fun `Import simpleCSV`() {

        // Fake save of the DataProvider
        whenever(dataProviderService.addDataProvider(any())).then { invocation ->
            val src = invocation.getArgument(0) as DataProviderSQL
            val newDataProvider = src.copy(id = DATA_PROVIDER_ID)
            whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_ID))).thenReturn(newDataProvider)
            newDataProvider
        }

        // Fake save of the DataSource
        whenever(dataSourceService.addDataSource(any())).then { invocation ->
            val src = invocation.getArgument(0) as DataSource
            val newDataSource = src.copy(id = DATA_SOURCE_ID)
            whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_ID))).thenReturn(newDataSource)
            newDataSource
        }

        val csvData = "stringValue,longValue,doubleValue,booleanValue,dateValue,timeValue,datetimeValue,stringNullValue,longNullValue,doubleNullValue,booleanNullValue,dateNullValue,timeNullValue,datetimeNullValue\n" +
            "abc   ,5 ,5  ,true ,2018-05-10,10:00:01,2018-10-21T10:00:01Z,abc def,5,5.2,true,2018-05-10,10:00:01,2018-10-21T10:00:01Z\n" +
            "abcdef,5 ,5.2,false,2018-05-11,10:00:02,2018-10-21T10:00:01Z,,,,,,,\n" +
            "abc,6 ,5.3,true ,2018-05-12,10:00:03,2018-10-21T10:00:01Z,abc def,5,5.2,true,2018-05-10,10:00:01,2018-10-21T10:00:01Z\n"

        val dataSource = csvImporter.autoImportCSV(schemaSQL, "table_test", csvData)

        Assertions.assertEquals("table_test", dataSource.name)

        val dataRead = dataAccessService.getData(dataSource, orders = listOf(Order("doubleValue")))

        Assertions.assertEquals(3, dataRead.size)

        val line1 = dataRead[0]
        Assertions.assertEquals("abc   ", line1["stringValue"])
        Assertions.assertEquals(5L, line1["longValue"])
        Assertions.assertEquals(5.0, line1["doubleValue"])
        Assertions.assertEquals(true, line1["booleanValue"])
        Assertions.assertEquals(LocalDate.parse("2018-05-10"), line1["dateValue"])
        Assertions.assertEquals(LocalTime.parse("10:00:01"), line1["timeValue"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-10-21T10:00:01Z"), line1["datetimeValue"])

        Assertions.assertEquals("abc def", line1["stringNullValue"])
        Assertions.assertEquals(5L, line1["longNullValue"])
        Assertions.assertEquals(5.2, line1["doubleNullValue"])
        Assertions.assertEquals(true, line1["booleanNullValue"])
        Assertions.assertEquals(LocalDate.parse("2018-05-10"), line1["dateNullValue"])
        Assertions.assertEquals(LocalTime.parse("10:00:01"), line1["timeNullValue"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-10-21T10:00:01Z"), line1["datetimeNullValue"])

        val line2 = dataRead[1]
        Assertions.assertEquals("abcdef", line2["stringValue"])
        Assertions.assertEquals(5L, line2["longValue"])
        Assertions.assertEquals(5.2, line2["doubleValue"])
        Assertions.assertEquals(false, line2["booleanValue"])
        Assertions.assertEquals(LocalDate.parse("2018-05-11"), line2["dateValue"])
        Assertions.assertEquals(LocalTime.parse("10:00:02"), line2["timeValue"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-10-21T10:00:01Z"), line2["datetimeValue"])

        Assertions.assertNull(line2["stringNullValue"])
        Assertions.assertNull(line2["longNullValue"])
        Assertions.assertNull(line2["doubleNullValue"])
        Assertions.assertNull(line2["booleanNullValue"])
        Assertions.assertNull(line2["dateNullValue"])
        Assertions.assertNull(line2["timeNullValue"])
        Assertions.assertNull(line2["datetimeNullValue"])

        val line3 = dataRead[2]
        Assertions.assertEquals("abc", line3["stringValue"])
        Assertions.assertEquals(6L, line3["longValue"])
        Assertions.assertEquals(5.3, line3["doubleValue"])
        Assertions.assertEquals(true, line3["booleanValue"])
        Assertions.assertEquals(LocalDate.parse("2018-05-12"), line3["dateValue"])
        Assertions.assertEquals(LocalTime.parse("10:00:03"), line3["timeValue"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-10-21T10:00:01Z"), line3["datetimeValue"])

        Assertions.assertEquals("abc def", line3["stringNullValue"])
        Assertions.assertEquals(5L, line3["longNullValue"])
        Assertions.assertEquals(5.2, line3["doubleNullValue"])
        Assertions.assertEquals(true, line3["booleanNullValue"])
        Assertions.assertEquals(LocalDate.parse("2018-05-10"), line3["dateNullValue"])
        Assertions.assertEquals(LocalTime.parse("10:00:01"), line3["timeNullValue"])
        Assertions.assertEquals(OffsetDateTime.parse("2018-10-21T10:00:01Z"), line3["datetimeNullValue"])

    }
}