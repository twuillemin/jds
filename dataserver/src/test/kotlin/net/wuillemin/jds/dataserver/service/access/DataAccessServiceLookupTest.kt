package net.wuillemin.jds.dataserver.service.access

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLConnectionCache
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class DataAccessServiceLookupTest {


    private var schemaService = Mockito.mock(SchemaService::class.java)
    private var dataSourceService = Mockito.mock(DataSourceService::class.java)
    private var dataProviderService = Mockito.mock(DataProviderService::class.java)
    private var sqlDataReader = Mockito.mock(SQLDataReader::class.java)
    private var sqlDataWriter = Mockito.mock(SQLDataWriter::class.java)
    private var sqlConnectionCache = Mockito.mock(SQLConnectionCache::class.java)


    private val SCHEMA_ID = 200L
    private val DATA_PROVIDER_LOOKUP_ID = 300L
    private val DATA_PROVIDER_TEST_ID = 301L
    private val DATA_SOURCE_LOOKUP_ID = 400L
    private val DATA_SOURCE_TEST_ID = 401L


    private val dataProviderLookup = DataProviderSQL(
        id = DATA_PROVIDER_LOOKUP_ID,
        schemaId = SCHEMA_ID,
        name = "The lookup data provider",
        columns = listOf(
            ColumnAttribute("key", DataType.STRING, 100, ReadOnlyStorage("key", false, false, false)),
            ColumnAttribute("value1", DataType.STRING, 100, ReadOnlyStorage("value1", false, false, false)),
            ColumnAttribute("value2", DataType.STRING, 100, ReadOnlyStorage("value2", false, false, false))),
        editable = false,
        query = "select * from lookup")

    private val dataSourceLookup = DataSource(
        id = DATA_SOURCE_LOOKUP_ID,
        name = "The lookup data source",
        dataProviderId = DATA_PROVIDER_LOOKUP_ID,
        userAllowedToReadIds = emptySet(),
        userAllowedToWriteIds = emptySet(),
        userAllowedToDeleteIds = emptySet())

    // The data provider to write
    private val dataProviderTest = DataProviderSQL(
        id = DATA_PROVIDER_TEST_ID,
        schemaId = SCHEMA_ID,
        name = "The data provider to test",
        columns = listOf(
            ColumnAttribute(
                "id",
                DataType.LONG,
                24,
                WritableStorage(
                    "test_table",
                    "id",
                    "id",
                    false,
                    true,
                    true)),
            ColumnLookup(
                "col1",
                DataType.LONG,
                24,
                WritableStorage(
                    "test_table",
                    "id",
                    "id",
                    false,
                    true,
                    true),
                1,
                dataSourceLookup.id!!,
                "key",
                "value1"),
            ColumnLookup(
                "col2",
                DataType.LONG,
                24,
                WritableStorage(
                    "test_table",
                    "id",
                    "id",
                    false,
                    true,
                    true),
                1,
                dataSourceLookup.id!!,
                "key",
                "value2")),
        editable = true,
        query = "select * from test_table")

    // The data source to write
    private val dataSourceTest = DataSource(
        id = DATA_SOURCE_TEST_ID,
        name = "Data source Test",
        dataProviderId = DATA_PROVIDER_TEST_ID,
        userAllowedToReadIds = emptySet(),
        userAllowedToWriteIds = emptySet(),
        userAllowedToDeleteIds = emptySet())

    @Test
    fun `With correct lookups, the insert can be run`() {

        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_LOOKUP_ID))).thenReturn(dataSourceLookup)
        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_TEST_ID))).thenReturn(dataSourceTest)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_LOOKUP_ID))).thenReturn(dataProviderLookup)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_TEST_ID))).thenReturn(dataProviderTest)

        // The lookup data
        whenever(sqlDataReader.getData(any(), isNull(), isNull(), isNull(), isNull())).thenReturn(
            listOf(
                mapOf("key" to "key_0", "value1" to "value1_0", "value2" to "value2_0"),
                mapOf("key" to "key_1", "value1" to "value1_1", "value2" to "value2_1")))

        // The service
        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        dataAccessService.insertData(dataSourceTest, mapOf("col1" to listOf("key_0"), "col2" to listOf("key_1")))
    }

    @Test
    fun `Lookup must be provided as list`() {

        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_LOOKUP_ID))).thenReturn(dataSourceLookup)
        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_TEST_ID))).thenReturn(dataSourceTest)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_LOOKUP_ID))).thenReturn(dataProviderLookup)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_TEST_ID))).thenReturn(dataProviderTest)

        // The lookup data
        whenever(sqlDataReader.getData(any(), isNull(), isNull(), isNull(), isNull())).thenReturn(
            listOf(
                mapOf("key" to "key_0", "value1" to "value1_0", "value2" to "value2_0"),
                mapOf("key" to "key_1", "value1" to "value1_1", "value2" to "value2_1")))

        // The service
        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        val exception = Assertions.assertThrows(BadParameterException::class.java) {
            dataAccessService.insertData(dataSourceTest, mapOf("col1" to listOf("key_0"), "col2" to "key_1"))
        }
        Assertions.assertEquals(E.service.model.lookup.badValueDataTypeForColumn, exception.code)
    }

    @Test
    fun `Lookup must exist`() {

        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_LOOKUP_ID))).thenReturn(dataSourceLookup)
        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_TEST_ID))).thenReturn(dataSourceTest)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_LOOKUP_ID))).thenReturn(dataProviderLookup)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_TEST_ID))).thenReturn(dataProviderTest)

        // The lookup data
        whenever(sqlDataReader.getData(any(), isNull(), isNull(), isNull(), isNull())).thenReturn(
            listOf(
                mapOf("key" to "key_0", "value1" to "value1_0", "value2" to "value2_0"),
                mapOf("key" to "key_1", "value1" to "value1_1", "value2" to "value2_1")))

        // The service
        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        val exception = Assertions.assertThrows(BadParameterException::class.java) {
            dataAccessService.insertData(dataSourceTest, mapOf("col1" to listOf("key_0"), "col2" to listOf("sure that does not exist")))
        }
        Assertions.assertEquals(E.service.model.lookup.badLookupForColumn, exception.code)
    }

    @Test
    fun `Lookup must respect limit`() {

        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_LOOKUP_ID))).thenReturn(dataSourceLookup)
        whenever(dataSourceService.getDataSourceById(eq(DATA_SOURCE_TEST_ID))).thenReturn(dataSourceTest)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_LOOKUP_ID))).thenReturn(dataProviderLookup)
        whenever(dataProviderService.getDataProviderById(eq(DATA_PROVIDER_TEST_ID))).thenReturn(dataProviderTest)

        // The lookup data
        whenever(sqlDataReader.getData(any(), isNull(), isNull(), isNull(), isNull())).thenReturn(
            listOf(
                mapOf("key" to "key_0", "value1" to "value1_0", "value2" to "value2_0"),
                mapOf("key" to "key_1", "value1" to "value1_1", "value2" to "value2_1")))

        // The service
        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        val exception = Assertions.assertThrows(BadParameterException::class.java) {
            dataAccessService.insertData(dataSourceTest, mapOf("col1" to listOf("key_0", "key_1"), "col2" to listOf("key_1")))
        }
        Assertions.assertEquals(E.service.model.lookup.tooManyLookupsForColumn, exception.code)
    }
}