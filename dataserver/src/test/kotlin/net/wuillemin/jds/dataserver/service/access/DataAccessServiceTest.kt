package net.wuillemin.jds.dataserver.service.access

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.query.ColumnName
import net.wuillemin.jds.dataserver.entity.query.Equal
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.service.model.DataProviderService
import net.wuillemin.jds.dataserver.service.model.DataSourceService
import net.wuillemin.jds.dataserver.service.model.SchemaService
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLConnectionCache
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLDataWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class DataAccessServiceTest {

    private var schemaService = Mockito.mock(SchemaService::class.java)
    private var dataSourceService = Mockito.mock(DataSourceService::class.java)
    private var dataProviderService = Mockito.mock(DataProviderService::class.java)
    private var sqlDataReader = Mockito.mock(SQLDataReader::class.java)
    private var sqlDataWriter = Mockito.mock(SQLDataWriter::class.java)
    private var sqlConnectionCache = Mockito.mock(SQLConnectionCache::class.java)

    private val SCHEMA_ID = 200L
    private val DATA_PROVIDER_SQL_ID = 300L
    private val DATA_SOURCE_ID = 400L
    
    
    @BeforeEach
    fun beforeEach() {
        dataProviderService = Mockito.mock(DataProviderService::class.java)
        sqlDataReader = Mockito.mock(SQLDataReader::class.java)
        sqlDataWriter = Mockito.mock(SQLDataWriter::class.java)
    }

    private val dataProviderSQL = DataProviderSQL(
        id = DATA_PROVIDER_SQL_ID,
        schemaId = SCHEMA_ID,
        name = "The SQL data provider",
        columns = emptyList(),
        editable = false,
        query = "select * from table")

    private val dataSourceSQL = DataSource(
        id = DATA_SOURCE_ID,
        name = "The SQL data provider",
        dataProviderId = DATA_PROVIDER_SQL_ID,
        userAllowedToReadIds = emptySet(),
        userAllowedToWriteIds = emptySet(),
        userAllowedToDeleteIds = emptySet())

    @Test
    fun `GetData for DataProviderSQL calls SQLDataReader service`() {

        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        whenever(dataProviderService.getDataProviderById(DATA_PROVIDER_SQL_ID)).thenReturn(dataProviderSQL)
        whenever(sqlDataReader.getData(eq(dataProviderSQL), isNull(), isNull(), isNull(), isNull())).thenReturn(listOf(emptyMap()))

        val result = dataAccessService.getData(dataSourceSQL, null, null)

        Assertions.assertTrue(result.size == 1)
    }

    @Test
    fun `InsertData for DataProviderSQL calls SQLDataWriter service`() {

        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        whenever(dataProviderService.getDataProviderById(DATA_PROVIDER_SQL_ID)).thenReturn(dataProviderSQL)
        whenever(sqlDataWriter.insertData(eq(dataProviderSQL), eq(emptyMap()))).thenReturn(1)

        val result = dataAccessService.insertData(dataSourceSQL, emptyMap())

        Assertions.assertEquals(1, result)
    }

    @Test
    fun `MassInsertData for DataProviderSQL calls SQLDataWriter service`() {

        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        whenever(dataProviderService.getDataProviderById(DATA_PROVIDER_SQL_ID)).thenReturn(dataProviderSQL)
        whenever(sqlDataWriter.massInsertData(eq(dataProviderSQL), eq(emptyList()))).thenReturn(25)

        val result = dataAccessService.massInsertData(dataSourceSQL, emptyList())

        Assertions.assertEquals(25, result)
    }

    @Test
    fun `UpdateData for DataProviderSQL calls SQLDataWriter service`() {

        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        val predicate: Predicate = Equal(
            ColumnName("columnName"),
            ColumnName("columnName"))

        whenever(dataProviderService.getDataProviderById(DATA_PROVIDER_SQL_ID)).thenReturn(dataProviderSQL)
        whenever(sqlDataWriter.updateData(eq(dataProviderSQL), eq(predicate), eq(emptyMap()), isNull())).thenReturn(15)

        val result = dataAccessService.updateData(dataSourceSQL, predicate, emptyMap())

        Assertions.assertEquals(15, result)
    }

    @Test
    fun `DeleteData for DataProviderSQL calls SQLDataWriter service`() {

        val dataAccessService = DataAccessService(dataSourceService, dataProviderService, schemaService, sqlDataReader, sqlDataWriter, sqlConnectionCache)

        val predicate: Predicate = Equal(
            ColumnName("columnName"),
            ColumnName("columnName"))

        whenever(dataProviderService.getDataProviderById(DATA_PROVIDER_SQL_ID)).thenReturn(dataProviderSQL)
        whenever(sqlDataWriter.deleteData(eq(dataProviderSQL), eq(predicate))).thenReturn(5)

        val result = dataAccessService.deleteData(dataSourceSQL, predicate)

        Assertions.assertEquals(5, result)
    }
}